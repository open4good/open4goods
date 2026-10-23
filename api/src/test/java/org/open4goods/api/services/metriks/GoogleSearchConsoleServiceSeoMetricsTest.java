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
        assertThat(limits).containsOnly(25000);
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
        verify(client, times(7)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
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

    private static String row(String key, long clicks) {
        return "{\"keys\":[\"" + key + "\"],\"clicks\":" + clicks
                + ",\"impressions\":100,\"ctr\":0.1,\"position\":3.0}";
    }

    @Test
    void dailySeriesHas56RowsInOrder_andMissingDayIsFilledWithZero() throws Exception {
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(3);
        LocalDate start = end.minusDays(55);
        LocalDate missing = start.plusDays(10);
        List<JsonNode> dailyBodies = new ArrayList<>();
        HttpClient client = clientAnswering(b -> {
            if (!"date".equals(b.path("dimensions").path(0).asString())) {
                return response(200, "{}");
            }
            dailyBodies.add(b);
            StringBuilder sb = new StringBuilder("{\"rows\":[");
            boolean first = true;
            for (int i = 55; i >= 0; i--) { // unordered on purpose
                LocalDate day = start.plusDays(i);
                if (day.equals(missing)) {
                    continue;
                }
                sb.append(first ? "" : ",").append(row(day.toString(), 5));
                first = false;
            }
            return response(200, sb.append("]}").toString());
        });

        var daily = service(client).fetchSeoMetrics().daily().value();

        assertThat(dailyBodies).hasSize(1);
        assertThat(dailyBodies.get(0).path("startDate").asString()).isEqualTo(start.toString());
        assertThat(dailyBodies.get(0).path("endDate").asString()).isEqualTo(end.toString());
        assertThat(daily).hasSize(56);
        assertThat(daily.get(0).date()).isEqualTo(start);
        assertThat(daily.get(55).date()).isEqualTo(end);
        assertThat(daily).extracting(GoogleSearchConsoleService.DailyRow::window)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 56)
                        .mapToObj(i -> i < 28 ? "previous" : "current").toList());
        var filled = daily.get(10);
        assertThat(filled.clicks()).isZero();
        assertThat(filled.impressions()).isZero();
        assertThat(filled.ctr()).isNull();
        assertThat(filled.position()).isNull();
        assertThat(daily.get(11).clicks()).isEqualTo(5);
    }

    @Test
    void losingQueriesAreComputedFilteredSortedAndShareTheTopReads() throws Exception {
        LocalDate currentStart = LocalDate.now(ZoneOffset.UTC).minusDays(3).minusDays(27);
        String current = "{\"rows\":[" + String.join(",",
                row("up", 50), row("tieA", 5), row("tieB", 5), row("same", 10), row("drop", 8)) + "]}";
        String previous = "{\"rows\":[" + String.join(",",
                row("up", 10), row("gone", 30), row("tieA", 15), row("tieB", 20), row("same", 10),
                row("drop", 18), row("zero", 0)) + "]}";
        AtomicInteger dimensionCalls = new AtomicInteger();
        HttpClient client = clientAnswering(b -> {
            if (b.path("dimensions").path(0).asString().equals("query")) {
                dimensionCalls.incrementAndGet();
                return response(200, isCurrent(b, currentStart) ? current : previous);
            }
            return response(200, "{}");
        });

        var top = service(client).fetchSeoMetrics().topQueries().value();

        assertThat(dimensionCalls).hasValue(2);
        // gone: -30 ; tieB: -15 (prev 20) ; tieA: -10 ; drop: -10 (prev 18, ties -> prev desc keeps drop first)
        assertThat(top.losing()).extracting(GoogleSearchConsoleService.LossRow::key)
                .containsExactly("gone", "tieB", "drop", "tieA");
        assertThat(top.losing().get(0).current()).isNull();
        assertThat(top.losing().get(0).clicksDelta()).isEqualTo(-30);
        assertThat(top.truncated()).isFalse();
        assertThat(top.rows().get(0).key()).isEqualTo("up");
    }

    @Test
    void oneFailingGroupDoesNotFailTheOthers() throws Exception {
        HttpClient client = clientAnswering(b -> {
            if ("date".equals(b.path("dimensions").path(0).asString())) {
                return response(500, "boom " + TOKEN);
            }
            return response(200, "{}");
        });

        var metrics = service(client).fetchSeoMetrics();

        assertThat(metrics.daily().isFailure()).isTrue();
        assertThat(metrics.daily().error()).contains("HTTP 500").doesNotContain(TOKEN);
        assertThat(metrics.currentTotals().isFailure()).isFalse();
        assertThat(metrics.topQueries().isFailure()).isFalse();
        assertThat(metrics.topPages().isFailure()).isFalse();
    }

    @Test
    void pagesWithImpressionsCarriesPreviousCount_andIsolatesItsFailure() throws Exception {
        LocalDate currentStart = LocalDate.now(ZoneOffset.UTC).minusDays(3).minusDays(27);
        HttpClient ok = clientAnswering(b -> response(200, isCurrent(b, currentStart)
                ? "{\"rows\":[" + row("a", 1) + "," + row("b", 1) + "]}"
                : "{\"rows\":[" + row("a", 1) + "]}"));
        var result = service(ok).countPagesWithImpressions();
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.previousCount()).isEqualTo(1L);
        assertThat(result.previousEndDate()).isEqualTo(currentStart.minusDays(1));
        assertThat(result.previousStartDate()).isEqualTo(currentStart.minusDays(28));
        assertThat(result.previousError()).isNull();

        HttpClient ko = clientAnswering(b -> isCurrent(b, currentStart)
                ? response(200, "{\"rows\":[" + row("a", 1) + "]}")
                : response(500, "down " + TOKEN));
        var failed = service(ko).countPagesWithImpressions();
        assertThat(failed.count()).isEqualTo(1);
        assertThat(failed.previousCount()).isNull();
        assertThat(failed.previousError()).contains("HTTP 500").doesNotContain(TOKEN);

        HttpClient malformed = clientAnswering(b -> isCurrent(b, currentStart)
                ? response(200, "{\"rows\":[" + row("a", 1) + "]}")
                : response(200, "not json"));
        var unreadable = service(malformed).countPagesWithImpressions();
        assertThat(unreadable.count()).isEqualTo(1);
        assertThat(unreadable.previousCount()).isNull();
        assertThat(unreadable.previousError()).isNotBlank();
    }
}
