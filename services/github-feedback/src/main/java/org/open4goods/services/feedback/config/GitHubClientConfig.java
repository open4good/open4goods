// src/main/java/org/open4goods/services/feedback/config/GitHubClientConfig.java
package org.open4goods.services.feedback.config;

import java.io.IOException;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a preconfigured GitHub repository client for issue/vote services.
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "feedback.github", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GitHubClientConfig {

    private final FeedbackConfiguration feedbackConfig;

    public GitHubClientConfig(FeedbackConfiguration feedbackConfig) {
        this.feedbackConfig = feedbackConfig;
    }

    @Bean
    public GHRepository ghRepository() throws IOException {
        var githubCfg = feedbackConfig.getGithub();
        return GitHubBuilder.fromEnvironment()
                .withOAuthToken(githubCfg.getAccessToken())
                .build()
                .getRepository(githubCfg.getOrganization() + "/" + githubCfg.getRepo());
    }
}
