package org.open4goods.api.services.metriks.affiliation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.services.contribution.model.AffiliationClickStats;
import org.open4goods.services.contribution.service.AffiliationClickStatsService;

/**
 * Test helpers of the Metriks affiliation clients: fake HTTP exchanges and recorded, anonymised responses
 * ({@code src/test/resources/metriks/affiliation}).
 */
final class AffiliationTestSupport {

    /** "Now" of the tests: 2026-10-22 10:00 in Europe/Paris, so 28d = [2026-09-24, 2026-10-21], 7d = [2026-10-15, 2026-10-21]. */
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T08:00:00Z"), AffiliationMetriksService.ZONE);

    private AffiliationTestSupport() {
    }

    /**
     * A click reader answering {@code stats} (or throwing {@code failure}) without Elasticsearch, recording the
     * requested bounds.
     */
    static final class FakeClickStats extends AffiliationClickStatsService {
        final List<long[]> calls = new ArrayList<>();
        private final AffiliationClickStats stats;
        private final RuntimeException failure;

        FakeClickStats(AffiliationClickStats stats, RuntimeException failure) {
            super(null);
            this.stats = stats;
            this.failure = failure;
        }

        @Override
        public AffiliationClickStats read(long longStart, long shortStart, long end, int topMerchants) {
            calls.add(new long[] {longStart, shortStart, end, topMerchants});
            if (failure != null) {
                throw failure;
            }
            return stats;
        }
    }

    /**
     * @return a click reader answering no click at all
     */
    static FakeClickStats noClicks() {
        Map<String, Long> zero = Map.of("awin", 0L, "effiliation", 0L, "other", 0L);
        return new FakeClickStats(new AffiliationClickStats(zero, zero, 0, 0, List.of()), null);
    }

    /**
     * Answers a request with a response, or by throwing an {@link IOException}.
     */
    @FunctionalInterface
    interface Responder {
        HttpResponse<String> respond(HttpRequest request) throws IOException;
    }

    /**
     * A fake HTTP client recording every request it receives.
     */
    static final class FakeHttp {
        final List<HttpRequest> requests = new ArrayList<>();
        final HttpClient client;

        @SuppressWarnings("unchecked")
        FakeHttp(Responder responder) {
            client = mock(HttpClient.class);
            try {
                doAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    requests.add(request);
                    return responder.respond(request);
                }).when(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    static String fixture(String name) {
        try (InputStream in = AffiliationTestSupport.class.getResourceAsStream("/metriks/affiliation/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return the body of a request (form or JSON), read from its publisher
     */
    static String body(HttpRequest request) {
        return request.bodyPublisher().map(publisher -> {
            StringBuilder body = new StringBuilder();
            publisher.subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
                @Override
                public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(java.nio.ByteBuffer item) {
                    body.append(StandardCharsets.UTF_8.decode(item));
                }

                @Override
                public void onError(Throwable throwable) {
                    throw new IllegalStateException(throwable);
                }

                @Override
                public void onComplete() {
                    // nothing to do
                }
            });
            return body.toString();
        }).orElse("");
    }
}
