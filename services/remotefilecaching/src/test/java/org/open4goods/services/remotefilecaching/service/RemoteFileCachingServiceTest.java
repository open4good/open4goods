package org.open4goods.services.remotefilecaching.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests diagnostic URL handling for remote resource caching.
 */
class RemoteFileCachingServiceTest {

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
}
