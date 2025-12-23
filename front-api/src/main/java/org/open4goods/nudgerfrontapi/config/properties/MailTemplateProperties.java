package org.open4goods.nudgerfrontapi.config.properties;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Configuration holder for reusable mail templates used by the contact flow.
 * <p>
 * Templates can be shared across the application (agents, contact forms, etc.)
 * and are resolved at runtime based on the domain language. Values are
 * provided through the {@code front.mail-templates} configuration namespace.
 * </p>
 */
@Configuration
@ConfigurationProperties(prefix = "front.mail-templates")
public class MailTemplateProperties {

    @Schema(description = "Collection of mail templates available to the contact service.")
    private List<MailTemplate> templates;

    public List<MailTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<MailTemplate> templates) {
        this.templates = templates;
    }

    public static class MailTemplate {
        @Schema(description = "Unique identifier used to resolve the template.", example = "agent-feedback")
        private String id;

        @Schema(description = "Recipient email override for this template.", example = "team@nudger.fr")
        private String to;

        @Schema(description = "Localized subject lines keyed by language tag.")
        private Map<String, String> subject;

        @Schema(description = "Localized message body keyed by language tag.")
        private Map<String, String> body;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public Map<String, String> getSubject() {
            return subject;
        }

        public void setSubject(Map<String, String> subject) {
            this.subject = subject;
        }

        public Map<String, String> getBody() {
            return body;
        }

        public void setBody(Map<String, String> body) {
            this.body = body;
        }
    }
}
