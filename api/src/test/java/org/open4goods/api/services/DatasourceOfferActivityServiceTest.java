package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.model.price.DatasourceOfferActivity;

/**
 * Tests {@link DatasourceOfferActivityService} (GOU-2037).
 */
class DatasourceOfferActivityServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@TempDir
	Path folder;

	@Test
	void keepsTheMostRecentOfferDatePerDatasource() {
		DatasourceOfferActivityService service = new DatasourceOfferActivityService(folder.resolve("a.json"), CLOCK);

		service.record("darty.com", 2_000L);
		service.record("darty.com", 1_000L);
		service.record("fnac.com", 1_500L);
		service.record("fnac.com", 3_000L);
		service.record(" ", 9_000L);
		service.record(null, 9_000L);
		service.record("cdiscount.com", null);

		assertThat(service.report().trackedSince()).isEqualTo(NOW.toEpochMilli());
		assertThat(service.report().datasources()).containsExactly(
				new DatasourceOfferActivity("darty.com", 2_000L),
				new DatasourceOfferActivity("fnac.com", 3_000L));
	}

	@Test
	void flushWritesTheStateThatARestartReadsBack() {
		Path file = folder.resolve("sub/activity.json");
		DatasourceOfferActivityService service = new DatasourceOfferActivityService(file, CLOCK);
		service.record("darty.com", 2_000L);

		service.flush();

		assertThat(file).exists();
		assertThat(file.resolveSibling("activity.json.tmp")).doesNotExist();
		Clock later = Clock.offset(CLOCK, java.time.Duration.ofDays(3));
		DatasourceOfferActivityService restarted = new DatasourceOfferActivityService(file, later);
		// trackedSince survives the restart: the dates of the previous run are still known
		assertThat(restarted.report().trackedSince()).isEqualTo(NOW.toEpochMilli());
		assertThat(restarted.report().datasources()).containsExactly(new DatasourceOfferActivity("darty.com", 2_000L));
	}

	@Test
	void flushDoesNotRewriteAnUnchangedState() throws Exception {
		Path file = folder.resolve("activity.json");
		DatasourceOfferActivityService service = new DatasourceOfferActivityService(file, CLOCK);
		service.record("darty.com", 2_000L);
		service.flush();
		Files.writeString(file, "{\"trackedSince\":1,\"datasources\":[]}", StandardCharsets.UTF_8);

		// An older date moves nothing: no write
		service.record("darty.com", 1_000L);
		service.flush();

		assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("{\"trackedSince\":1,\"datasources\":[]}");
	}

	@Test
	void writesNoFileBeforeTheFirstOffer() {
		Path file = folder.resolve("activity.json");
		DatasourceOfferActivityService service = new DatasourceOfferActivityService(file, CLOCK);

		service.destroy();

		assertThat(file).doesNotExist();
	}

	@Test
	void unreadableFileStartsAnEmptyStateTrackedFromNow() throws Exception {
		Path file = folder.resolve("activity.json");
		Files.writeString(file, "not json", StandardCharsets.UTF_8);

		DatasourceOfferActivityService service = new DatasourceOfferActivityService(file, CLOCK);

		assertThat(service.report().trackedSince()).isEqualTo(NOW.toEpochMilli());
		assertThat(service.report().datasources()).isEmpty();
	}
}
