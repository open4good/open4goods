package org.open4goods.api.services;

import java.time.Duration;
import java.util.Optional;

import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * Applies the settings and mapping of the products index ({@value Product#DEFAULT_REPO}) once the api context is
 * created (GOU-1835).
 * <p>
 * {@link Product} no longer lets Spring Data Elasticsearch create its index or write its mapping in the repository
 * constructor ({@code createIndex = false}, {@code alwaysWriteMapping = false}): a transient Elasticsearch failure
 * there failed the whole startup of ui and api (GOU-1812). The api is now the only application that writes this
 * mapping, here, as an {@link ApplicationRunner}:
 * <ul>
 * <li>index absent: created with {@code product-settings.json} and {@code product-mappings.json};</li>
 * <li>index present: {@code product-mappings.json} is put on it (a mapping change only adds fields).</li>
 * </ul>
 * At most {@value #MAX_ATTEMPTS} attempts, the delay between two of them doubling from
 * {@code api.product-index-mapping.retry-delay} (default 2 s). Each failure is logged at WARN with its full stack
 * trace, {@code Caused by} included. After the last one, an ERROR is logged and the api keeps starting: the index
 * exists in production and its current mapping stays usable.
 */
@Component
public class ProductIndexMappingRunner implements ApplicationRunner {

    /** Maximum number of attempts before giving up. */
    static final int MAX_ATTEMPTS = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductIndexMappingRunner.class);

    /** What a successful attempt did to the products index. */
    enum Action {
        /** The index was absent and has been created with its settings and mapping. */
        CREATED,
        /** The index was present and its mapping has been put again. */
        MAPPING_UPDATED
    }

    /** Waits between two attempts; replaced in tests. */
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration delay) throws InterruptedException;
    }

    private final ElasticsearchOperations elasticsearchOperations;
    private final Duration retryDelay;
    private final Sleeper sleeper;

    /**
     * @param elasticsearchOperations the api Elasticsearch template
     * @param retryDelay              delay before the second attempt, doubled before the third
     */
    @Autowired
    public ProductIndexMappingRunner(ElasticsearchOperations elasticsearchOperations,
            @Value("${api.product-index-mapping.retry-delay:PT2S}") Duration retryDelay) {
        this(elasticsearchOperations, retryDelay, delay -> Thread.sleep(delay));
    }

    ProductIndexMappingRunner(ElasticsearchOperations elasticsearchOperations, Duration retryDelay, Sleeper sleeper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.retryDelay = retryDelay;
        this.sleeper = sleeper;
    }

    @Override
    public void run(ApplicationArguments args) {
        applyMapping();
    }

    /**
     * Creates the products index or puts its mapping, retrying on failure. Never throws.
     *
     * @return the action applied, or empty when every attempt failed
     */
    Optional<Action> applyMapping() {
        long start = System.nanoTime();
        Duration delay = retryDelay;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Action action = applyOnce();
                LOGGER.info("Products index mapping: index {}, action {}, attempt {}/{}, {} ms", Product.DEFAULT_REPO,
                        action, attempt, MAX_ATTEMPTS, elapsedMillis(start));
                return Optional.of(action);
            } catch (RuntimeException e) {
                // Last argument without placeholder: SLF4J prints the full stack trace, Caused by included.
                LOGGER.warn("Products index mapping: index {}, attempt {}/{} failed", Product.DEFAULT_REPO, attempt,
                        MAX_ATTEMPTS, e);
            }
            if (attempt < MAX_ATTEMPTS) {
                try {
                    sleeper.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Products index mapping: index {}, interrupted after {} attempt(s), {} ms, mapping not"
                            + " applied; the api keeps starting", Product.DEFAULT_REPO, attempt, elapsedMillis(start));
                    return Optional.empty();
                }
                delay = delay.multipliedBy(2);
            }
        }
        LOGGER.error("Products index mapping: index {}, failed after {} attempts, {} ms, mapping not applied; the api"
                + " keeps starting (stack traces in the WARN lines above)", Product.DEFAULT_REPO, MAX_ATTEMPTS,
                elapsedMillis(start));
        return Optional.empty();
    }

    private Action applyOnce() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(Product.class);
        if (!indexOperations.exists()) {
            requireAcknowledged(indexOperations.createWithMapping(), "index creation");
            return Action.CREATED;
        }
        requireAcknowledged(indexOperations.putMapping(), "put mapping");
        return Action.MAPPING_UPDATED;
    }

    private static void requireAcknowledged(boolean acknowledged, String operation) {
        if (!acknowledged) {
            throw new IllegalStateException(operation + " not acknowledged by Elasticsearch");
        }
    }

    private static long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
