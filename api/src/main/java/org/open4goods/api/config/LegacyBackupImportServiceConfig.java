package org.open4goods.api.config;

import org.open4goods.api.config.yml.LegacyBackupImportConfig;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupCancellationRegistry;
import org.open4goods.api.services.migration.legacybackup.LegacyBackupImportService;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.pricehistory.port.LegacyPriceBackfillStore;
import org.open4goods.pricehistory.service.LegacyPriceBackfillService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Wires the resumable legacy backup importer for profiles that have Elasticsearch-backed source,
 * checkpoint and legacy price stores available.
 *
 * <p>Deliberately not {@code @Profile("local")}-restricted beyond the other backing stores: this
 * bean only builds the service, it never triggers an import, so making it available wherever
 * those stores exist does not conflict with "no import starts on application boot" (AC1).
 */
@Configuration
@Profile({ "beta", "dev", "devsec", "prod" })
public class LegacyBackupImportServiceConfig {

    @Bean
    LegacyBackupCancellationRegistry legacyBackupCancellationRegistry() {
        return new LegacyBackupCancellationRegistry();
    }

    @Bean
    LegacyPriceBackfillService legacyPriceBackfillService(LegacyPriceBackfillStore legacyPriceBackfillStore) {
        return new LegacyPriceBackfillService(legacyPriceBackfillStore);
    }

    @Bean
    LegacyBackupImportService legacyBackupImportService(
            LegacyBackupImportConfig legacyBackupImportConfig,
            SourceRecordHeadStore sourceRecordHeadStore,
            LegacyPriceBackfillService legacyPriceBackfillService,
            IngestionCheckpointStore ingestionCheckpointStore,
            LegacyBackupCancellationRegistry legacyBackupCancellationRegistry) {
        return new LegacyBackupImportService(legacyBackupImportConfig, sourceRecordHeadStore, legacyPriceBackfillService,
                ingestionCheckpointStore, legacyBackupCancellationRegistry);
    }
}
