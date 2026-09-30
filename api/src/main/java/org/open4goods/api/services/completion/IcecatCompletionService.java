package org.open4goods.api.services.completion;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.datareference.model.IngestionCheckpoint;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.ScanCursor;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.services.IcecatLiveClient;
import org.open4goods.icecat.services.IcecatLiveLookupResult;
import org.open4goods.icecat.services.IcecatSourceRecordAdapter;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Icecat product completion, driven from the live API and expressed as source-record heads.
 *
 * <p>This service is the Icecat live-completion boundary: it never mutates a legacy
 * {@code Product} or {@code DataFragment} and never invokes the legacy aggregation pipeline.
 * Icecat's own transport, retries and localization stay owned by {@code services/icecat};
 * {@link IcecatSourceRecordAdapter} is the sole translation of their neutral result into the
 * shared source-record contract, and {@link SourceRecordHeadStore} is the sole persistence
 * boundary.
 */
public class IcecatCompletionService extends AbstractCompletionService {

	/**
	 * Suffix appended to {@link #getDatasourceName()} to record the negative-cache timestamp
	 * (NOT_FOUND / RESTRICTED / ERROR outcomes), kept distinct from the success timestamp so
	 * that {@link #shouldProcess(VerticalConfig, Product)} can gate each independently.
	 */
	private static final String MISS_SUFFIX = ".miss";
	/** O4G contract version this service writes heads and field identities against. */
	private static final String SCHEMA_VERSION = "icecat-live-v1";
	/** Bounded owner identity for the last-processed-product checkpoint. */
	private static final String CHECKPOINT_OWNER = "icecat-live-v1";

    private final IcecatCompletionConfig icecatConfig;
    private final IcecatLiveClient liveClient;
    private final IcecatSourceRecordAdapter adapter;
    private final SourceRecordHeadStore sourceRecordStore;
    private final IngestionCheckpointStore checkpointStore;

	public IcecatCompletionService(ProductRepository dataRepository, VerticalsConfigService verticalConfigService,
			ApiProperties apiProperties, DataSourceConfigService dataSourceConfigService,
			IcecatSourceRecordAdapter adapter, SourceRecordHeadStore sourceRecordStore,
			IngestionCheckpointStore checkpointStore)  {
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());

		this.icecatConfig = apiProperties.getIcecatCompletionConfig();
		this.liveClient = new IcecatLiveClient(icecatConfig);
		this.adapter = adapter;
		this.sourceRecordStore = sourceRecordStore;
		this.checkpointStore = checkpointStore;
	}

        @Override
        public boolean shouldProcess(VerticalConfig vertical, Product data) {
                // TODO(p2,perf) : should adda check on unprocessed resources
                long now = System.currentTimeMillis();

                Long lastSuccess = data.getDatasourceCodes().get(getDatasourceName());
                if (null != lastSuccess) {
                        long refreshIntervalMs = Duration.ofDays(icecatConfig.getRefreshIntervalDays()).toMillis();
                        if (now - lastSuccess < refreshIntervalMs) {
                                return false;
                        }
                }

                Long lastMiss = data.getDatasourceCodes().get(missDatasourceName());
                if (null != lastMiss) {
                        long negativeCacheMs = Duration.ofDays(icecatConfig.getNegativeCacheIntervalDays()).toMillis();
                        if (now - lastMiss < negativeCacheMs) {
                                return false;
                        }
                }

                return true;
        }

	@Override
	public String getDatasourceName() {
		return "icecat.biz";
	}

	private String missDatasourceName() {
		return getDatasourceName() + MISS_SUFFIX;
	}

	/**
	 * Trigger icecat call on a product. Here the logic :
	 * > If no Icecat id is known yet, search by GTIN and associate the id on a match.
	 * > If an Icecat id is already known, refresh that exact product by id.
	 * The success timestamp ({@link #getDatasourceName()}) is set only when the resulting
	 * source-record mutation actually changes or confirms the head; a NOT_FOUND/RESTRICTED/ERROR
	 * outcome instead sets a distinct negative-cache timestamp ({@link #missDatasourceName()}), so
	 * {@link #shouldProcess} can gate retries on misses independently of the refresh interval for
	 * known matches.
	 */
	public void processProduct(VerticalConfig vertical, Product data) {
		logger.info("Icecat completion for {}", data.getId());

		if (null == data.getId()) {
			logger.warn("Skipping icecat completion, product has no id");
			return;
		}

		String knownIcecatId = data.getExternalIds().getIcecat();
		boolean idAlreadyKnown = StringUtils.isNotEmpty(knownIcecatId);
		IcecatLiveLookupResult result = idAlreadyKnown
				? liveClient.fetchProductByIcecatId(knownIcecatId, icecatConfig.getDomainLanguage())
				: liveClient.fetchProduct(data.getId(), icecatConfig.getDomainLanguage());

		Instant retrievedAt = Instant.now();
		LanguageTag language = new LanguageTag(icecatConfig.getDomainLanguage().languageTag());

		boolean succeeded = false;
		switch (result.status()) {
			case FOUND -> {
				IceDataItem iceItem = result.product().orElseThrow();
				String icecatId = String.valueOf(iceItem.generalInfo.icecatId);
				data.getExternalIds().setIcecat(icecatId);
				Optional<SourceRecordMutation> mutation =
						adapter.adapt(iceItem, language, Set.of(language), SCHEMA_VERSION, retrievedAt);
				if (mutation.isPresent()) {
					succeeded = isChangeAccepted(apply(mutation.orElseThrow()));
					if (succeeded) {
						acknowledgeCheckpoint(icecatId, retrievedAt);
					}
				}
			}
			case NOT_FOUND -> withdrawKnownRecord(idAlreadyKnown, knownIcecatId, retrievedAt);
			case RESTRICTED -> rejectKnownRecord(idAlreadyKnown, knownIcecatId, retrievedAt, "RESTRICTED");
			case ERROR -> {
				logger.error("Icecat live lookup failed for gtin {} : {}", data.gtin(),
						result.errorMessage().orElse("unknown error"));
				rejectKnownRecord(idAlreadyKnown, knownIcecatId, retrievedAt, "ERROR");
			}
		}

		if (succeeded) {
			data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());
		} else {
			data.getDatasourceCodes().put(missDatasourceName(), System.currentTimeMillis());
		}

        try {
            Thread.sleep(icecatConfig.getPolitenessDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Icecat politeness sleep interrupted");
        }
    }

	/**
	 * A product no longer served by Icecat is only tombstoned when a prior head could exist
	 * (its id was already known); an unmatched GTIN search never had a head to withdraw.
	 */
	private void withdrawKnownRecord(boolean idAlreadyKnown, String knownIcecatId, Instant retrievedAt) {
		if (!idAlreadyKnown) {
			return;
		}
		apply(adapter.tombstone(recordKey(knownIcecatId), SCHEMA_VERSION, retrievedAt));
	}

	/**
	 * A restricted/errored lookup is only recorded as a terminal attempt when a prior head could
	 * exist; it never fabricates a key for a product Icecat has never matched.
	 */
	private void rejectKnownRecord(boolean idAlreadyKnown, String knownIcecatId, Instant retrievedAt, String sanitizedErrorCode) {
		if (!idAlreadyKnown) {
			return;
		}
		SourceRecordMutation mutation = "RESTRICTED".equals(sanitizedErrorCode)
				? adapter.restricted(recordKey(knownIcecatId), SCHEMA_VERSION, retrievedAt, sanitizedErrorCode)
				: adapter.unavailable(recordKey(knownIcecatId), SCHEMA_VERSION, retrievedAt, sanitizedErrorCode);
		apply(mutation);
	}

	private SourceRecordKey recordKey(String icecatId) {
		return IcecatSourceRecordAdapter.keyFor(Integer.parseInt(icecatId));
	}

	private SourceRecordTransition apply(SourceRecordMutation mutation) {
		return sourceRecordStore.apply(mutation);
	}

	private boolean isChangeAccepted(SourceRecordTransition transition) {
		return transition.outcome() == SourceRecordTransitionOutcome.ACCEPTED
				|| transition.outcome() == SourceRecordTransitionOutcome.DUPLICATE
				|| transition.outcome() == SourceRecordTransitionOutcome.TOMBSTONED;
	}

	/**
	 * Best-effort, diagnostic checkpoint of the last Icecat product processed: it never gates a
	 * correctness decision, so a failure to persist it must not shadow a successful head write.
	 */
	private void acknowledgeCheckpoint(String icecatId, Instant retrievedAt) {
		try {
			SourceId sourceId = new SourceId(IcecatSourceRecordAdapter.SOURCE_ID);
			Optional<IngestionCheckpoint> current = checkpointStore.find(CHECKPOINT_OWNER, sourceId);
			IngestionCheckpoint next = new IngestionCheckpoint(
					CHECKPOINT_OWNER,
					sourceId,
					Optional.of(new ScanCursor(SCHEMA_VERSION + ":" + icecatId)),
					0,
					Optional.empty(),
					retrievedAt,
					current.map(IngestionCheckpoint::revision).orElse(0L) + 1);
			checkpointStore.compareAndSet(next, current.map(IngestionCheckpoint::revision).orElse(0L));
		} catch (RuntimeException exception) {
			logger.warn("Unable to persist Icecat completion checkpoint for {}", icecatId, exception);
		}
	}
}
