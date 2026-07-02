package org.open4goods.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    @Test
    void healthIsUpWithZeroCounters() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Health health = handler.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("total_500_errors", 0);
        assertThat(health.getDetails()).containsEntry("total_400_errors", 0);
    }

    @Test
    void serverErrorIsCountedWithoutMarkingHealthDown() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleException(new IllegalStateException("boom"), request("/broken"));
        Health health = handler.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("total_500_errors", 1);
        assertThat(health.getDetails()).containsEntry("total_400_errors", 0);
        assertThat(detailMap(health, "server_errors_by_endpoint")).containsEntry("/broken", 1);
    }

    @Test
    void noResourceFoundIsCountedAsClientError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        NoResourceFoundException exception = new NoResourceFoundException(
                HttpMethod.GET,
                "/wiki-files/missing-image.webp",
                "No static resource /wiki-files/missing-image.webp.");

        var response = handler.handleException(exception, request("/wiki-files/missing-image.webp"));
        Health health = handler.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("total_500_errors", 0);
        assertThat(health.getDetails()).containsEntry("total_400_errors", 1);
        assertThat(detailMap(health, "client_errors_by_endpoint")).containsEntry("/wiki-files/missing-image.webp", 1);
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> detailMap(Health health, String key) {
        return (Map<String, Integer>) health.getDetails().get(key);
    }
}
