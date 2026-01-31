package org.open4goods.nudgerfrontapi.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.indexation.GoogleIndexationMetricsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.GoogleIndexationQueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link GoogleIndexationController}.
 */
class GoogleIndexationControllerTest {

    @Test
    void metricsReturnsSnapshot() {
        GoogleIndexationQueueService queueService = mock(GoogleIndexationQueueService.class);
        GoogleIndexationMetricsDto metrics = new GoogleIndexationMetricsDto(
                true,
                3,
                2,
                1,
                Instant.now(),
                Instant.now(),
                "error");
        when(queueService.metricsSnapshot(DomainLanguage.fr)).thenReturn(metrics);

        GoogleIndexationController controller = new GoogleIndexationController(queueService);

        ResponseEntity<GoogleIndexationMetricsDto> response = controller.metrics(DomainLanguage.fr);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(metrics);
    }
}
