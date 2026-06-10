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
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
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

        productRepository = mock(ProductRepository.class);

        AggregationFacadeService aggregationFacadeService = mock(AggregationFacadeService.class);
        CompletionFacadeService completionFacadeService = mock(CompletionFacadeService.class);
        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        CsvDatasourceFetchingService csvDatasourceFetchingService = mock(CsvDatasourceFetchingService.class);
        FeedService feedService = mock(FeedService.class);
        SerialisationService serialisationService = mock(SerialisationService.class);

        batchService = new BatchService(
                aggregationFacadeService,
                completionFacadeService,
                verticalsConfigService,
                productRepository,
                csvDatasourceFetchingService,
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
        batchService.cleanOrphanResources();

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

    private Path createFileInCache(String relativePath, String content, long lastModifiedTime) throws IOException {
        Path filePath = tempCacheDir.resolve(relativePath);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        assertTrue(filePath.toFile().setLastModified(lastModifiedTime), "Failed to set mock file modification time");
        return filePath;
    }
}
