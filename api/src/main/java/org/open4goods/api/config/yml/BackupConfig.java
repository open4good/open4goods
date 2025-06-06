package org.open4goods.api.config.yml;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotEmpty;

/**
 * Configuration of backup locations and parameters.
 */
@Validated
public record BackupConfig(
        @NotEmpty String xwikiBackupFile,
        @NotEmpty String dataBackupFolder,
        @NotEmpty String importProductPath,
        int importBulkSize,
        int productImportThreads,
        int productsExportThreads,
        int minXwikiBackupFileSizeInMb,
        int minProductsBackupFolderSizeInMb,
        int maxWikiBackupAgeInHours,
        long maxProductsBackupAgeInHours) {

    /**
     * Default constructor initializing sensible defaults.
     */
    public BackupConfig() {
        this(null, null, null, 100, 4, 4, 30, 16000, 14, 1000L * 3600 * 24 * 8);
    }

    /**
     * Canonical constructor applying defaults for unset values.
     */
    public BackupConfig {
        importBulkSize = importBulkSize == 0 ? 100 : importBulkSize;
        productImportThreads = productImportThreads == 0 ? 4 : productImportThreads;
        productsExportThreads = productsExportThreads == 0 ? 4 : productsExportThreads;
        minXwikiBackupFileSizeInMb = minXwikiBackupFileSizeInMb == 0 ? 30 : minXwikiBackupFileSizeInMb;
        minProductsBackupFolderSizeInMb = minProductsBackupFolderSizeInMb == 0 ? 16000 : minProductsBackupFolderSizeInMb;
        maxWikiBackupAgeInHours = maxWikiBackupAgeInHours == 0 ? 14 : maxWikiBackupAgeInHours;
        maxProductsBackupAgeInHours = maxProductsBackupAgeInHours == 0 ? 1000L * 3600 * 24 * 8 : maxProductsBackupAgeInHours;
    }

    // Compatibility accessors -------------------------------------------------
    public String getXwikiBackupFile() { return xwikiBackupFile; }
    public String getDataBackupFolder() { return dataBackupFolder; }
    public String getImportProductPath() { return importProductPath; }
    public int getImportBulkSize() { return importBulkSize; }
    public int getProductImportThreads() { return productImportThreads; }
    public int getProductsExportThreads() { return productsExportThreads; }
    public int getMinXwikiBackupFileSizeInMb() { return minXwikiBackupFileSizeInMb; }
    public int getMinProductsBackupFolderSizeInMb() { return minProductsBackupFolderSizeInMb; }
    public int getMaxWikiBackupAgeInHours() { return maxWikiBackupAgeInHours; }
    public long getMaxProductsBackupAgeInHours() { return maxProductsBackupAgeInHours; }
}
