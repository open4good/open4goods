package org.open4goods.api.services.aggregation.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * {@link ScoringBatchedAggregator}: a skipped product only skips that product, any other failure stops the scoring
 * run (GOU-2273), and the log names the service whose {@code init} or {@code done} failed (GOU-2280).
 */
class ScoringBatchedAggregatorTest {

	private final Logger logger = (Logger) LoggerFactory.getLogger(ScoringBatchedAggregator.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private final VerticalConfig vertical = new VerticalConfig();
	private final Set<Product> products = Set.of(new Product(7333394145372L));

	@BeforeEach
	void setUp() {
		vertical.setId("washing-machine");
		appender.start();
		logger.addAppender(appender);
	}

	@AfterEach
	void tearDown() {
		logger.detachAppender(appender);
	}

	/** Records the steps it runs, and throws at the step it is told to fail. */
	private static final class StepService extends AbstractAggregationService {

		private final String failingStep;
		private final List<String> steps;

		private StepService(String failingStep, List<String> steps) {
			super(LoggerFactory.getLogger(ScoringBatchedAggregatorTest.class));
			this.failingStep = failingStep;
			this.steps = steps;
		}

		@Override
		public void init(Collection<Product> datas) {
			run("init");
		}

		@Override
		public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
			if ("skip".equals(failingStep)) {
				steps.add("skip");
				throw new AggregationSkipException("no score for this product");
			}
			run("onProduct");
		}

		@Override
		public void done(Collection<Product> datas, VerticalConfig vConf) {
			run("done");
		}

		private void run(String step) {
			steps.add(step);
			if (step.equals(failingStep)) {
				throw new IllegalStateException(step + " failed");
			}
		}
	}

	private List<String> errors() {
		return appender.list.stream().filter(e -> e.getLevel() == Level.ERROR).map(ILoggingEvent::getFormattedMessage)
				.toList();
	}

	@Test
	void initFailureStopsTheRunAndNamesTheService() {
		List<String> steps = new ArrayList<>();
		ScoringBatchedAggregator aggregator = new ScoringBatchedAggregator(
				List.of(new StepService("init", steps), new StepService(null, steps)));

		assertThatThrownBy(() -> aggregator.score(products, vertical)).hasMessage("init failed");

		assertThat(steps).containsExactly("init");
		assertThat(errors()).singleElement().asString()
				.contains("Initialisation", "StepService", "washing-machine", "init failed");
	}

	@Test
	void doneFailureStopsTheRunAndNamesTheService() {
		List<String> steps = new ArrayList<>();
		ScoringBatchedAggregator aggregator = new ScoringBatchedAggregator(
				List.of(new StepService("done", steps), new StepService(null, steps)));

		assertThatThrownBy(() -> aggregator.score(products, vertical)).hasMessage("done failed");

		assertThat(steps).containsExactly("init", "onProduct", "done");
		assertThat(errors()).singleElement().asString()
				.contains("Post computing", "StepService", "washing-machine", "done failed");
	}

	@Test
	void productFailureStopsTheRunAndNamesTheProductAndTheService() {
		List<String> steps = new ArrayList<>();
		ScoringBatchedAggregator aggregator = new ScoringBatchedAggregator(
				List.of(new StepService("onProduct", steps), new StepService(null, steps)));

		assertThatThrownBy(() -> aggregator.score(products, vertical)).hasMessage("onProduct failed");

		assertThat(steps).containsExactly("init", "onProduct");
		assertThat(errors()).singleElement().asString().contains("7333394145372", "StepService");
	}

	@Test
	void skippedProductDoesNotStopTheRun() {
		List<String> steps = new ArrayList<>();
		ScoringBatchedAggregator aggregator = new ScoringBatchedAggregator(
				List.of(new StepService("skip", steps), new StepService(null, steps)));

		aggregator.score(products, vertical);

		assertThat(steps).containsExactly("init", "skip", "done", "init", "onProduct", "done");
		assertThat(errors()).isEmpty();
	}
}
