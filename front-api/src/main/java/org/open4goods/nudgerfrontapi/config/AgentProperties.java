package org.open4goods.nudgerfrontapi.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "front.agent")
public class AgentProperties {

    private List<AgentConfig> agents;

    public List<AgentConfig> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentConfig> agents) {
        this.agents = agents;
    }

    public static class AgentConfig {
        private String id;
        private Map<String, String> name;
        private Map<String, String> description;
        private String icon;
        private String promptTemplate;
        private List<String> tags;
        private List<String> allowedRoles;
        private boolean publicPromptHistory;
        private MailTemplateConfig mailTemplate;
        private String previewLabelKey;
        private List<AgentAttribute> attributes;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, String> getName() {
            return name;
        }

        public void setName(Map<String, String> name) {
            this.name = name;
        }

        public Map<String, String> getDescription() {
            return description;
        }

        public void setDescription(Map<String, String> description) {
            this.description = description;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getPromptTemplate() {
            return promptTemplate;
        }

        public void setPromptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public List<String> getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        public boolean isPublicPromptHistory() {
            return publicPromptHistory;
        }

        public void setPublicPromptHistory(boolean publicPromptHistory) {
            this.publicPromptHistory = publicPromptHistory;
        }

        public MailTemplateConfig getMailTemplate() {
            return mailTemplate;
        }

        public void setMailTemplate(MailTemplateConfig mailTemplate) {
            this.mailTemplate = mailTemplate;
        }

        public String getPreviewLabelKey() {
            return previewLabelKey;
        }

        public void setPreviewLabelKey(String previewLabelKey) {
            this.previewLabelKey = previewLabelKey;
        }

        public List<AgentAttribute> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<AgentAttribute> attributes) {
            this.attributes = attributes;
        }
    }

    public static class AgentAttribute {
        private String id;
        private String type; // TEXT, LIST, CHECKBOX, COMBO
        private Map<String, String> label;
        private List<String> options;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, String> getLabel() {
            return label;
        }

        public void setLabel(Map<String, String> label) {
            this.label = label;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }
    }

    public static class MailTemplateConfig {
        private String to;
        private Map<String, String> subject;
        private Map<String, String> body;

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
