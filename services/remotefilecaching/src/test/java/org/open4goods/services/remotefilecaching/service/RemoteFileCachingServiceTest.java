package org.open4goods.services.remotefilecaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests diagnostic URL handling for remote resource caching.
 */
class RemoteFileCachingServiceTest {

    @TempDir
    Path tempDir;

    /**
     * Ensures query values, which can contain credentials, never reach logs.
     */
    @Test
    void redactsQueryValuesFromDiagnosticUrls() {
        assertThat(RemoteFileCachingService.loggableUrl(
                "https://example.test/resource.json?accessToken=secret&signature=also-secret"))
                .isEqualTo("https://example.test/resource.json?<redacted>");
    }

    /**
     * Retains path-only URLs to preserve useful diagnostics.
     */
    @Test
    void retainsQueryFreeDiagnosticUrls() {
        assertThat(RemoteFileCachingService.loggableUrl("https://example.test/resource.json"))
                .isEqualTo("https://example.test/resource.json");
    }

    /**
     * Ensures a URL carrying a secret as a path segment is replaced by its log label, including when the download
     * fails.
     */
    @Test
    void logsTheLabelInsteadOfAUrlCarryingASecretInItsPath() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RemoteFileCachingService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            RemoteFileCachingService service = new RemoteFileCachingService(tempDir.toString());

            // Port 1 is never listening: the download fails and the failure is logged.
            service.getResource("http://127.0.0.1:1/apiv3/path-secret-value/resource?page=1", 1,
                    "http://127.0.0.1:1/apiv3/{apikey}/resource");
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
                .doesNotContain("path-secret-value"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("{apikey}"));
    }
}
