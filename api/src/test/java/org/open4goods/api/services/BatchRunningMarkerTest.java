package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Marker read by the beta deployment to postpone the api restart during a full batch (GOU-2106).
 */
class BatchRunningMarkerTest {

	@TempDir
	Path dir;

	@Test
	void writesPidAndStartThenDeletesTheMarker() throws Exception {
		Path file = dir.resolve("run/api-batch.marker");
		BatchRunningMarker marker = new BatchRunningMarker(file.toString());

		marker.begin();

		assertThat(Files.readAllLines(file)).hasSize(2)
				.contains("pid=" + ProcessHandle.current().pid())
				.anyMatch(line -> line.startsWith("started="));
		assertThat(dir.resolve("run/api-batch.marker.tmp")).doesNotExist();

		marker.end();

		assertThat(file).doesNotExist();
	}

	@Test
	void keepsTheMarkerUntilTheLastOverlappingBatchEnds() {
		Path file = dir.resolve("api-batch.marker");
		BatchRunningMarker marker = new BatchRunningMarker(file.toString());

		marker.begin();
		marker.begin();
		marker.end();

		assertThat(file).exists();

		marker.end();

		assertThat(file).doesNotExist();
	}

	@Test
	void blankPathDisablesTheMarker() {
		BatchRunningMarker marker = new BatchRunningMarker(" ");

		marker.begin();
		marker.end();

		assertThat(marker.getFile()).isNull();
	}

	@Test
	void anUnwritablePathNeverFailsTheBatch() throws Exception {
		Path notADirectory = Files.writeString(dir.resolve("file"), "x");
		BatchRunningMarker marker = new BatchRunningMarker(notADirectory.resolve("api-batch.marker").toString());

		marker.begin();
		marker.end();

		assertThat(notADirectory).hasContent("x");
	}

	@Test
	void fullBatchHoldsTheMarkerAndRemovesItEvenOnFailure() {
		Path file = dir.resolve("api-batch.marker");
		VerticalConfig tv = new VerticalConfig();
		tv.setId("tv");
		VerticalsConfigService verticals = mock(VerticalsConfigService.class);
		when(verticals.getConfigsWithoutDefault()).thenReturn(List.of(tv));
		ApiProperties properties = mock(ApiProperties.class);
		when(properties.getBatchRunningMarker()).thenReturn(file.toString());
		BatchService service = spy(new BatchService(null, null, verticals, mock(ProductRepository.class), null, null,
				null, null, properties));

		List<Boolean> presentDuringVertical = new ArrayList<>();
		doAnswer(invocation -> {
			presentDuringVertical.add(Files.exists(file));
			return null;
		}).when(service).batch(any(VerticalConfig.class));

		service.batch();

		assertThat(presentDuringVertical).containsExactly(true);
		assertThat(file).doesNotExist();

		doAnswer(invocation -> {
			throw new IllegalStateException("vertical failed");
		}).when(service).batch(any(VerticalConfig.class));

		assertThatThrownBy(service::batch).isInstanceOf(IllegalStateException.class);
		assertThat(file).doesNotExist();
	}
}
