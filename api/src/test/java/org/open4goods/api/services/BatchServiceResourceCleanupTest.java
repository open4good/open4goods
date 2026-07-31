package org.open4goods.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceType;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;

public class BatchServiceResourceCleanupTest {

    private Path tempCacheDir;
    private Path tempDeletionDir;

    private ResourceService resourceService;
    private ApiProperties apiProperties;
    private ProductRepository productRepository;

    private BatchService batchService;

    @BeforeEach
    public void setUp() throws IOException {
        tempCacheDir = Files.createTempDirectory("gc-cache-test");
        tempDeletionDir = Files.createTempDirectory("gc-deletion-test");

        resourceService = mock(ResourceService.class);
        // Ensure paths end with File.separator for consistency
        when(resourceService.getRemoteCachingFolder()).thenReturn(tempCacheDir.toAbsolutePath().toString() + File.separator);

        apiProperties = mock(ApiProperties.class);
        when(apiProperties.remoteCachingDeletionFolder()).thenReturn(tempDeletionDir.toAbsolutePath().toString() + File.separator);
        when(apiProperties.getResourceCleanupGracePeriodMs()).thenReturn(5000L); // 5 seconds grace period
        when(apiProperties.getAllowedImagesSizeSuffixes()).thenReturn(List.of(30, 50));
        when(apiProperties.getResourceCleanupMinProductRatio()).thenReturn(0.9d);
        when(apiProperties.getResourceDeletionFolderRetentionMs()).thenReturn(5000L);

        productRepository = mock(ProductRepository.class);
        when(productRepository.countMainIndex()).thenReturn(1L);

        AggregationFacadeService aggregationFacadeService = mock(AggregationFacadeService.class);
        CompletionFacadeService completionFacadeService = mock(CompletionFacadeService.class);
        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        FeedIndexingService feedIndexingService = mock(FeedIndexingService.class);
        FeedService feedService = mock(FeedService.class);
        SerialisationService serialisationService = mock(SerialisationService.class);

        batchService = new BatchService(
                aggregationFacadeService,
                completionFacadeService,
                verticalsConfigService,
                productRepository,
                feedIndexingService,
                feedService,
                serialisationService,
                resourceService,
                apiProperties
        );
    }

    @AfterEach
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempCacheDir.toFile());
        FileUtils.deleteDirectory(tempDeletionDir.toFile());
    }

    @Test
    public void testCleanOrphanResources() throws Exception {
        // Setup mock product with resource
        Product product = new Product();
        product.setId(999L);

        Resource activeResource = new Resource("http://example.com/image.jpg");
        activeResource.setResourceType(ResourceType.IMAGE);
        activeResource.setFileName("image");
        activeResource.setCacheKey("hash123");
        product.setResources(new HashSet<>(Collections.singletonList(activeResource)));

        when(productRepository.exportAll()).thenReturn(Stream.of(product));

        // Create cache files
        // 1. Active resource original file (should NOT be moved)
        // File name matches the cacheKey: "hash123" (since ResourceService.getCacheFile uses r.getCacheKey())
        String originalFolderPrefix = activeResource.folderHashPrefix();
        Path activeOriginalFile = createFileInCache(originalFolderPrefix + "/hash123", "active content", System.currentTimeMillis() - 10000);

        // 2. Active resource resized file (should NOT be moved)
        // For resizing, path() is "/images/image_hash123-30.webp"
        // The generated cache key for it is: IdHelper.generateResourceId("/images/image_hash123-30.webp") + ".cache.webp"
        String resizedPath = activeResource.path(30);
        String resizedCacheKey = IdHelper.generateResourceId(resizedPath) + ".cache.webp";
        String resizedFolderPrefix = Resource.folderHashPrefix(resizedCacheKey);
        Path activeResizedFile = createFileInCache(resizedFolderPrefix + "/" + resizedCacheKey, "active resized content", System.currentTimeMillis() - 10000);

        // 3. Stale orphan file - older than grace period (SHOULD be moved)
        String orphanStaleKey = "orphan_stale_key";
        String orphanStalePrefix = Resource.folderHashPrefix(orphanStaleKey);
        Path staleOrphanFile = createFileInCache(orphanStalePrefix + "/" + orphanStaleKey, "orphan stale content", System.currentTimeMillis() - 10000);

        // 4. Young orphan file - modified within grace period (should NOT be moved)
        String orphanYoungKey = "orphan_young_key";
        String orphanYoungPrefix = Resource.folderHashPrefix(orphanYoungKey);
        Path youngOrphanFile = createFileInCache(orphanYoungPrefix + "/" + orphanYoungKey, "orphan young content", System.currentTimeMillis() - 1000);

        // Execute cleanup
        batchService.cleanOrphanResources(ResourceCleanupMode.MOVE);

        // Verify file states
        // 1. Active original file must still be in cache and NOT in deletion dir
        assertTrue(Files.exists(activeOriginalFile), "Active original file should still exist in cache");
        assertFalse(Files.exists(tempDeletionDir.resolve(originalFolderPrefix + "/hash123")), "Active original file should not be in deletion folder");

        // 2. Active resized file must still be in cache and NOT in deletion dir
        assertTrue(Files.exists(activeResizedFile), "Active resized file should still exist in cache");
        assertFalse(Files.exists(tempDeletionDir.resolve(resizedFolderPrefix + "/" + resizedCacheKey)), "Active resized file should not be in deletion folder");

        // 3. Stale orphan file must NOT exist in cache and MUST exist in deletion dir
        assertFalse(Files.exists(staleOrphanFile), "Stale orphan file should have been removed from cache");
        assertTrue(Files.exists(tempDeletionDir.resolve(orphanStalePrefix + "/" + orphanStaleKey)), "Stale orphan file should have been moved to deletion folder");
        assertEquals("orphan stale content", Files.readString(tempDeletionDir.resolve(orphanStalePrefix + "/" + orphanStaleKey)), "Stale orphan contents must be preserved");

        // 4. Young orphan file must still exist in cache (skipped due to grace period) and NOT in deletion dir
        assertTrue(Files.exists(youngOrphanFile), "Young orphan file should be skipped and still exist in cache");
        assertFalse(Files.exists(tempDeletionDir.resolve(orphanYoungPrefix + "/" + orphanYoungKey)), "Young orphan file should not be in deletion folder");
    }

    /**
     * A stream coming back short of the index count means the enumeration is incomplete, not that
     * the cache is orphaned. Nothing may be touched in that case.
     */
    @Test
    public void testTruncatedProductStreamAbortsWithoutTouchingAnyFile() throws Exception {
        // The index claims 100 products, but the stream only yields one
        when(productRepository.countMainIndex()).thenReturn(100L);
        when(productRepository.exportAll()).thenReturn(Stream.of(productWithResource()));

        String orphanKey = "orphan_stale_key";
        Path orphan = createFileInCache(Resource.folderHashPrefix(orphanKey) + "/" + orphanKey, "content",
                System.currentTimeMillis() - 10000);

        ResourceCleanupReport report = batchService.cleanOrphanResources(ResourceCleanupMode.DELETE);

        assertTrue(report.aborted(), "Run must abort when the product stream is truncated");
        assertEquals(0, report.processedCount(), "No file may be processed on an aborted run");
        assertTrue(Files.exists(orphan), "Orphan must survive an aborted run");
    }

    /**
     * An empty product index is never a licence to evict the cache.
     */
    @Test
    public void testEmptyProductIndexAborts() throws Exception {
        when(productRepository.countMainIndex()).thenReturn(0L);

        String orphanKey = "orphan_stale_key";
        Path orphan = createFileInCache(Resource.folderHashPrefix(orphanKey) + "/" + orphanKey, "content",
                System.currentTimeMillis() - 10000);

        ResourceCleanupReport report = batchService.cleanOrphanResources(ResourceCleanupMode.DELETE);

        assertTrue(report.aborted(), "Run must abort on an empty product index");
        assertTrue(Files.exists(orphan), "Orphan must survive an aborted run");
    }

    /**
     * The cache root is shared with unrelated caches. Only the three level hash shard hierarchy
     * holds product resources; everything else must be left strictly alone.
     */
    @Test
    public void testForeignCachesOutsideShardsAreNeverSwept() throws Exception {
        when(productRepository.exportAll()).thenReturn(Stream.of(productWithResource()));

        long stale = System.currentTimeMillis() - 10000;
        // Flat files written at the cache root by RemoteFileCachingService and IcecatFileDownloadService
        Path remoteFile = createFileInCache("1234567890123456789", "remote file cache", stale);
        // Named sub folders owned by other services
        Path geocode = createFileInCache("geocode/GeoLite2-City.mmdb", "geoip database", stale);
        Path aiBatch = createFileInCache("batch-ia/batch-42.jsonl", "in flight ai batch", stale);
        // A shard shaped folder, but a file sitting at the wrong depth
        Path shallow = createFileInCache("A/B/stray-file", "not at shard depth", stale);

        ResourceCleanupReport report = batchService.cleanOrphanResources(ResourceCleanupMode.DELETE);

        assertFalse(report.aborted(), "Run should proceed");
        assertTrue(Files.exists(remoteFile), "Remote file cache entry must not be swept");
        assertTrue(Files.exists(geocode), "Geocode database must not be swept");
        assertTrue(Files.exists(aiBatch), "In flight AI batch must not be swept");
        assertTrue(Files.exists(shallow), "File outside the shard depth must not be swept");
        assertEquals(0, report.orphanCount(), "No foreign cache file may be classified as orphan");
    }

    /**
     * DRY_RUN must account for the reclaimable volume without touching anything.
     */
    @Test
    public void testDryRunReportsOrphansWithoutTouchingThem() throws Exception {
        when(productRepository.exportAll()).thenReturn(Stream.of(productWithResource()));

        String orphanKey = "orphan_stale_key";
        Path orphan = createFileInCache(Resource.folderHashPrefix(orphanKey) + "/" + orphanKey, "0123456789",
                System.currentTimeMillis() - 10000);

        ResourceCleanupReport report = batchService.cleanOrphanResources(ResourceCleanupMode.DRY_RUN);

        assertFalse(report.aborted());
        assertEquals(1, report.orphanCount(), "Orphan should be counted");
        assertEquals(10, report.orphanBytes(), "Reclaimable volume should be measured");
        assertEquals(0, report.processedCount(), "DRY_RUN must not process anything");
        assertTrue(Files.exists(orphan), "DRY_RUN must leave the orphan in place");
    }

    /**
     * DELETE reclaims the space in place, where MOVE only parks the file on the same filesystem.
     */
    @Test
    public void testDeleteModeRemovesOrphanInPlace() throws Exception {
        when(productRepository.exportAll()).thenReturn(Stream.of(productWithResource()));

        String orphanKey = "orphan_stale_key";
        String relative = Resource.folderHashPrefix(orphanKey) + "/" + orphanKey;
        Path orphan = createFileInCache(relative, "content", System.currentTimeMillis() - 10000);

        ResourceCleanupReport report = batchService.cleanOrphanResources(ResourceCleanupMode.DELETE);

        assertEquals(1, report.processedCount());
        assertFalse(Files.exists(orphan), "Orphan should be deleted in place");
        assertFalse(Files.exists(tempDeletionDir.resolve(relative)), "DELETE must not park the file anywhere");
    }

    /**
     * Parking files is what the previous implementation called a cleanup; the purge is what
     * actually gives the disk back.
     */
    @Test
    public void testPurgeDeletionFolderReclaimsSpaceBeyondRetention() throws Exception {
        Path parkedStale = tempDeletionDir.resolve("K/E/Y/orphan_stale_key");
        Files.createDirectories(parkedStale.getParent());
        Files.writeString(parkedStale, "0123456789", StandardCharsets.UTF_8);
        assertTrue(parkedStale.toFile().setLastModified(System.currentTimeMillis() - 10000));

        Path parkedRecent = tempDeletionDir.resolve("K/E/Y/orphan_recent_key");
        Files.writeString(parkedRecent, "still inspectable", StandardCharsets.UTF_8);
        assertTrue(parkedRecent.toFile().setLastModified(System.currentTimeMillis() - 1000));

        long reclaimed = batchService.purgeDeletionFolder();

        assertEquals(10, reclaimed, "Only the file beyond retention should be reclaimed");
        assertFalse(Files.exists(parkedStale), "File beyond retention should be purged");
        assertTrue(Files.exists(parkedRecent), "File within retention should stay inspectable");
    }

    private Product productWithResource() throws Exception {
        Product product = new Product();
        product.setId(999L);

        Resource activeResource = new Resource("http://example.com/image.jpg");
        activeResource.setResourceType(ResourceType.IMAGE);
        activeResource.setFileName("image");
        activeResource.setCacheKey("hash123");
        product.setResources(new HashSet<>(Collections.singletonList(activeResource)));
        return product;
    }

    private Path createFileInCache(String relativePath, String content, long lastModifiedTime) throws IOException {
        Path filePath = tempCacheDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        assertTrue(filePath.toFile().setLastModified(lastModifiedTime), "Failed to set mock file modification time");
        return filePath;
    }
}
