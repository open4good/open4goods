package org.open4goods.nudgerfrontapi.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.services.feedback.dto.IssueDto;
import org.open4goods.services.feedback.service.IssueService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalMockConfig {

    @Bean
    @ConditionalOnProperty(prefix = "feedback.github", name = "enabled", havingValue = "false", matchIfMissing = false)
    public IssueService mockIssueService() {
        return new IssueService() {
            private final AtomicInteger counter = new AtomicInteger(1000);

            @Override
            public IssueDto createBug(String title, String description, String urlSource, String author, Set<String> labels) throws IOException {
                return createIssue(title, description, author, labels);
            }

            @Override
            public IssueDto createIdea(String title, String description, String urlSource, String author, Set<String> labels) throws IOException {
                return createIssue(title, description, author, labels);
            }

            @Override
            public List<IssueDto> listBugs() throws IOException {
                return Collections.emptyList();
            }

            @Override
            public List<IssueDto> listIdeas() throws IOException {
                return Collections.emptyList();
            }

            @Override
            public List<IssueDto> listIssues(String... labels) throws IOException {
                return Collections.emptyList();
            }

            @Override
            public IssueDto createIssue(String title, String description, String author, Set<String> labels) throws IOException {
                int id = counter.incrementAndGet();
                return new IssueDto(
                        String.valueOf(id),
                        id,
                        "https://github.com/mock/issue/" + id,
                        "OPEN",
                        title,
                        labels
                );
            }
        };
    }
}
