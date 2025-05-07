// src/main/java/org/open4goods/services/feedback/config/FeedbackConfiguration.java
package org.open4goods.services.feedback.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralized configuration for both feedback (GitHub issues) and voting.
 * <p>
 * Binds to properties under the prefix "feedback" in application.yml
 * (e.g. feedback.github.access-token, feedback.voting.max-votes-per-ip-per-day).
 */
@Component
@ConfigurationProperties(prefix = "feedback")
public class FeedbackConfiguration {

    private final Github github = new Github();
    private final Voting voting = new Voting();

    /** GitHub-related settings (token, org, repo). */
    public Github getGithub() {
        return github;
    }

    /** Voting-related settings (per‑IP quotas, labels). */
    public Voting getVoting() {
        return voting;
    }

    public static class Github {
        @NotBlank private String accessToken;
        @NotBlank private String organization;
        @NotBlank private String repo;
        @NotBlank private String user;

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }

        public String getRepo() { return repo; }
        public void setRepo(String repo) { this.repo = repo; }

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
    }

    public static class Voting {
        /** Max votes each IP may cast per day. */
        @Min(1)
        private int maxVotesPerIpPerDay = 5;

        /** Label that issues must have to be votable. */
        @NotBlank
        private String requiredLabel = "votable";

        /**
         * If true, newly created issues will automatically receive the required votable label.
         */
        private boolean defaultVotable = true;

        public int getMaxVotesPerIpPerDay() {
            return maxVotesPerIpPerDay;
        }
        public void setMaxVotesPerIpPerDay(int maxVotesPerIpPerDay) {
            this.maxVotesPerIpPerDay = maxVotesPerIpPerDay;
        }

        public String getRequiredLabel() {
            return requiredLabel;
        }
        public void setRequiredLabel(String requiredLabel) {
            this.requiredLabel = requiredLabel;
        }

        public boolean isDefaultVotable() {
            return defaultVotable;
        }
        public void setDefaultVotable(boolean defaultVotable) {
            this.defaultVotable = defaultVotable;
        }
    }
}
