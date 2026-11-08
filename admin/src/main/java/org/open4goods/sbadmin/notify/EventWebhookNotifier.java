package org.open4goods.sbadmin.notify;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import de.codecentric.boot.admin.server.notify.AbstractStatusChangeNotifier;

import org.open4goods.sbadmin.config.properties.SbaEventWebhookProperties;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.json.JsonMapper;

/**
 * Signs and forwards every SBA instance status change to the event-gateway, so that Ops can be
 * woken on {@code DOWN}. Registered as a plain {@code Notifier} bean alongside the standard
 * {@code MailNotifier}, which it leaves untouched.
 * <p>
 * A transition to {@code DOWN} is held for {@link SbaEventWebhookProperties#getDownConfirmationDelay()}
 * and re-checked against the {@link InstanceRepository} before sending, to avoid paging Ops for a
 * flap that resolves on its own. Every other transition, including the return to {@code UP}, is
 * sent immediately. Delivery never surfaces an error back to SBA: failures are retried a bounded
 * number of times, then logged and swallowed.
 * <p>
 * The last status <em>reported</em> to the gateway is tracked independently of
 * {@link AbstractStatusChangeNotifier}'s own {@code lastStatuses} map: that map is updated as soon as
 * {@code notify()} completes, i.e. immediately for a held {@code DOWN} transition, before the delay even
 * elapses. Relying on it here would let an intervening flap mark the pending {@code DOWN} as already
 * reported and mask the real outage that follows. {@link #lastReportedStatus} only advances when this
 * notifier itself decides to send, so {@link #shouldNotify} and {@link #doNotify} stay consistent with
 * what was actually delivered.
 */
public class EventWebhookNotifier extends AbstractStatusChangeNotifier {

    private static final Logger log = LoggerFactory.getLogger(EventWebhookNotifier.class);

    private static final Set<String> ALLOWED_ENVIRONMENTS = Set.of("prod", "beta");

    private static final int MAX_ATTEMPTS = 3;

    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(5);

    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);

    private final InstanceRepository repository;

    private final WebClient webClient;

    private final SbaEventWebhookProperties properties;

    private final Duration downConfirmationDelay;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** Last status reported to the gateway per instance, advanced only when a send is actually decided. */
    private final ConcurrentHashMap<InstanceId, String> lastReportedStatus = new ConcurrentHashMap<>();

    public EventWebhookNotifier(InstanceRepository repository, WebClient.Builder webClientBuilder,
            SbaEventWebhookProperties properties) {
        this(repository, webClientBuilder, properties, properties.getDownConfirmationDelay());
    }

    EventWebhookNotifier(InstanceRepository repository, WebClient.Builder webClientBuilder,
            SbaEventWebhookProperties properties, Duration downConfirmationDelay) {
        super(repository);
        this.repository = repository;
        this.webClient = webClientBuilder.build();
        this.properties = properties;
        this.downConfirmationDelay = downConfirmationDelay;

        boolean enabled = properties.isConfigured();
        if (enabled && !ALLOWED_ENVIRONMENTS.contains(properties.getEnvironment())) {
            throw new IllegalStateException(
                    "o4g.sba.event-webhook.environment must be 'prod' or 'beta' when the notifier is configured");
        }
        setEnabled(enabled);
        if (enabled) {
            log.info("SBA event webhook notifier enabled (environment={})", properties.getEnvironment());
        } else {
            log.info("SBA event webhook notifier disabled: o4g.sba.event-webhook.url/secret not configured");
        }
    }

    @Override
    protected boolean shouldNotify(InstanceEvent event, Instance instance) {
        if (!(event instanceof InstanceStatusChangedEvent statusEvent)) {
            return false;
        }
        String reported = lastReportedStatus.get(event.getInstance());
        String to = statusEvent.getStatusInfo().getStatus();
        if (reported == null && StatusInfo.STATUS_UP.equals(to)) {
            // First UP after registration: no incident to open or close, so nothing is sent, but it is
            // recorded so that the next outage is reported as from=UP rather than from=UNKNOWN.
            lastReportedStatus.put(event.getInstance(), to);
            return false;
        }
        return !(reported == null ? StatusInfo.STATUS_UNKNOWN : reported).equals(to);
    }

    @Override
    protected Mono<Void> doNotify(InstanceEvent event, Instance instance) {
        if (!(event instanceof InstanceStatusChangedEvent statusEvent)) {
            return Mono.empty();
        }
        InstanceId instanceId = event.getInstance();
        String from = lastReportedStatus.getOrDefault(instanceId, StatusInfo.STATUS_UNKNOWN);
        String to = statusEvent.getStatusInfo().getStatus();

        if (StatusInfo.STATUS_DOWN.equals(to)) {
            Instant transitionTimestamp = event.getTimestamp();
            // repository.find(...) must be deferred: Mono.then(Mono) evaluates its argument eagerly
            // at chain-construction time, which would re-check the instance before the delay even
            // elapses instead of after it.
            return Mono.delay(downConfirmationDelay)
                    .then(Mono.defer(() -> repository.find(instanceId)))
                    // Compare against this event's own transition timestamp, not just "is it still
                    // DOWN": a later DOWN->UP->DOWN flap keeps the instance down but starts its own
                    // timer, and only that newer timer's expiry should report it.
                    .filter(current -> current.getStatusInfo().isDown()
                            && transitionTimestamp.equals(current.getStatusTimestamp()))
                    .flatMap(current -> {
                        lastReportedStatus.put(instanceId, to);
                        return sendEvent(event, instance, from, to);
                    })
                    .then();
        }
        lastReportedStatus.put(instanceId, to);
        return sendEvent(event, instance, from, to);
    }

    private Mono<Void> sendEvent(InstanceEvent event, Instance instance, String from, String to) {
        EventWebhookPayload payload = buildPayload(event, instance, from, to);
        byte[] body = jsonMapper.writeValueAsBytes(payload);

        return Mono.defer(() -> dispatch(body))
                .timeout(ATTEMPT_TIMEOUT)
                .retryWhen(Retry.backoff(MAX_ATTEMPTS - 1, RETRY_BACKOFF))
                .onErrorResume(error -> {
                    log.warn("SBA event webhook delivery failed for instance {} ({} -> {}) after {} attempt(s): {}",
                            instance.getId(), from, to, MAX_ATTEMPTS, error.toString());
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> dispatch(byte[] body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = "sha256=" + hmacSha256Hex(properties.getSecret(), timestamp, body);

        return webClient.post()
                .uri(properties.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Event-Timestamp", timestamp)
                .header("X-Event-Signature", signature)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .then();
    }

    private EventWebhookPayload buildPayload(InstanceEvent event, Instance instance, String from, String to) {
        String instanceId = event.getInstance().getValue();
        String eventId = eventId(instanceId, event.getVersion());
        String at = DateTimeFormatter.ISO_INSTANT.format(event.getTimestamp());
        String application = instance.getRegistration().getName();
        return new EventWebhookPayload("sba", properties.getEnvironment(), application, instanceId, from, to, at,
                eventId);
    }

    /** UUID v3 (name-based) over {@code instanceId:version}, stable across retries of the same transition. */
    static String eventId(String instanceId, long version) {
        return UUID.nameUUIDFromBytes((instanceId + ":" + version).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** {@code HMAC-SHA256(secret, "<timestamp>." + body)}, hex-encoded. */
    static String hmacSha256Hex(String secret, String timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            byte[] digest = mac.doFinal(body);
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256 signature", e);
        }
    }
}
