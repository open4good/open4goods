package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.ResourceCompletionConfig;
import org.open4goods.api.config.yml.ResourceCompletionUrlTemplate;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceStatus;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.resource.ResourceType;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.imageprocessing.service.ImageMagickService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import ch.qos.logback.classic.Level;

class ResourceCompletionServiceTest {

    @TempDir
    private Path cacheFolder;

    private ResourceCompletionConfig config;
    private ResourceService resourceService;
    private ImageMagickService imageService;
    private ResourceCompletionService service;
    private VerticalConfig vertical;

    @BeforeEach
    void setUp() {
        config = new ResourceCompletionConfig();
        resourceService = new ResourceService(cacheFolder.toString());
        imageService = mock(ImageMagickService.class);

        ApiProperties apiProperties = mock(ApiProperties.class);
        when(apiProperties.getResourceCompletionConfig()).thenReturn(config);
        when(apiProperties.logsFolder()).thenReturn("target/test-logs");
        when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);

        service = new ResourceCompletionService(imageService, mock(VerticalsConfigService.class),
                resourceService, mock(ProductRepository.class), apiProperties, null);
        vertical = new VerticalConfig();
    }

    @Test
    void defaultConfigExposesLegacyTuningValues() {
        ResourceCompletionConfig defaults = new ResourceCompletionConfig();

        assertThat(defaults.getUrlTemplates()).isEmpty();
        assertThat(defaults.isForceEraseFileName()).isFalse();
        assertThat(defaults.getConnectTimeoutMs()).isEqualTo(1_000);
        assertThat(defaults.getSocketTimeoutMs()).isEqualTo(1_000);
        assertThat(defaults.getEmbeddingSimilarityThreshold()).isEqualTo(0.80);
        assertThat(defaults.getPerceptiveHashSize()).isEqualTo(32);
        assertThat(defaults.getPerceptiveHashAlphaThreshold()).isEqualTo(243);
        assertThat(defaults.getPdfLanguageMinConfidence()).isEqualTo(0.5);
        assertThat(defaults.getPdfLanguageMaxPages()).isEqualTo(5);
        assertThat(defaults.getPdfLanguageMaxChars()).isEqualTo(20_000);
        assertThat(defaults.getPdfTitleMaxLines()).isEqualTo(10);
        assertThat(defaults.getPdfTitleFontSizeTolerance()).isEqualTo(0.8f);
    }

    @Test
    void shouldProcessWhenConfiguredTemplateResourceIsMissing() {
        config.setUrlTemplates(List.of(new ResourceCompletionUrlTemplate(
                "https://example.test/{GTIN}.jpg", "template", "fr", List.of(ResourceTag.PRIMARY))));

        Product product = new Product(123L);

        assertThat(service.shouldProcess(vertical, product)).isTrue();
    }

    @Test
    void templateResourceIsMergedWithMetadataAndProcessedFromCache() throws Exception {
        String url = "https://example.test/123.jpg";
        config.setUrlTemplates(List.of(new ResourceCompletionUrlTemplate(
                "https://example.test/{GTIN}.jpg", "template", "fr", List.of(ResourceTag.PRIMARY))));
        writePngToCache(url, 100, 100);
        when(imageService.buildImageInfo(any(File.class))).thenReturn(imageInfo(100, 100, new float[] { 1.0f, 0.0f }));

        Product product = new Product(123L);
        service.processProduct(vertical, product);

        assertThat(product.getResources()).hasSize(1);
        Resource resource = product.getResources().iterator().next();
        assertThat(resource.getUrl()).isEqualTo(url);
        assertThat(resource.getDatasourceName()).isEqualTo("template");
        assertThat(resource.getHardTags()).contains(ResourceTag.PRIMARY);
        assertThat(resource.isProcessed()).isTrue();
        assertThat(resource.getResourceType()).isEqualTo(ResourceType.IMAGE);
        assertThat(product.getCoverImagePath()).startsWith("/images/");
    }

    @Test
    void nullMd5ResourcesAreNotMarkedAsDuplicates() throws Exception {
        Product product = new Product(123L);
        product.getResources().add(processedImage("https://example.test/a.jpg", null, 100, 100));
        product.getResources().add(processedImage("https://example.test/b.jpg", null, 100, 100));

        service.processProduct(vertical, product);

        assertThat(product.getResources()).hasSize(2);
        assertThat(product.getResources()).allMatch(resource -> !resource.isEvicted());
    }

    @Test
    void duplicateAndBlacklistedMd5ResourcesAreRemoved() throws Exception {
        vertical.getResourcesConfig().getMd5Exclusions().add("blocked");
        Product product = new Product(123L);
        product.getResources().add(processedImage("https://example.test/blocked.jpg", "blocked", 100, 100));
        product.getResources().add(processedImage("https://example.test/one.jpg", "same", 100, 100));
        product.getResources().add(processedImage("https://example.test/two.jpg", "same", 100, 100));

        service.processProduct(vertical, product);

        assertThat(product.getResources()).hasSize(1);
        assertThat(product.getResources().iterator().next().getUrl()).isEqualTo("https://example.test/one.jpg");
    }

    @Test
    void smallImagesAreRemoved() throws Exception {
        vertical.getResourcesConfig().setMinPixelsEvictionSize(2_000);
        Product product = new Product(123L);
        product.getResources().add(processedImage("https://example.test/small.jpg", "small", 20, 20));
        product.getResources().add(processedImage("https://example.test/large.jpg", "large", 100, 100));

        service.processProduct(vertical, product);

        assertThat(product.getResources())
                .singleElement()
                .extracting(Resource::getUrl)
                .isEqualTo("https://example.test/large.jpg");
    }

    @Test
    void filenameFallbackUsesLastExtensionOnly() throws Exception {
        Product product = new Product(123L);
        Resource resource = processedImage("https://example.test/folder/manual.v2.large.jpg?token=abc", "md5", 100, 100);
        resource.setFileName(null);
        product.getResources().add(resource);

        service.processProduct(vertical, product);

        assertThat(resource.getFileName()).isEqualTo("manual-v2-large");
    }

    @Test
    void unsupportedMimeTypeIsEvicted() throws Exception {
        String url = "https://example.test/readme.txt";
        Resource resource = new Resource(url);
        writeTextToCache(url, "plain text");

        Resource processed = service.fetchResource(resource, vertical);

        assertThat(processed.isEvicted()).isTrue();
        assertThat(processed.getStatus()).isEqualTo(ResourceStatus.UNSUPPORTED_MIME_TYPE);
        assertThat(processed.getResourceType()).isEqualTo(ResourceType.UNKNOWN);
    }

    @Test
    void emptyCachedFileIsEvictedBeforeMimeDetection() throws Exception {
        String url = "https://example.test/empty.jpg";
        Resource resource = new Resource(url);
        File cacheFile = cacheFileFor(url);
        assertThat(cacheFile.createNewFile()).isTrue();

        Resource processed = service.fetchResource(resource, vertical);

        assertThat(processed.isEvicted()).isTrue();
        assertThat(processed.getStatus()).isEqualTo(ResourceStatus.EMPTY_FILE);
    }

    @Test
    void imagesWithoutEmbeddingsStayAsSingletonGroups() throws Exception {
        Product product = new Product(123L);
        product.getResources().add(processedImage("https://example.test/a.jpg", "a", 100, 100));
        product.getResources().add(processedImage("https://example.test/b.jpg", "b", 120, 120));

        service.processProduct(vertical, product);

        assertThat(product.getResources()).hasSize(2);
        assertThat(product.getResources()).extracting(Resource::getGroup).containsExactlyInAnyOrder(0, 1);
        assertThat(product.getCoverImagePath()).startsWith("/images/");
    }

    private Resource processedImage(String url, String md5, int width, int height) throws Exception {
        Resource resource = new Resource(url);
        resource.setProcessed(true);
        resource.setCacheKey(IdHelper.generateResourceId(url));
        resource.setFileName("image");
        resource.setExtension("jpg");
        resource.setMd5(md5);
        resource.setResourceType(ResourceType.IMAGE);
        resource.setImageInfo(imageInfo(width, height, null));
        return resource;
    }

    private ImageInfo imageInfo(int width, int height, float[] embedding) {
        ImageInfo imageInfo = new ImageInfo();
        imageInfo.setWidth(width);
        imageInfo.setHeight(height);
        imageInfo.setEmbedding(embedding);
        return imageInfo;
    }

    private void writePngToCache(String url, int width, int height) throws Exception {
        File cacheFile = cacheFileFor(url);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().setColor(Color.WHITE);
        image.getGraphics().fillRect(0, 0, width, height);
        ImageIO.write(image, "png", cacheFile);
    }

    private void writeTextToCache(String url, String text) throws Exception {
        java.nio.file.Files.writeString(cacheFileFor(url).toPath(), text);
    }

    private File cacheFileFor(String url) throws Exception {
        Resource resource = new Resource(url);
        resource.setCacheKey(IdHelper.generateResourceId(url));
        File cacheFile = resourceService.getCacheFile(resource);
        assertThat(cacheFile.getParentFile().exists()).isTrue();
        return cacheFile;
    }
}
