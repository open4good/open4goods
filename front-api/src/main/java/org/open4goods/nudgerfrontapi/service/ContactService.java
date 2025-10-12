package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.config.properties.ContactProperties;
import org.open4goods.nudgerfrontapi.dto.contact.ContactRequestDto;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.springframework.stereotype.Service;

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
    private final HcaptchaService hcaptchaService;

    public ContactService(ContactMailService contactMailService, ContactProperties contactProperties,
            HcaptchaService hcaptchaService) {
        this.contactMailService = contactMailService;
        this.contactProperties = contactProperties;
        this.hcaptchaService = hcaptchaService;
    }

    /**
     * Verify captcha and forward the contact message by email.
     *
     * @param request contact request payload
     * @param clientIp IP address reported by the caller
     * @throws Exception when captcha verification or email dispatch fails
     */
    public void submit(ContactRequestDto request, String clientIp) throws Exception {
        hcaptchaService.verifyRecaptcha(clientIp, request.captchaResponse());
        // Prefix the subject to keep a consistent format in the shared inbox.
        contactMailService.send(contactProperties.getEmail(), request.message(), SUBJECT_PREFIX + request.name(),
                request.email());
    }
}
