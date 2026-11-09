package org.open4goods.api.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.impactscore.ImpactScoreSnapshot;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.impactscore.ImpactScoreSnapshotRepository;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

class BatchServiceImpactScoreSnapshotTest {

	private static VerticalConfig vertical() {
		ImpactScoreConfig config = new ImpactScoreConfig();
		config.setCriteriasPonderation(Map.of("WARRANTY", 1.0));
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("tv");
		vertical.setImpactScoreConfig(config);
		return vertical;
	}

	private static BatchService service(ImpactScoreSnapshotRepository repository) {
		BatchService service = new BatchService(null, null, mock(VerticalsConfigService.class), mock(ProductRepository.class),
				null, null, null, null, mock(ApiProperties.class));
		service.setImpactScoreSnapshotRepository(repository);
		return service;
	}

	@Test
	void storesTheSnapshotWhenTheMonthHasNone() {
		ImpactScoreSnapshotRepository repository = mock(ImpactScoreSnapshotRepository.class);
		service(repository).snapshotImpactScore(vertical(), List.of(new Product(1L)));
		// not the 1st: stored only if absent, which it is (mock answers false)
		verify(repository).save(any(ImpactScoreSnapshot.class));
	}

	@Test
	void doesNotRewriteAnExistingMonthOutsideTheFirst() {
		ImpactScoreSnapshotRepository repository = mock(ImpactScoreSnapshotRepository.class);
		when(repository.exists(any())).thenReturn(true);
		service(repository).snapshotImpactScore(vertical(), List.of(new Product(1L)));
		boolean firstOfMonth = java.time.LocalDate.now(java.time.ZoneOffset.UTC).getDayOfMonth() == 1;
		if (firstOfMonth) {
			verify(repository).save(any(ImpactScoreSnapshot.class));
		} else {
			verify(repository, never()).save(any(ImpactScoreSnapshot.class));
		}
	}

	@Test
	void aStorageFailureNeverStopsTheBatch() {
		ImpactScoreSnapshotRepository repository = mock(ImpactScoreSnapshotRepository.class);
		org.mockito.Mockito.doThrow(new IllegalStateException("es down")).when(repository).save(any());
		service(repository).snapshotImpactScore(vertical(), List.of(new Product(1L)));
	}
}
