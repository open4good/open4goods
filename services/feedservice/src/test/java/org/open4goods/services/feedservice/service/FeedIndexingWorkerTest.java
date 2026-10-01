package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.LoggerFactory;

/**
 * Covers AC2 for FeedIndexingWorker: unknown headers are reported, never guessed from a label,
 * and a feed whose config declares neither url nor price is skipped rather than indexed blindly.
 */
class FeedIndexingWorkerTest {

    @TempDir
    File tempDir;

    @Test
    void unmappedHeaderIsReportedAsUnknownAndNeverFeedsAField() throws Exception {
        FeedIndexingService csvService = buildCsvService();
        DataFragmentIndexer indexer = mock(DataFragmentIndexer.class);
        RemoteFileCachingService remoteFileCachingService = mock(RemoteFileCachingService.class);

        File csvFile = new File(tempDir, "feed.csv");
        Files.writeString(csvFile.toPath(),
                "product_url,price,name,mystery_column\nhttp://shop.test/p1,12.50,Widget,???\n");
        when(remoteFileCachingService.downloadToTmpFile(any(), any())).thenReturn(csvFile);

        FeedIndexingWorker worker = new FeedIndexingWorker(csvService, mock(DataFragmentCompletionService.class),
                indexer, 1000, tempDir.getAbsolutePath(), remoteFileCachingService);

        worker.fetch(datasource("Acme Merchant", "acme-merchant", "product_url", Set.of("price"), "name",
                "http://example.test/feed.csv"));

        assertThat(worker.stats().getUnknownColumns()).containsExactly("mystery_column");
        verify(indexer).index(any(DataFragment.class), org.mockito.ArgumentMatchers.eq("acme-merchant"));
    }

    @Test
    void feedWithoutUrlOrPriceColumnIsSkippedRatherThanGuessed() throws Exception {
        FeedIndexingService csvService = buildCsvService();
        DataFragmentIndexer indexer = mock(DataFragmentIndexer.class);
        RemoteFileCachingService remoteFileCachingService = mock(RemoteFileCachingService.class);

        FeedIndexingWorker worker = new FeedIndexingWorker(csvService, mock(DataFragmentCompletionService.class),
                indexer, 1000, tempDir.getAbsolutePath(), remoteFileCachingService);

        worker.fetch(datasource("Unmapped Merchant", "unmapped-merchant", null, Set.of(), null,
                "http://example.test/feed2.csv"));

        verifyNoInteractions(remoteFileCachingService);
        verify(indexer, never()).index(any(), any());
        verify(csvService).incrementFeedMissingRequiredColumns();
    }

    private FeedIndexingService buildCsvService() throws Exception {
        FeedIndexingService csvService = mock(FeedIndexingService.class);
        when(csvService.createDatasourceLogger(any(), any(), any())).thenReturn(LoggerFactory.getLogger("test"));
        when(csvService.detectSchema(any(File.class), any(Charset.class)))
                .thenAnswer(inv -> new CsvDialectDetector().detectSchema(inv.getArgument(0), inv.getArgument(1)));
        return csvService;
    }

    private DataSourceProperties datasource(String feedKey, String datasourceConfigName, String urlColumn,
            Set<String> priceColumns, String nameColumn, String feedUrl) {
        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setUrl(urlColumn);
        csv.setPrice(priceColumns);
        csv.setName(nameColumn);
        csv.setImportAllAttributes(false);
        csv.getDatasourceUrls().add(feedUrl);

        DataSourceProperties ds = new DataSourceProperties();
        ds.setCsvDatasource(csv);
        ds.setFeedKey(feedKey);
        ds.setDatasourceConfigName(datasourceConfigName);
        ds.setName(datasourceConfigName);
        ds.setLanguage("FR");
        return ds;
    }
}
