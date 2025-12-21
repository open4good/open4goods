package org.open4goods.nudgerfrontapi.config.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties describing the agents exposed by the frontend.
 */
@ConfigurationProperties(prefix = "front.agent")
public class AgentProperties {

    /**
     * Ordered list of configured agents available for frontend interactions.
     */
    private List<AgentConfig> agents = new ArrayList<>();

    /**
     * Returns the configured agents.
     *
     * @return configured agents list
     */
    public List<AgentConfig> getAgents() {
        return agents;
    }

    /**
     * Sets the configured agents.
     *
     * @param agents agents definition list
     */
    public void setAgents(List<AgentConfig> agents) {
        this.agents = agents;
    }

    /**
     * Description of a single agent definition.
     */
    public static class AgentConfig {

        /**
         * Technical identifier for the agent.
         */
        private String id;

        /**
         * Icon reference used by the frontend (e.g. mdi identifier or asset path).
         */
        private String icon;

        /**
         * Prompt template forwarded to the agent runtime.
         */
        private String promptTemplate;

        /**
         * Tags helping to classify or filter the agent in the UI.
         */
        private List<String> tags = new ArrayList<>();

        /**
         * Application roles allowed to access the agent.
         */
        private List<String> allowedRoles = new ArrayList<>();

        /**
         * Optional mail template identifier associated with the agent.
         */
        private String mailTemplate;

        /**
         * Localised labels and descriptions keyed by ISO language code.
         */
        private Map<String, AgentLocalization> i18n = new HashMap<>();

        /**
         * Returns the agent identifier.
         *
         * @return agent identifier
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the agent identifier.
         *
         * @param id agent identifier
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Returns the frontend icon reference.
         *
         * @return icon reference
         */
        public String getIcon() {
            return icon;
        }

        /**
         * Sets the frontend icon reference.
         *
         * @param icon icon reference
         */
        public void setIcon(String icon) {
            this.icon = icon;
        }

        /**
         * Returns the prompt template content.
         *
         * @return prompt template
         */
        public String getPromptTemplate() {
            return promptTemplate;
        }

        /**
         * Sets the prompt template content.
         *
         * @param promptTemplate prompt template
         */
        public void setPromptTemplate(String promptTemplate) {
            this.promptTemplate = promptTemplate;
        }

        /**
         * Returns the configured tags.
         *
         * @return agent tags
         */
        public List<String> getTags() {
            return tags;
        }

        /**
         * Sets the tags associated with the agent.
         *
         * @param tags agent tags
         */
        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        /**
         * Returns the allowed roles.
         *
         * @return allowed roles
         */
        public List<String> getAllowedRoles() {
            return allowedRoles;
        }

        /**
         * Sets the roles allowed to access the agent.
         *
         * @param allowedRoles allowed roles
         */
        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }

        /**
         * Returns the mail template identifier.
         *
         * @return mail template identifier
         */
        public String getMailTemplate() {
            return mailTemplate;
        }

        /**
         * Sets the mail template identifier.
         *
         * @param mailTemplate mail template identifier
         */
        public void setMailTemplate(String mailTemplate) {
            this.mailTemplate = mailTemplate;
        }

        /**
         * Returns the localised labels and descriptions.
         *
         * @return localisation map
         */
        public Map<String, AgentLocalization> getI18n() {
            return i18n;
        }

        /**
         * Sets the localised labels and descriptions.
         *
         * @param i18n localisation map
         */
        public void setI18n(Map<String, AgentLocalization> i18n) {
            this.i18n = i18n;
        }
    }

    /**
     * Localised title and description for an agent.
     */
    public static class AgentLocalization {

        /**
         * Localised agent title displayed in the UI.
         */
        private String title;

        /**
         * Localised description helping users understand the agent.
         */
        private String description;

        /**
         * Returns the localised title.
         *
         * @return localised title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the localised title.
         *
         * @param title localised title
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Returns the localised description.
         *
         * @return localised description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the localised description.
         *
         * @param description localised description
         */
        public void setDescription(String description) {
            this.description = description;
        }
    }
}
