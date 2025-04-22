// src/main/java/org/open4goods/services/feedback/health/GitHubHealthIndicator.java
package org.open4goods.services.feedback.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import org.kohsuke.github.GHRepository;
import org.open4goods.services.feedback.config.FeedbackConfiguration;

/**
 * Verifies that GitHub-based feedback is properly configured and reachable.
 */
@Component("feedbackHealthIndicator")
public class GitHubHealthIndicator implements HealthIndicator {

    private final FeedbackConfiguration config;
    private final GHRepository repository;

    public GitHubHealthIndicator(FeedbackConfiguration config,
                                 GHRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    @Override
    public Health health() {
        var githubCfg = config.getGithub();
        if (githubCfg.getAccessToken() == null
                || githubCfg.getOrganization().isBlank()
                || githubCfg.getRepo().isBlank()) {
            return Health.down()
                    .withDetail("feedback.github", "missing required configuration")
                    .build();
        }
        try {
            // simple ping to validate credentials
            repository.getDirectoryContent(".");
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).withDetail("feedback.github", "unreachable").build();
        }
    }
}
