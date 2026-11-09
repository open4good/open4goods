package org.open4goods.services.productrepository.impactscore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.impactscore.ImpactScoreSnapshot;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

class ImpactScoreSnapshotRepositoryTest {

	private static ImpactScoreSnapshot snapshot() {
		return new ImpactScoreSnapshot("tv_2026-11", "tv", "2026-11", 1L, 2, "rev", 0, 0, 0, Map.of(), 0, Map.of(),
				Map.of(), null, Map.of(), 0, null, List.of());
	}

	@Test
	void savingTheSameMonthTwiceWritesTheSameDocumentId() {
		ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
		IndexOperations index = mock(IndexOperations.class);
		when(operations.indexOps(ImpactScoreSnapshot.class)).thenReturn(index);
		when(index.exists()).thenReturn(false, true);

		ImpactScoreSnapshotRepository repository = new ImpactScoreSnapshotRepository(operations);
		repository.save(snapshot());
		repository.save(snapshot());

		verify(index, times(1)).createWithMapping();
		// ES replaces a document of the same id: two saves of one month leave one document
		verify(operations, times(2)).save(snapshot());
	}

	@Test
	void missingIndexReadsAsEmpty() {
		ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
		IndexOperations index = mock(IndexOperations.class);
		when(operations.indexOps(ImpactScoreSnapshot.class)).thenReturn(index);
		when(index.exists()).thenReturn(false);

		ImpactScoreSnapshotRepository repository = new ImpactScoreSnapshotRepository(operations);
		assertThat(repository.find("tv", null, null)).isEmpty();
		assertThat(repository.exists("tv_2026-11")).isFalse();
		verify(operations, never()).exists(any(String.class), any(Class.class));
	}
}
