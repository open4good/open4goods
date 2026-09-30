package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.mockito.Mockito;

/**
 * Verifies that Icecat download failures do not preserve sensitive request metadata.
 */
class IcecatFileDownloadServiceTest {

    @TempDir
    Path cacheDirectory;

    @Test
    void downloadFailureDoesNotRetainTheSignedUrlInTheExceptionChain() {
        RemoteFileCachingService remoteCache = Mockito.mock(RemoteFileCachingService.class);
        doThrow(new IllegalStateException("upstream?signature=synthetic-value"))
                .when(remoteCache).downloadTo(any(), any(), any(), any());
        IcecatFileDownloadService service = new IcecatFileDownloadService(remoteCache, cacheDirectory.toString(),
                new IcecatConfiguration());

        assertThatThrownBy(() -> service.getOrDownload("https://example.test/catalogue?signature=synthetic-value"))
                .isInstanceOf(TechnicalException.class)
                .hasMessage("Error retrieving Icecat cache resource")
                .hasNoCause();
    }
}
