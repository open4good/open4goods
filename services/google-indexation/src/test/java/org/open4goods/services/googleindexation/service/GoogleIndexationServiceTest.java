package org.open4goods.services.googleindexation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.services.googleindexation.config.GoogleIndexationConfig;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResult;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResultItem;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for {@link GoogleIndexationService}.
 */
class GoogleIndexationServiceTest {

    @Test
    void publishUrlsReturnsEmptyResultWhenDisabled() {
        GoogleIndexationConfig config = new GoogleIndexationConfig();
        config.setEnabled(false);
        GoogleIndexationService service = new GoogleIndexationService(config, new SimpleMeterRegistry(), HttpClient.newHttpClient(),
                () -> GoogleCredentials.create(new AccessToken("token", Date.from(Instant.now()))));

        GoogleIndexationResult result = service.publishUrls(List.of("https://example.org/product/1"));

        assertThat(result.totalCount()).isZero();
        assertThat(result.successCount()).isZero();
    }

    @Test
    void publishUrlMarksSuccessOn2xxResponse() throws Exception {
        GoogleIndexationConfig config = new GoogleIndexationConfig();
        config.setEnabled(true);
        config.setServiceAccountJson("dummy");

        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("ok");
        when(httpClient.send(any(), any())).thenReturn((HttpResponse) response);

        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken("token", Date.from(Instant.now().plusSeconds(3600))));
        GoogleIndexationService service = new GoogleIndexationService(config, new SimpleMeterRegistry(), httpClient, () -> credentials);

        GoogleIndexationResultItem result = service.publishUrl("https://example.org/product/2");

        assertThat(result.success()).isTrue();
        assertThat(service.getLastSuccessAt()).isNotNull();
    }

    @Test
    void publishUrlMarksFailureOnNon2xxResponse() throws Exception {
        GoogleIndexationConfig config = new GoogleIndexationConfig();
        config.setEnabled(true);
        config.setServiceAccountJson("dummy");

        HttpClient httpClient = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("error");
        when(httpClient.send(any(), any())).thenReturn((HttpResponse) response);

        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken("token", Date.from(Instant.now().plusSeconds(3600))));
        GoogleIndexationService service = new GoogleIndexationService(config, new SimpleMeterRegistry(), httpClient, () -> credentials);

        GoogleIndexationResultItem result = service.publishUrl("https://example.org/product/3");

        assertThat(result.success()).isFalse();
        assertThat(service.getLastErrorMessage()).contains("HTTP 500");
    }
}
