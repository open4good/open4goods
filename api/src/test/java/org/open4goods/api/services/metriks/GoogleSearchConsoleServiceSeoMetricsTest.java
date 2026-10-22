package org.open4goods.api.services.metriks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Tests {@link GoogleSearchConsoleService#fetchSeoMetrics()} with a mocked {@link HttpClient}. */
class GoogleSearchConsoleServiceSeoMetricsTest {

    private static final String TOKEN = "ya29.secret-token";
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    private HttpClient clientAnswering(Function<JsonNode, HttpResponse<String>> answer) throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(inv -> {
            HttpRequest request = inv.getArgument(0);
            return answer.apply(mapper.readTree(bodyOf(request)));
        });
        return client;
    }

    private static String bodyOf(HttpRequest request) {
        var subscriber = new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            final StringBuilder sb = new StringBuilder();
            public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer b) { sb.append(java.nio.charset.StandardCharsets.UTF_8.decode(b)); }
            public void onError(Throwable t) { }
            public void onComplete() { }
        };
        request.bodyPublisher().orElseThrow().subscribe(subscriber);
        return subscriber.sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private GoogleSearchConsoleService service(HttpClient client) {
        return new GoogleSearchConsoleService("sc-domain:nudger.fr", 28, 3, client, () -> TOKEN);
    }

    private static boolean isCurrent(JsonNode body, LocalDate currentStart) {
        return body.path("startDate").asString().equals(currentStart.toString());
    }

    @Test
    void windowsAreContiguous28DaysEndingLagDaysAgo() throws Exception {
        HttpClient client = clientAnswering(b -> response(200, "{}"));

        var metrics = service(client).fetchSeoMetrics();

        LocalDate expectedEnd = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        assertThat(metrics.current().end()).isEqualTo(expectedEnd);
        assertThat(metrics.current().start()).isEqualTo(expectedEnd.minusDays(27));
        assertThat(metrics.previous().end()).isEqualTo(metrics.current().start().minusDays(1));
        assertThat(metrics.previous().start()).isEqualTo(metrics.previous().end().minusDays(27));
    }

    @Test
    void totalsAndPreviousTotalsAreRead_andQueriesUseFinalDataWithoutDimensionForTotals() throws Exception {
        List<JsonNode> bodies = new ArrayList<>();
        LocalDate currentStart = LocalDate.now(ZoneOffset.UTC).minusDays(3).minusDays(27);
        HttpClient client = clientAnswering(b -> {
            bodies.add(b);
            if (b.has("dimensions")) {
                return response(200, "{\"rows\":[]}");
            }
            return response(200, isCurrent(b, currentStart)
                    ? "{\"rows\":[{\"clicks\":120,\"impressions\":3000,\"ctr\":0.04,\"position\":12.345}]}"
                    : "{\"rows\":[{\"clicks\":100,\"impressions\":2500,\"ctr\":0.04,\"position\":14.0}]}");
        });

        var metrics = service(client).fetchSeoMetrics();

        assertThat(metrics.currentTotals().value().clicks()).isEqualTo(120);
        assertThat(metrics.currentTotals().value().impressions()).isEqualTo(3000);
        assertThat(metrics.currentTotals().value().position()).isEqualTo(12.345);
        assertThat(metrics.previousTotals().value().clicks()).isEqualTo(100);
        assertThat(bodies).allSatisfy(b -> assertThat(b.path("dataState").asString()).isEqualTo("final"));
        assertThat(bodies.stream().filter(b -> !b.has("dimensions"))).hasSize(2);
    }

    @Test
    void topsAreSortedByClicksCappedAt20_andPreviousIsNullWhenKeyMissing() throws Exception {
        LocalDate currentStart = LocalDate.now(ZoneOffset.UTC).minusDays(3).minusDays(27);
        StringBuilder current = new StringBuilder("{\"rows\":[");
        for (int i = 0; i < 25; i++) {
            if (i > 0) {
                current.append(',');
            }
            current.append("{\"keys\":[\"q").append(i).append("\"],\"clicks\":").append(i)
                    .append(",\"impressions\":10,\"ctr\":0.1,\"position\":2.0}");
        }
        current.append("]}");
        String previous = "{\"rows\":[{\"keys\":[\"q24\"],\"clicks\":7,\"impressions\":50,\"ctr\":0.1,\"position\":5.5}]}";
        List<Integer> limits = new ArrayList<>();
        HttpClient client = clientAnswering(b -> {
            if (!b.has("dimensions")) {
                return response(200, "{}");
            }
            limits.add(b.path("rowLimit").asInt());
            return response(200, isCurrent(b, currentStart) ? current.toString() : previous);
        });

        var top = service(client).fetchSeoMetrics().topQueries().value();

        assertThat(top.rows()).hasSize(20);
        assertThat(top.rows().get(0).key()).isEqualTo("q24");
        assertThat(top.rows().get(0).clicks()).isEqualTo(24);
        assertThat(top.rows()).isSortedAccordingTo((a, b) -> Long.compare(b.clicks(), a.clicks()));
        assertThat(top.previousByKey()).containsOnlyKeys("q24");
        assertThat(top.previousByKey().get("q24").clicks()).isEqualTo(7);
        assertThat(top.previousByKey().get("q23")).isNull();
        assertThat(limits).contains(20, 1000);
    }

    @Test
    void tokenIsLoadedOncePerCall() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        HttpClient client = clientAnswering(b -> response(200, "{}"));
        var service = new GoogleSearchConsoleService("sc-domain:nudger.fr", 28, 3, client, () -> {
            loads.incrementAndGet();
            return TOKEN;
        });

        service.fetchSeoMetrics();

        assertThat(loads).hasValue(1);
        verify(client, times(6)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void http401MarksEveryGroupAsFailed_withoutLeakingToken() throws Exception {
        HttpClient client = clientAnswering(b -> response(401, "invalid credentials " + TOKEN));

        var metrics = service(client).fetchSeoMetrics();

        assertThat(metrics.currentTotals().isFailure()).isTrue();
        assertThat(metrics.previousTotals().isFailure()).isTrue();
        assertThat(metrics.topQueries().isFailure()).isTrue();
        assertThat(metrics.topPages().isFailure()).isTrue();
        assertThat(metrics.currentTotals().error()).contains("HTTP 401").doesNotContain(TOKEN);
    }

    @Test
    void missingCredentialFailsEveryGroupWithoutAnyHttpCall() throws Exception {
        HttpClient client = mock(HttpClient.class);
        var service = new GoogleSearchConsoleService("sc-domain:nudger.fr", 28, 3, client, () -> {
            throw new java.io.IOException("Missing Google credentials");
        });

        var metrics = service.fetchSeoMetrics();

        assertThat(metrics.currentTotals().error()).contains("Missing Google credentials");
        assertThat(metrics.topPages().isFailure()).isTrue();
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(0)).send(captor.capture(), any(HttpResponse.BodyHandler.class));
    }
}
