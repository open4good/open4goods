package org.open4goods.nudgerfrontapi.service.css;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService.MerchantDomain;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.protobuf.util.JsonFormat;
import com.google.shopping.css.v1.CssProductInput;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Synchronisation of the eligible products with Google CSS (GOU-2081). Compares each run with the last successful
 * send ({@link CssSyncState}, fingerprint of the {@link CssProductInput} per {@code rawProvidedId}) and sends only:
 * <ul>
 * <li>an insert (upsert) for a new or modified eligible product;</li>
 * <li>a delete for a product that is no longer eligible (or no longer in the index, once its vertical was fully
 * scanned);</li>
 * <li>an insert again, before {@code expirationDate}, for an unchanged product still eligible
 * ({@code front.css.sync.resend-before}). A product losing its fresh offers is not sent again and expires by
 * itself.</li>
 * </ul>
 * A failed call leaves the state of that product untouched, so the next run retries it without resending the rest.
 * Metrics: {@code css.products.sent|rejected|deleted|expired} and {@code css.products.sync.duration}.
 */
@Service
@ConditionalOnProperty(prefix = "front.css", name = { "enabled", "sync.enabled" }, havingValue = "true")
public class CssSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CssSyncService.class);
    private static final int SAVE_EVERY = 50;

    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsConfigService;
    private final MerchantDomainsService merchantDomainsService;
    private final CssEligibilityService eligibilityService;
    private final CssProductInputMapper mapper;
    private final CssProductsClient client;
    private final CssProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final LongConsumer sleeper;
    private final CssSyncStateStore store;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile CssSyncReport last;

    /**
     * @param productRepository      product index
     * @param verticalsConfigService vertical configurations
     * @param merchantDomainsService merchant domain rules
     * @param eligibilityService     eligibility rules
     * @param mapper                 product to CssProductInput mapping
     * @param client                 CSS client (real or simulated)
     * @param properties             CSS settings
     * @param meterRegistry          metrics
     */
    public CssSyncService(ProductRepository productRepository, VerticalsConfigService verticalsConfigService,
            MerchantDomainsService merchantDomainsService, CssEligibilityService eligibilityService,
            CssProductInputMapper mapper, CssProductsClient client, CssProperties properties,
            MeterRegistry meterRegistry) {
        this(productRepository, verticalsConfigService, merchantDomainsService, eligibilityService, mapper, client,
                properties, meterRegistry, Clock.systemUTC(), CssSyncService::sleepMillis);
    }

    CssSyncService(ProductRepository productRepository, VerticalsConfigService verticalsConfigService,
            MerchantDomainsService merchantDomainsService, CssEligibilityService eligibilityService,
            CssProductInputMapper mapper, CssProductsClient client, CssProperties properties,
            MeterRegistry meterRegistry, Clock clock, LongConsumer sleeper) {
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
        this.merchantDomainsService = merchantDomainsService;
        this.eligibilityService = eligibilityService;
        this.mapper = mapper;
        this.client = client;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.sleeper = sleeper;
        this.store = new CssSyncStateStore(Path.of(properties.getSync().getStateFile()));
    }

    /**
     * @return the report of the last run since startup, or null
     */
    public CssSyncReport lastReport() {
        return last;
    }

    /**
     * Scheduled entry point; failures are logged, never propagated to the scheduler.
     */
    @Scheduled(initialDelayString = "${front.css.sync.initial-delay:PT5M}",
            fixedDelayString = "${front.css.sync.interval:PT6H}")
    public void scheduledRun() {
        try {
            run();
        } catch (Exception e) {
            LOGGER.error("CSS synchronisation failed", e);
        }
    }

    /**
     * Runs one synchronisation synchronously.
     *
     * @return the report, or null when another run (this instance or another one sharing the state file) is in progress
     * @throws IOException when the state cannot be read or written
     */
    public CssSyncReport run() throws IOException {
        if (!running.compareAndSet(false, true)) {
            LOGGER.warn("CSS synchronisation skipped: a run is already in progress");
            return null;
        }
        try (CssSyncStateStore.Lock lock = store.tryLock()) {
            if (lock == null) {
                LOGGER.warn("CSS synchronisation skipped: state file locked by another instance");
                return null;
            }
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                return doRun();
            } finally {
                sample.stop(meterRegistry.timer("css.products.sync.duration"));
            }
        } finally {
            running.set(false);
        }
    }

    /** Mutable counters and throttle of one run. */
    private final class Run {
        final CssProperties.Sync sync = properties.getSync();
        final Instant now = clock.instant();
        final CssSyncState state;
        final Map<String, Long> rejected = new TreeMap<>();
        long examined, inserted, renewed, deleted, unchanged, expired, calls, sinceSave;
        int consecutiveFailures;
        boolean capped, aborted;
        Instant lastCall;

        Run(CssSyncState state) {
            this.state = state;
        }

        /** @return false when the cap is reached or the run is aborted: no more call may be made */
        boolean canCall() {
            if (aborted) {
                return false;
            }
            if (sync.getMaxProductsPerRun() > 0 && calls >= sync.getMaxProductsPerRun()) {
                capped = true;
                return false;
            }
            return true;
        }

        void throttle() {
            if (sync.getMaxRequestsPerSecond() > 0) {
                long minIntervalMs = (long) (1000 / sync.getMaxRequestsPerSecond());
                Instant earliest = lastCall == null ? null : lastCall.plusMillis(minIntervalMs);
                Instant current = clock.instant();
                if (earliest != null && current.isBefore(earliest)) {
                    sleeper.accept(Duration.between(current, earliest).toMillis());
                }
            }
            lastCall = clock.instant();
            calls++;
        }

        void failed(CssClientException e, String id) {
            rejected.merge(e.reason(), 1L, Long::sum);
            meterRegistry.counter("css.products.rejected", "reason", e.reason()).increment();
            LOGGER.warn("CSS call rejected for {}: {} ({})", id, e.reason(), e.getMessage());
            if (++consecutiveFailures >= sync.getMaxConsecutiveFailures()) {
                aborted = true;
                LOGGER.error("CSS synchronisation aborted after {} consecutive failures", consecutiveFailures);
            }
        }
    }

    private CssSyncReport doRun() throws IOException {
        Run run = new Run(store.load());
        Function<String, MerchantDomain> resolver = merchantDomainsService.domainResolver();
        Set<String> selected = run.sync.getVerticals();
        Set<String> seen = new HashSet<>();
        Set<String> scannedVerticals = new HashSet<>();
        try {
            // Inputs already past their expiration: Google dropped them, they are not deleted but sent again if still eligible
            run.state.entries().values().stream().filter(e -> Instant.parse(e.expiresAt()).isBefore(run.now))
                    .forEach(e -> {
                        run.expired++;
                        meterRegistry.counter("css.products.expired").increment();
                    });
            for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault(true)) {
                if (!selected.isEmpty() && !selected.contains(vertical.getId())) {
                    continue;
                }
                if (scanVertical(run, vertical, resolver, seen)) {
                    scannedVerticals.add(vertical.getId());
                }
                if (!run.canCall()) {
                    break;
                }
            }
            // Products gone from the index of a fully scanned vertical are no longer eligible
            for (var entry : List.copyOf(run.state.entries().entrySet())) {
                if (!run.canCall()) {
                    break;
                }
                if (!seen.contains(entry.getKey()) && scannedVerticals.contains(entry.getValue().vertical())) {
                    delete(run, entry.getKey());
                }
            }
        } finally {
            store.save(run.state);
        }
        CssSyncReport report = new CssSyncReport(run.examined, run.inserted, run.renewed, run.deleted, run.unchanged,
                run.expired, run.rejected, run.capped, run.aborted);
        last = report;
        LOGGER.info("CSS synchronisation: {}", report);
        return report;
    }

    /** @return true when the vertical was entirely scanned (neither capped nor aborted) */
    private boolean scanVertical(Run run, VerticalConfig vertical, Function<String, MerchantDomain> resolver,
            Set<String> seen) throws IOException {
        try (Stream<Product> products = productRepository.exportAll(vertical.getId())) {
            for (var it = products.iterator(); it.hasNext();) {
                if (!run.canCall()) {
                    return false;
                }
                Product product = it.next();
                run.examined++;
                CssEligibility result = eligibilityService.evaluate(product, vertical, resolver);
                String id = product.gtin();
                if (id != null) {
                    seen.add(id);
                }
                if (!result.eligible()) {
                    if (id != null && run.state.entries().containsKey(id)) {
                        delete(run, id);
                    }
                    continue;
                }
                CssProductInput input = mapper.map(product, vertical);
                String fingerprint = fingerprint(input);
                CssSyncState.Entry known = run.state.entries().get(id);
                boolean changed = known == null || !known.fingerprint().equals(fingerprint);
                boolean renew = known != null && !changed
                        && Instant.parse(known.expiresAt()).minus(run.sync.getResendBefore()).isBefore(run.now);
                if (!changed && !renew) {
                    run.unchanged++;
                    continue;
                }
                insert(run, vertical.getId(), input, fingerprint, renew);
            }
            return !run.capped && !run.aborted;
        }
    }

    private void insert(Run run, String vertical, CssProductInput input, String fingerprint, boolean renew) {
        run.throttle();
        try {
            client.insert(input);
        } catch (CssClientException e) {
            run.failed(e, input.getRawProvidedId());
            return;
        }
        run.consecutiveFailures = 0;
        Instant expires = Instant.ofEpochSecond(input.getAttributes().getExpirationDate().getSeconds());
        run.state.entries().put(input.getRawProvidedId(),
                new CssSyncState.Entry(fingerprint, vertical, run.now.toString(), expires.toString()));
        if (renew) {
            run.renewed++;
        } else {
            run.inserted++;
        }
        meterRegistry.counter("css.products.sent", "type", renew ? "renewal" : "insert").increment();
        checkpoint(run);
    }

    private void delete(Run run, String id) {
        CssSyncState.Entry known = run.state.entries().get(id);
        if (known != null && Instant.parse(known.expiresAt()).isBefore(run.now)) {
            // Already expired at Google: nothing to delete, only forget it
            run.state.entries().remove(id);
            return;
        }
        run.throttle();
        try {
            client.delete(id, properties.getContentLanguage(), properties.getFeedLabel());
        } catch (CssClientException e) {
            if (!"NOT_FOUND".equals(e.reason())) {
                run.failed(e, id);
                return;
            }
            // Already absent at Google: same outcome as a delete
        }
        run.consecutiveFailures = 0;
        run.state.entries().remove(id);
        run.deleted++;
        meterRegistry.counter("css.products.deleted").increment();
        checkpoint(run);
    }

    /** Persists the state regularly so an interrupted run does not resend what was already accepted. */
    private void checkpoint(Run run) {
        if (++run.sinceSave >= SAVE_EVERY) {
            run.sinceSave = 0;
            try {
                store.save(run.state);
            } catch (IOException e) {
                LOGGER.warn("CSS synchronisation state checkpoint failed", e);
            }
        }
    }

    /**
     * Fingerprint of an input ignoring its {@code expirationDate}, which changes at every mapping.
     */
    static String fingerprint(CssProductInput input) {
        CssProductInput stable = input.toBuilder()
                .setAttributes(input.getAttributes().toBuilder().clearExpirationDate()).build();
        try {
            String text = JsonFormat.printer().omittingInsignificantWhitespace().print(stable);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to fingerprint " + input.getRawProvidedId(), e);
        }
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
