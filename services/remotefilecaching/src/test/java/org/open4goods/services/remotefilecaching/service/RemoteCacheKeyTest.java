package org.open4goods.services.remotefilecaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;

import org.open4goods.model.exceptions.TechnicalException;
import org.junit.jupiter.api.Test;

/**
 * Verifies that remote cache names remain deterministic without retaining URL text.
 */
class RemoteCacheKeyTest {

    @Test
    void derivesOpaqueDeterministicKeyFromCompleteUrl() {
        String first = RemoteCacheKey.fromUrl("https://example.test/catalogue?accessToken=first-secret");
        String same = RemoteCacheKey.fromUrl("https://example.test/catalogue?accessToken=first-secret");
        String different = RemoteCacheKey.fromUrl("https://example.test/catalogue?accessToken=second-secret");

        assertThat(first).isEqualTo(same).matches("[0-9a-f]{64}");
        assertThat(first).isNotEqualTo(different);
        assertThat(first).doesNotContain("example", "accessToken", "first-secret");
    }

    @Test
    void temporaryKeyOnlyAddsFixedPrefixToOpaqueKey() {
        String url = "https://example.test/catalogue?signature=signed-value";

        assertThat(RemoteCacheKey.temporaryFromUrl(url))
                .isEqualTo("tmp-" + RemoteCacheKey.fromUrl(url))
                .doesNotContain("signature", "signed-value");
    }

    @Test
    void downloadFailureDoesNotRetainTheRequestUrlAsAnExceptionCause() throws Exception {
        RemoteFileCachingService service = new RemoteFileCachingService(Files.createTempDirectory("remote-cache").toString());
        String url = "not-a-url?credential=synthetic-value";

        assertThatThrownBy(() -> service.download(url, Files.createTempFile("cache-entry", ".tmp").toFile()))
                .isInstanceOf(TechnicalException.class)
                .hasMessage("Cannot download resource")
                .hasNoCause();
    }
}
