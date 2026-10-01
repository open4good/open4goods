package org.open4goods.api.config.yml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/**
 * Configuration for the resumable legacy backup to neutral data importer.
 *
 * <p>{@code datasetRoot} is a read-only mount of the pinned legacy backup archive
 * (see {@code docs/operations/product-backup-input-contract.md}); this importer never
 * writes to it. {@code deadLetterFolder} is a private, non-Git operational location for
 * sanitized parse/digest/quarantine records.
 */
@Configuration
@ConfigurationProperties(prefix = "api.legacybackupimport")
@Validated
public class LegacyBackupImportConfig {

    /** Read-only directory holding the pinned gzip JSONL archive and its manifest. */
    @NotEmpty
    private String datasetRoot;

    /** Private directory for sanitized dead letters, never tracked in Git. */
    @NotEmpty
    private String deadLetterFolder;

    /** Bounded importer identity used as the {@code IngestionCheckpointStore} owner. */
    private String checkpointOwner = "legacy-backup-import-v1";

    /** Number of lines reconciled per checkpoint-advancing batch. */
    @Min(1)
    private int batchSize = 500;

    public String getDatasetRoot() {
        return datasetRoot;
    }

    public void setDatasetRoot(String datasetRoot) {
        this.datasetRoot = datasetRoot;
    }

    public String getDeadLetterFolder() {
        return deadLetterFolder;
    }

    public void setDeadLetterFolder(String deadLetterFolder) {
        this.deadLetterFolder = deadLetterFolder;
    }

    public String getCheckpointOwner() {
        return checkpointOwner;
    }

    public void setCheckpointOwner(String checkpointOwner) {
        this.checkpointOwner = checkpointOwner;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
