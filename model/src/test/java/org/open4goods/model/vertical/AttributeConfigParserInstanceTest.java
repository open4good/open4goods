package org.open4goods.model.vertical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.exceptions.ResourceNotFoundException;

/**
 * Shared parser instances of {@link AttributeConfig#getParserInstance()} (GOU-2279).
 */
class AttributeConfigParserInstanceTest {

    private static final int THREADS = 16;

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concurrentCallsShareOneParserInstance() throws Exception {
        AttributeConfig config = withParser(CountingParser.class.getName());
        CountDownLatch start = new CountDownLatch(1);
        Callable<AttributeParser> call = () -> {
            start.await();
            return config.getParserInstance();
        };

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<AttributeParser>> futures = IntStream.range(0, THREADS)
                    .mapToObj(i -> executor.submit(call))
                    .toList();
            start.countDown();

            AttributeParser first = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<AttributeParser> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS)).isSameAs(first);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(CountingParser.INSTANCES).hasValue(1);
        assertThat(withParser(CountingParser.class.getName()).getParserInstance())
                .isSameAs(config.getParserInstance());
    }

    @Test
    void unknownParserClassIsReportedAsResourceNotFound() {
        AttributeConfig config = withParser("org.open4goods.model.vertical.NoSuchParser");

        assertThatThrownBy(config::getParserInstance)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void missingParserClassIsReportedAsResourceNotFound() {
        AttributeConfig config = withParser(null);

        assertThatThrownBy(config::getParserInstance).isInstanceOf(ResourceNotFoundException.class);
    }

    private static AttributeConfig withParser(String clazz) {
        AttributeParserConfig parser = new AttributeParserConfig();
        parser.setClazz(clazz);
        AttributeConfig config = new AttributeConfig();
        config.setKey("WEIGHT");
        config.setParser(parser);
        return config;
    }

    /**
     * Parser whose constructor is slow and counts its calls, used only by this test so that the shared static cache
     * holds no other entry for it.
     */
    public static class CountingParser extends AttributeParser {

        static final AtomicInteger INSTANCES = new AtomicInteger();

        public CountingParser() throws InterruptedException {
            INSTANCES.incrementAndGet();
            // Widens the window in which a check-then-put cache would build a second instance.
            Thread.sleep(50);
        }

        @Override
        public String parse(ProductAttribute attr, AttributeConfig attributeConfig, VerticalConfig verticalConfig) {
            return attr.getValue();
        }
    }
}
