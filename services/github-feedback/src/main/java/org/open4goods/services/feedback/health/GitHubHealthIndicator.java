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
            java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    // Lighter check: just get metadata
                    repository.getUpdatedAt();
                } catch (Exception e) {
                     throw new RuntimeException(e);
                }
            });
            
            future.get(3, java.util.concurrent.TimeUnit.SECONDS);
            return Health.up().build();
        } catch (java.util.concurrent.TimeoutException e) {
            return Health.down(e).withDetail("feedback.github", "timeout after 3s").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("feedback.github", "unreachable").build();
        }
    }
}
