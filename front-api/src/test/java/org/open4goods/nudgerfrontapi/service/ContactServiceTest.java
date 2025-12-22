package org.open4goods.nudgerfrontapi.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.ContactProperties;
import org.open4goods.nudgerfrontapi.dto.contact.ContactRequestDto;
import org.open4goods.nudgerfrontapi.config.properties.MailTemplateProperties;
import org.open4goods.nudgerfrontapi.config.properties.MailTemplateProperties.MailTemplate;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.captcha.service.HcaptchaService;

/**
 * Unit tests for {@link ContactService}.
 */
class ContactServiceTest {

    private ContactMailService contactMailService;
    private ContactProperties contactProperties;
    private MailTemplateProperties mailTemplateProperties;
    private HcaptchaService hcaptchaService;
    private ContactService contactService;

    @BeforeEach
    void setUp() {
        contactMailService = mock(ContactMailService.class);
        contactProperties = new ContactProperties();
        contactProperties.setEmail("contact@nudger.fr");
        mailTemplateProperties = new MailTemplateProperties();
        hcaptchaService = mock(HcaptchaService.class);
        contactService = new ContactService(contactMailService, contactProperties, mailTemplateProperties, hcaptchaService);
    }

    @Test
    void submitShouldVerifyCaptchaAndSendEmail() throws Exception {
        ContactRequestDto request = new ContactRequestDto(
                "Jean",
                "jean@example.com",
                "Bonjour",
                "token",
                null,
                null,
                null,
                null,
                null);

        contactService.submit(request, "127.0.0.1", DomainLanguage.fr);

        verify(hcaptchaService).verifyRecaptcha("127.0.0.1", "token");
        verify(contactMailService).send("contact@nudger.fr", "Bonjour", "nudger.fr > Message de Jean",
                "jean@example.com");
    }

    @Test
    void submitShouldUseTemplateWhenAvailable() throws Exception {
        MailTemplate template = new MailTemplate();
        template.setId("agent-question");
        template.setTo("agents@nudger.fr");
        template.setSubject(java.util.Map.of("fr", "Sujet {{name}}", "en", "Subject {{name}}"));
        template.setBody(java.util.Map.of("fr", "Corps {{message}}", "en", "Body {{message}}"));
        mailTemplateProperties.setTemplates(java.util.List.of(template));

        ContactRequestDto request = new ContactRequestDto(
                "Marie",
                "marie@example.com",
                "Bonjour",
                "token",
                "agent-question",
                null,
                "/prompt",
                "AgentPromptInput",
                "Espace Agents"
        );

        contactService.submit(request, "10.0.0.1", DomainLanguage.fr);

        verify(hcaptchaService).verifyRecaptcha("10.0.0.1", "token");
        verify(contactMailService).send("agents@nudger.fr", "Corps Bonjour", "Sujet Marie", "marie@example.com");
    }

    @Test
    void submitShouldPropagateExceptions() throws Exception {
        ContactRequestDto request = new ContactRequestDto(
                "Jane",
                "jane@example.com",
                "Salut",
                "token",
                null,
                null,
                null,
                null,
                null);
        doThrow(new Exception("smtp error")).when(contactMailService)
                .send("contact@nudger.fr", "Salut", "nudger.fr > Message de Jane", "jane@example.com");

        assertThrows(Exception.class, () -> contactService.submit(request, "192.168.0.1", DomainLanguage.fr));
        verify(hcaptchaService).verifyRecaptcha("192.168.0.1", "token");
    }
}
