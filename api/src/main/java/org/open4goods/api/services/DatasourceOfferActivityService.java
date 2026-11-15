package org.open4goods.api.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.price.DatasourceOfferActivity;
import org.open4goods.model.price.DatasourceOfferActivityReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PreDestroy;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records, as offers are aggregated, the date of the last offer each datasource brought to a product (GOU-2037).
 * <p>
 * {@code price.offers} is not indexed in Elasticsearch, so "which merchants still have an active offer" cannot be
 * aggregated there, and scanning the tens of millions of products with offers is not an option. The datafragment
 * workers call {@link #record(String, Long)} for every fragment that becomes an offer
 * ({@code PriceAggregationService.isOffer}); front-api reads {@link #report()} through
 * {@code GET /merchants/offer-activity} and applies its own offer freshness rule.
 * </p>
 * <p>
 * The dates are kept in memory and written to a JSON file every few minutes and at shutdown (through a temporary
 * file renamed over it), and read back at startup. {@code trackedSince} is the start of the recording that wrote the
 * file: a datasource whose last offer predates it is unknown, never reported as inactive by mistake.
 * </p>
 */
public class DatasourceOfferActivityService {

	private static final Logger logger = LoggerFactory.getLogger(DatasourceOfferActivityService.class);

	private static final JsonMapper JSON = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	/** JSON file holding the last offer dates. */
	private final Path stateFile;

	/** Last offer date (epoch ms) per datasource name. */
	private final Map<String, Long> lastOfferSeen = new ConcurrentHashMap<>();

	/** Set when a date moved since the last write. */
	private final AtomicBoolean dirty = new AtomicBoolean(false);

	/** Epoch ms since which dates are recorded. */
	private final long trackedSince;

	/**
	 * Creates the service and reads back the state file. A missing or unreadable file starts an empty state tracked
	 * from now.
	 *
	 * @param stateFile JSON state file, its folder is created when missing
	 * @param clock     clock giving the start of tracking when there is no state file
	 */
	public DatasourceOfferActivityService(Path stateFile, Clock clock) {
		this.stateFile = Objects.requireNonNull(stateFile, "stateFile");
		DatasourceOfferActivityReport persisted = readPersisted();
		if (persisted == null) {
			// Written with the first recorded offer: an api that indexes nothing leaves no file
			this.trackedSince = clock.millis();
		} else {
			this.trackedSince = persisted.trackedSince();
			persisted.datasources().forEach(a -> record(a.datasourceName(), a.lastOfferSeen()));
			dirty.set(false);
		}
	}

	/**
	 * Records an offer of a datasource. Dates only move forward; a blank name or a missing date is ignored.
	 *
	 * @param datasourceName datasource of the offer
	 * @param offerTimestamp offer timestamp (epoch ms), i.e. the fragment indexation date
	 */
	public void record(String datasourceName, Long offerTimestamp) {
		if (StringUtils.isBlank(datasourceName) || offerTimestamp == null) {
			return;
		}
		Long previous = lastOfferSeen.get(datasourceName);
		if (previous != null && previous >= offerTimestamp) {
			// Fast path: the datafragment workers call this for every offer
			return;
		}
		lastOfferSeen.merge(datasourceName, offerTimestamp, Math::max);
		dirty.set(true);
	}

	/**
	 * @return the last offer date of every datasource seen since {@code trackedSince}, sorted by name
	 */
	public DatasourceOfferActivityReport report() {
		List<DatasourceOfferActivity> datasources = lastOfferSeen.entrySet().stream()
				.map(e -> new DatasourceOfferActivity(e.getKey(), e.getValue()))
				.sorted(Comparator.comparing(DatasourceOfferActivity::datasourceName))
				.toList();
		return new DatasourceOfferActivityReport(trackedSince, datasources);
	}

	/**
	 * Writes the state file when a date moved since the last write.
	 */
	@Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT5M")
	public void flush() {
		if (dirty.getAndSet(false)) {
			persist(report());
		}
	}

	/**
	 * Writes the state at shutdown, so a restart loses nothing.
	 */
	@PreDestroy
	public void destroy() {
		flush();
	}

	/**
	 * Writes the state through a temporary file renamed over the state file. A failure is logged and the state is
	 * written again at the next flush.
	 */
	private synchronized void persist(DatasourceOfferActivityReport snapshot) {
		Path tmp = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
		try {
			Path folder = stateFile.toAbsolutePath().getParent();
			if (folder != null) {
				Files.createDirectories(folder);
			}
			Files.writeString(tmp, JSON.writeValueAsString(snapshot), StandardCharsets.UTF_8);
			Files.move(tmp, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			logger.info("Wrote the last offer date of {} datasources to {}", snapshot.datasources().size(), stateFile);
		} catch (IOException | JacksonException e) {
			dirty.set(true);
			logger.error("Cannot write the datasource offer activity to {}", stateFile, e);
		}
	}

	/**
	 * Reads back the state file.
	 *
	 * @return the persisted state, or null when the file is missing or unreadable
	 */
	private DatasourceOfferActivityReport readPersisted() {
		try {
			DatasourceOfferActivityReport persisted = JSON.readValue(Files.readString(stateFile, StandardCharsets.UTF_8),
					DatasourceOfferActivityReport.class);
			if (persisted != null) {
				logger.info("Read back the last offer date of {} datasources from {}, tracked since {}",
						persisted.datasources().size(), stateFile, persisted.trackedSince());
			}
			return persisted;
		} catch (NoSuchFileException e) {
			logger.info("No datasource offer activity file at {}, tracking starts now", stateFile);
			return null;
		} catch (IOException | JacksonException e) {
			logger.warn("Cannot read the datasource offer activity from {}, tracking starts now", stateFile, e);
			return null;
		}
	}
}
