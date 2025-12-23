package org.open4goods.nudgerfrontapi.service;

import java.util.Optional;

import org.open4goods.nudgerfrontapi.config.properties.ContactProperties;
import org.open4goods.nudgerfrontapi.config.properties.MailTemplateProperties;
import org.open4goods.nudgerfrontapi.config.properties.MailTemplateProperties.MailTemplate;
import org.open4goods.nudgerfrontapi.dto.contact.ContactRequestDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

/**
 * Service orchestrating contact form submissions: captcha verification and email dispatching.
 * <p>
 * Splitting responsibilities between captcha validation and email rendering allows unit tests to
 * cover error scenarios without hitting external infrastructure.
 * </p>
 */
@Service
public class ContactService {

    private static final String SUBJECT_PREFIX = "nudger.fr > Message de ";

    private final ContactMailService contactMailService;
    private final ContactProperties contactProperties;
    private final MailTemplateProperties mailTemplateProperties;
    private final HcaptchaService hcaptchaService;

    public ContactService(ContactMailService contactMailService, ContactProperties contactProperties,
            MailTemplateProperties mailTemplateProperties, HcaptchaService hcaptchaService) {
        this.contactMailService = contactMailService;
        this.contactProperties = contactProperties;
        this.mailTemplateProperties = mailTemplateProperties;
        this.hcaptchaService = hcaptchaService;
    }

    /**
     * Verify captcha and forward the contact message by email.
     *
     * @param request contact request payload
     * @param clientIp IP address reported by the caller
     * @param domainLanguage locale hint used to select template translations
     * @throws Exception when captcha verification or email dispatch fails
     */
    public void submit(ContactRequestDto request, String clientIp, DomainLanguage domainLanguage) throws Exception {
        hcaptchaService.verifyRecaptcha(clientIp, request.captchaResponse());

        String targetEmail = contactProperties.getEmail();
        String subject = SUBJECT_PREFIX + request.name();
        String body = request.message();

        Optional<MailTemplate> resolvedTemplate = resolveTemplate(request.templateId());

        if (resolvedTemplate.isPresent()) {
            MailTemplate template = resolvedTemplate.get();
            if (StringUtils.hasText(template.getTo())) {
                targetEmail = template.getTo();
            }
            subject = renderTemplateValue(template.getSubject(), request, subject, domainLanguage);
            body = renderTemplateValue(template.getBody(), request, body, domainLanguage);
        } else if (StringUtils.hasText(request.subject())) {
            subject = request.subject();
        }

        contactMailService.send(targetEmail, body, subject, request.email());
    }

    private Optional<MailTemplate> resolveTemplate(String templateId) {
        if (!StringUtils.hasText(templateId) || mailTemplateProperties.getTemplates() == null) {
            return Optional.empty();
        }
        return mailTemplateProperties.getTemplates().stream()
                .filter(template -> templateId.equals(template.getId()))
                .findFirst();
    }

    private String renderTemplateValue(java.util.Map<String, String> localizedValues, ContactRequestDto request,
            String fallback, DomainLanguage domainLanguage) {
        if (localizedValues == null || localizedValues.isEmpty()) {
            return fallback;
        }
        String languageKey = domainLanguage.languageTag();
        String chosen = Optional.ofNullable(localizedValues.get(languageKey))
                .or(() -> Optional.ofNullable(localizedValues.get(languageKey.split("-")[0])))
                .or(() -> Optional.ofNullable(localizedValues.get("en")))
                .orElse(localizedValues.values().iterator().next());

        return chosen
                .replace("{{name}}", request.name())
                .replace("{{email}}", request.email())
                .replace("{{message}}", request.message())
                .replace("{{sourceRoute}}", Optional.ofNullable(request.sourceRoute()).orElse(""))
                .replace("{{sourceComponent}}", Optional.ofNullable(request.sourceComponent()).orElse(""))
                .replace("{{sourcePage}}", Optional.ofNullable(request.sourcePage()).orElse(""))
                .replace("{{subject}}", Optional.ofNullable(request.subject()).orElse(""));
    }
}
