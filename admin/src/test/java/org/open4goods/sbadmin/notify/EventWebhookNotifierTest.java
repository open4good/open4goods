package org.open4goods.sbadmin.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;

import org.open4goods.sbadmin.config.properties.SbaEventWebhookProperties;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Covers plan criteria 1-6: signature, closed body, disablement, confirmed DOWN -&gt; UP,
 * anti-noise on a DOWN that reverts before confirmation, and bounded retries without surfacing an
 * error to SBA.
 */
class EventWebhookNotifierTest {

    private static final int PORT = 4103;

    private static final String SECRET = "test-secret-do-not-log";

    private static final InstanceId INSTANCE_ID = InstanceId.of("777de8a5e899");

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void signsRequestWithHmacSha256OfTimestampAndRawBody() throws IOException {
        CapturingHandler handler = new CapturingHandler(200);
        server = startServer(handler);
        StubInstanceRepository repository = new StubInstanceRepository(instance(StatusInfo.ofUp()));
        EventWebhookNotifier notifier = notifier(repository, Duration.ofMillis(100));

        InstanceStatusChangedEvent event = new InstanceStatusChangedEvent(INSTANCE_ID, 1L, Instant.now(),
                StatusInfo.ofOutOfService());
        notifier.notify(event).block(Duration.ofSeconds(2));

        assertThat(handler.requestCount.get()).isEqualTo(1);
        String timestamp = handler.lastHeaders.get().getFirst("X-Event-Timestamp");
        String signature = handler.lastHeaders.get().getFirst("X-Event-Signature");
        assertThat(timestamp).isNotBlank();
        assertThat(signature).startsWith("sha256=");

        String expected = "sha256=" + EventWebhookNotifier.hmacSha256Hex(SECRET, timestamp, handler.lastBody.get());
        assertThat(signature).isEqualTo(expected);
    }

    @Test
    void bodyContainsExactlyTheEightDocumentedFields() throws IOException {
        CapturingHandler handler = new CapturingHandler(200);
        server = startServer(handler);
        Instance withNoise = instance(StatusInfo.ofDown(Map.of(
                "exception", "java.lang.RuntimeException: boom",
                "details", Map.of("nested", "value"))));
        StubInstanceRepository repository = new StubInstanceRepository(withNoise);
        EventWebhookNotifier notifier = notifier(repository, Duration.ofMillis(100));

        InstanceStatusChangedEvent event = new InstanceStatusChangedEvent(INSTANCE_ID, 1L, Instant.now(),
                StatusInfo.ofOutOfService(Map.of("exception", "should-not-leak")));
        notifier.notify(event).block(Duration.ofSeconds(2));

        Map<String, Object> body = JSON.readValue(handler.lastBody.get(), Map.class);
        assertThat(body.keySet()).containsExactlyInAnyOrder("source", "environment", "application", "instanceId",
                "from", "to", "at", "eventId");
        assertThat(body.toString()).doesNotContain("should-not-leak");
    }

    @Test
    void disabledWhenUrlOrSecretIsBlank() {
        SbaEventWebhookProperties properties = new SbaEventWebhookProperties();
        properties.setUrl("");
        properties.setSecret("");
        properties.setEnvironment("prod");
        StubInstanceRepository repository = new StubInstanceRepository(instance(StatusInfo.ofUp()));

        EventWebhookNotifier notifier = new EventWebhookNotifier(repository, org.springframework.web.reactive.function.client.WebClient.builder(),
                properties, Duration.ofMillis(100));

        assertThat(notifier.isEnabled()).isFalse();

        InstanceStatusChangedEvent event = new InstanceStatusChangedEvent(INSTANCE_ID, 1L, Instant.now(),
                StatusInfo.ofDown());
        // Disabled notifiers short-circuit before ever consulting the repository (AbstractEventNotifier gate).
        repository.failOnFind = true;
        notifier.notify(event).block(Duration.ofSeconds(1));
    }

    @Test
    void confirmedDownIsSentThenTheReturnToUpIsSentImmediately() throws IOException {
        CapturingHandler handler = new CapturingHandler(200);
        server = startServer(handler);
        StubInstanceRepository repository = new StubInstanceRepository(instance(StatusInfo.ofDown()));
        EventWebhookNotifier notifier = notifier(repository, Duration.ofMillis(150));

        InstanceStatusChangedEvent downEvent = new InstanceStatusChangedEvent(INSTANCE_ID, 1L, Instant.now(),
                StatusInfo.ofDown());
        notifier.notify(downEvent).block(Duration.ofSeconds(2));

        assertThat(handler.requestCount.get()).isEqualTo(1);
        Map<String, Object> downBody = JSON.readValue(handler.lastBody.get(), Map.class);
        assertThat(downBody.get("from")).isEqualTo("UNKNOWN");
        assertThat(downBody.get("to")).isEqualTo("DOWN");

        repository.set(instance(StatusInfo.ofUp()));
        InstanceStatusChangedEvent upEvent = new InstanceStatusChangedEvent(INSTANCE_ID, 2L, Instant.now(),
                StatusInfo.ofUp());
        notifier.notify(upEvent).block(Duration.ofSeconds(2));

        assertThat(handler.requestCount.get()).isEqualTo(2);
        Map<String, Object> upBody = JSON.readValue(handler.lastBody.get(), Map.class);
        assertThat(upBody.get("from")).isEqualTo("DOWN");
        assertThat(upBody.get("to")).isEqualTo("UP");
    }

    @Test
    void downThatRevertsBeforeConfirmationSendsNothing() throws IOException, InterruptedException {
        CapturingHandler handler = new CapturingHandler(200);
        server = startServer(handler);
        StubInstanceRepository repository = new StubInstanceRepository(instance(StatusInfo.ofDown()));
        // A generous confirmation delay keeps this test robust under scheduling jitter: the UP
        // event below is fired essentially immediately after the DOWN one, so the gap only needs
        // to stay well below the delay, not match a tight real-time window.
        EventWebhookNotifier notifier = notifier(repository, Duration.ofSeconds(2));

        InstanceStatusChangedEvent downEvent = new InstanceStatusChangedEvent(INSTANCE_ID, 1L, Instant.now(),
                StatusInfo.ofDown());
        notifier.notify(downEvent).subscribe();

        repository.set(instance(StatusInfo.ofUp()));
        InstanceStatusChangedEvent upEvent = new InstanceStatusChangedEvent(INSTANCE_ID, 2L, Instant.now(),
                StatusInfo.ofUp());
        notifier.notify(upEvent).block(Duration.ofSeconds(1));

        // Wait past the DOWN confirmation delay so its re-check has run.
        Thread.sleep(2500);

        assertThat(handler.requestCount.get()).isZero();
    }

    @Test
    void failingDeliveryIsRetriedAtMostThreeTimesAndNeverPropagatesAnError() throws IOException {
        CapturingHandler handler = new CapturingHandler(500);
        server = startServer(handler);
        StubInstanceRepository repository = new StubInstanceRepository(instance(StatusInfo.ofUp()));
        EventWebhookNotifier notifier = notifier(repository, Duration.ofMillis(100));

        Logger logger = (Logger) LoggerFactory.getLogger(EventWebhookNotifier.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            InstanceStatusChangedEvent event = new InstanceStatusChangedEvent(INSTANCE_ID, 1L, Instant.now(),
                    StatusInfo.ofOutOfService());
            // Must complete without error despite the receiver always failing.
            notifier.notify(event).block(Duration.ofSeconds(10));
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(handler.requestCount.get()).isEqualTo(3);
        List<ILoggingEvent> warnings = appender.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getFormattedMessage()).doesNotContain(SECRET);
    }

    private EventWebhookNotifier notifier(InstanceRepository repository, Duration downConfirmationDelay) {
        SbaEventWebhookProperties properties = new SbaEventWebhookProperties();
        properties.setUrl("http://127.0.0.1:" + PORT + "/events");
        properties.setSecret(SECRET);
        properties.setEnvironment("prod");
        return new EventWebhookNotifier(repository, org.springframework.web.reactive.function.client.WebClient.builder(),
                properties, downConfirmationDelay);
    }

    private static HttpServer startServer(HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/events", handler);
        server.start();
        return server;
    }

    private static Instance instance(StatusInfo statusInfo) {
        Registration registration = Registration.create("front-api", "http://localhost/actuator/health")
                .serviceUrl("http://localhost")
                .managementUrl("http://localhost/actuator")
                .build();
        return Instance.create(INSTANCE_ID).register(registration).withStatusInfo(statusInfo);
    }

    private static final class CapturingHandler implements HttpHandler {

        private final int responseStatus;

        final AtomicInteger requestCount = new AtomicInteger();

        final AtomicReference<byte[]> lastBody = new AtomicReference<>();

        final AtomicReference<com.sun.net.httpserver.Headers> lastHeaders = new AtomicReference<>();

        CapturingHandler(int responseStatus) {
            this.responseStatus = responseStatus;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = exchange.getRequestBody().readAllBytes();
            lastBody.set(body);
            lastHeaders.set(exchange.getRequestHeaders());
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        }
    }

    private static final class StubInstanceRepository implements InstanceRepository {

        private final AtomicReference<Instance> current;

        private volatile boolean failOnFind = false;

        StubInstanceRepository(Instance initial) {
            this.current = new AtomicReference<>(initial);
        }

        void set(Instance instance) {
            current.set(instance);
        }

        @Override
        public Mono<Instance> save(Instance instance) {
            current.set(instance);
            return Mono.just(instance);
        }

        @Override
        public Flux<Instance> findAll() {
            return Flux.just(current.get());
        }

        @Override
        public Mono<Instance> find(InstanceId id) {
            if (failOnFind) {
                return Mono.error(new AssertionError("disabled notifier must never consult the repository"));
            }
            return Mono.justOrEmpty(current.get());
        }

        @Override
        public Flux<Instance> findByName(String name) {
            return Flux.just(current.get());
        }

        @Override
        public Mono<Instance> compute(InstanceId id,
                BiFunction<InstanceId, Instance, Mono<Instance>> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Mono<Instance> computeIfPresent(InstanceId id,
                BiFunction<InstanceId, Instance, Mono<Instance>> remappingFunction) {
            throw new UnsupportedOperationException();
        }
    }
}
