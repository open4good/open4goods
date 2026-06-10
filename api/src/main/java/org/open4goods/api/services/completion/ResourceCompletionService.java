package org.open4goods.api.services.completion;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.ResourceCompletionConfig;
import org.open4goods.api.config.yml.ResourceCompletionUrlTemplate;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.embedding.service.image.DjlImageEmbeddingService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.model.resource.PdfInfo;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceStatus;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.resource.ResourceType;
import org.open4goods.model.vertical.ResourcesAggregationConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.imageprocessing.service.ImageMagickService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import dev.brachtendorf.jimagehash.hash.Hash;
import jakarta.annotation.PostConstruct;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

/**
 * Completes product resources by adding configured URLs, downloading missing
 * files, extracting metadata, filtering invalid resources, clustering images and
 * choosing a product cover.
 */
public class ResourceCompletionService extends AbstractCompletionService
{

    /** Default cover path when no usable image remains. */
    private static final String NO_IMAGE_PATH = "/icons/no-image.png";

    private static final Tika TIKA = new Tika();
    private static final TikaConfig TIKA_CONFIG = TikaConfig.getDefaultConfig();
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/png", "image/jpg", "image/jpeg", "image/webp", "image/gif");
    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "video/quicktime", "video/mp4", "video/mpeg", "video/ogg", "video/webm",
            "video/x-msvideo", "video/x-ms-wmv", "video/3gpp", "video/3gpp2");

    private final ResourceCompletionConfig config;
    private final ImageMagickService imageService;
    private final ResourceService resourceService;
    private final DjlImageEmbeddingService embeddingService;
    private final HashingAlgorithm hasher;

    private volatile LanguageDetector pdfLanguageDetector;

    /**
     * Creates the resource completion service.
     *
     * @param imageService image information extraction service
     * @param verticalConfigService vertical configuration service
     * @param resourceService local resource cache service
     * @param dataRepository product repository
     * @param apiProperties API configuration properties
     * @param embeddingService optional image embedding service
     */
    public ResourceCompletionService(ImageMagickService imageService,
                                     VerticalsConfigService verticalConfigService,
                                     ResourceService resourceService,
                                     ProductRepository dataRepository,
                                     ApiProperties apiProperties,
                                     DjlImageEmbeddingService embeddingService)
    {
        super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
        this.config = Optional.ofNullable(apiProperties.getResourceCompletionConfig())
                .orElseGet(ResourceCompletionConfig::new);
        this.imageService = imageService;
        this.resourceService = resourceService;
        this.embeddingService = embeddingService;
        this.hasher = new PerceptiveHash(config.getPerceptiveHashSize());
        this.hasher.setOpaqueHandling(Color.WHITE, config.getPerceptiveHashAlphaThreshold());
    }

    /**
     * Warms up the PDF language detector on startup to avoid first-hit latency spikes.
     */
    @PostConstruct
    public void warmPdfLanguageDetector()
    {
        logger.info("Warming up PDF language detector...");
        try
        {
            pdfLanguageDetector();
            logger.info("PDF language detector warmed up successfully.");
        }
        catch (Exception e)
        {
            logger.error("Failed to warm PDF language detector at startup", e);
        }
    }

    /**
     * Returns whether this product still needs resource completion.
     *
     * @param vertical vertical configuration
     * @param data product to inspect
     * @return true when resources need downloading, configured templates are
     *         missing, image grouping/cover data is stale, or override is enabled
     */
    @Override
    public boolean shouldProcess(VerticalConfig vertical, Product data)
    {
        if (overrideResources(vertical))
        {
            return true;
        }

        boolean hasUnprocessed = data.getResources().stream()
                .anyMatch(r -> !r.isProcessed() && !r.isEvicted());
        if (hasUnprocessed)
        {
            return true;
        }

        boolean missingTemplate = config.getUrlTemplates().stream()
                .map(tpl -> buildTemplateUrl(tpl, data.gtin()))
                .filter(StringUtils::isNotBlank)
                .anyMatch(url -> data.getResources().stream().noneMatch(r -> url.equals(r.getUrl())));
        if (missingTemplate)
        {
            return true;
        }

        boolean imageNeedsGrouping = data.getResources().stream()
                .filter(r -> !r.isEvicted())
                .filter(r -> r.getResourceType() == ResourceType.IMAGE)
                .anyMatch(r -> r.getGroup() == null);

        return imageNeedsGrouping || StringUtils.isBlank(data.getCoverImagePath());
    }

    /**
     * Resource completion does not map to one external datasource key.
     *
     * @return null
     */
    @Override
    public String getDatasourceName()
    {
        return null;
    }

    /**
     * Completes all product resources and updates the product cover image.
     *
     * @param vertical vertical configuration
     * @param data product being processed
     */
    @Override
    public void processProduct(VerticalConfig vertical, Product data)
    {
        addTemplateResources(data);
        data.getResources().forEach(r -> r.setGroup(null));

        List<Resource> resourcesToProcess = data.getResources().stream()
                .filter(r -> overrideResources(vertical) || !r.isProcessed())
                .filter(r -> overrideResources(vertical) || !r.isEvicted())
                .map(r -> fetchResource(r, vertical))
                .toList();

        data.getResources().forEach(r -> normalizeFileNameIfNeeded(r, data));

        List<Resource> retainedResources = filterRetainedResources(data, vertical);
        data.setResources(retainedResources.stream().collect(Collectors.toSet()));

        List<Resource> imageResources = retainedResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.IMAGE)
                .filter(r -> r.getImageInfo() != null)
                .toList();
        List<List<Resource>> clusters = classifyWithEmbeddings(imageResources);
        List<Resource> representativeImages = clusters.stream()
                .filter(cluster -> !cluster.isEmpty())
                .map(List::getFirst)
                .toList();

        logger.info("{} - {} resource links, {} processed, {} retained and classified in {} clusters",
                data.gtin(), data.getResources().size(), resourcesToProcess.size(), retainedResources.size(), clusters.size());

        setCoverImage(data, representativeImages);
    }

    /**
     * Downloads and analyzes one resource.
     *
     * @param resource resource to fetch
     * @param vertical vertical configuration
     * @return enriched resource
     */
    public Resource fetchResource(Resource resource, VerticalConfig vertical)
    {
        logger.info("Handling resource : {} ", resource);

        resource.setProcessed(true);
        resource.setCacheKey(IdHelper.generateResourceId(resource.getUrl()));
        resource.setTimeStamp(System.currentTimeMillis());

        File target = resourceService.getCacheFile(resource);
        if (!target.exists())
        {
            downloadResource(resource, target);
            if (resource.isEvicted())
            {
                return resource;
            }
        }
        else
        {
            logger.info("Resource found in file cache: {}", target);
        }

        resource.setFileSize(target.length());
        if (resource.getFileSize() == 0L)
        {
            logger.warn("Empty resource file: {}", resource.getUrl());
            resource.setStatus(ResourceStatus.EMPTY_FILE);
            resource.setEvicted(true);
            return resource;
        }

        if (!computeMd5(resource, target) || !detectMimeType(resource, target))
        {
            return resource;
        }

        try
        {
            if (IMAGE_MIME_TYPES.contains(resource.getMimeType()))
            {
                processImage(resource, target);
            }
            else if ("application/pdf".equals(resource.getMimeType()))
            {
                processPdf(resource, target);
            }
            else if (VIDEO_MIME_TYPES.contains(resource.getMimeType()))
            {
                processVideo(resource, target);
            }
            else
            {
                logger.warn("Unsupported resource type : {} : {}", resource.getMimeType(), resource.getUrl());
                resource.setResourceType(ResourceType.UNKNOWN);
                resource.setStatus(ResourceStatus.UNSUPPORTED_MIME_TYPE);
                resource.setEvicted(true);
            }
            logger.debug("Fetching and analysis done : {}", resource);
        }
        catch (Exception e)
        {
            logger.warn("Resource integration failed : {} : {}", e.getMessage(), resource);
            resource.setEvicted(true);
        }

        return resource;
    }

    /**
     * Adds or refreshes resources generated from configured URL templates.
     *
     * @param data product to update
     */
    private void addTemplateResources(Product data)
    {
        for (ResourceCompletionUrlTemplate template : config.getUrlTemplates())
        {
            Resource resource = processUrlTemplate(template, data.gtin());
            if (resource == null)
            {
                continue;
            }
            Resource existing = data.getResources().stream()
                    .filter(r -> resource.getUrl().equals(r.getUrl()))
                    .findFirst()
                    .orElse(null);
            if (existing == null)
            {
                data.getResources().add(resource);
            }
            else
            {
                existing.setDatasourceName(resource.getDatasourceName());
                existing.setHardTags(resource.getHardTags());
            }
        }
    }

    /**
     * Builds a resource from a configured URL template.
     *
     * @param template URL template
     * @param gtin product GTIN
     * @return resource, or null when the template URL is invalid
     */
    private Resource processUrlTemplate(ResourceCompletionUrlTemplate template, String gtin)
    {
        String url = buildTemplateUrl(template, gtin);
        if (StringUtils.isBlank(url))
        {
            logger.warn("Skipping blank resource URL template for GTIN {}", gtin);
            return null;
        }

        Resource resource = new Resource();
        resource.setUrl(url);
        resource.setDatasourceName(template.getDatasourceName());
        resource.getHardTags().addAll(template.getHardTags());
        // TODO(p3, i18n): Resource has no language field; map template language once the model supports localized resources.
        return resource;
    }

    /**
     * Replaces the GTIN placeholder in a URL template.
     *
     * @param template template definition
     * @param gtin product GTIN
     * @return resolved URL, or null when no URL is configured
     */
    private String buildTemplateUrl(ResourceCompletionUrlTemplate template, String gtin)
    {
        if (template == null || StringUtils.isBlank(template.getUrl()))
        {
            return null;
        }
        return template.getUrl().replace("{GTIN}", gtin);
    }

    /**
     * Generates a resource file name when missing, or always when configured to
     * force regeneration.
     *
     * @param resource resource to update
     * @param product product owning the resource
     */
    private void normalizeFileNameIfNeeded(Resource resource, Product product)
    {
        if (!config.isForceEraseFileName() && StringUtils.isNotBlank(resource.getFileName()))
        {
            return;
        }

        String name = buildResourceFileName(resource, product);
        if (StringUtils.isBlank(name))
        {
            name = product.gtin();
        }
        if (resource.getResourceType() == ResourceType.PDF && !resource.getHardTags().isEmpty())
        {
            String prefix = StringUtils.join(resource.getHardTags(), "-").toLowerCase();
            name = prefix + "-" + name;
        }
        resource.setFileName(IdHelper.normalizeFileName(name));
    }

    /**
     * Builds a stable, normalized base file name for a resource.
     *
     * @param resource resource to name
     * @param product product owning the resource
     * @return base file name before final normalization
     */
    private String buildResourceFileName(Resource resource, Product product)
    {
        if (!product.getOfferNames().isEmpty())
        {
            return product.getOfferNames().stream().sorted().findFirst().orElse(null);
        }
        if (StringUtils.isNotBlank(product.brand()) && StringUtils.isNotBlank(product.model()))
        {
            return product.brand() + "-" + product.model();
        }
        return basenameWithoutExtension(resource.getUrl());
    }

    /**
     * Extracts the final path segment from a URL and removes only its final
     * extension.
     *
     * @param url source URL
     * @return filename stem, or null when none can be extracted
     */
    private String basenameWithoutExtension(String url)
    {
        if (StringUtils.isBlank(url))
        {
            return null;
        }
        String path = url;
        try
        {
            path = URI.create(url).getPath();
        }
        catch (IllegalArgumentException e)
        {
            logger.debug("Cannot parse resource URL for filename: {}", url);
        }
        String name = StringUtils.substringAfterLast(path, "/");
        if (StringUtils.isBlank(name))
        {
            return null;
        }
        int extensionStart = name.lastIndexOf('.');
        return extensionStart > 0 ? name.substring(0, extensionStart) : name;
    }

    /**
     * Applies MD5 blacklist, MD5 duplicate and minimum image-size filters.
     *
     * @param data product resources to filter
     * @param vertical vertical resource rules
     * @return non-evicted resources
     */
    private List<Resource> filterRetainedResources(Product data, VerticalConfig vertical)
    {
        Set<String> md5s = new HashSet<>();
        ResourcesAggregationConfig resourcesConfig = vertical.getResourcesConfig();

        for (Resource resource : data.getResources())
        {
            if (resource.isEvicted())
            {
                continue;
            }
            String md5 = resource.getMd5();
            if (StringUtils.isNotBlank(md5) && resourcesConfig.getMd5Exclusions().contains(md5))
            {
                logger.info("Excluded because of blacklisted MD5 : {}", resource.getUrl());
                resource.setStatus(ResourceStatus.MD5_EXCLUSION);
                resource.setEvicted(true);
                continue;
            }
            if (StringUtils.isNotBlank(md5) && !md5s.add(md5))
            {
                logger.info("Excluded because of duplicate MD5 : {}", resource.getUrl());
                resource.setStatus(ResourceStatus.MD5_DUPLICATE);
                resource.setEvicted(true);
                continue;
            }
            if (resource.getResourceType() == ResourceType.IMAGE
                    && safePixels(resource) < resourcesConfig.getMinPixelsEvictionSize())
            {
                logger.info("Excluded because image is too small : {}", resource.getUrl());
                resource.setStatus(ResourceStatus.TOO_SMALL);
                resource.setEvicted(true);
            }
        }

        return data.getResources().stream()
                .filter(r -> !r.isEvicted())
                .toList();
    }

    /**
     * Picks the product cover from representative images.
     *
     * @param data product to update
     * @param representativeImages one image per image cluster
     */
    private void setCoverImage(Product data, List<Resource> representativeImages)
    {
        Resource cover = representativeImages.stream()
                .filter(r -> r.getHardTags().contains(ResourceTag.PRIMARY))
                .max(Comparator.comparingInt(this::safePixels))
                .orElse(null);

        if (cover == null)
        {
            cover = representativeImages.stream()
                    .filter(r -> r.getImageInfo() != null && r.getImageInfo().getConsistencyScore() != null)
                    .max(Comparator.comparingDouble(r -> r.getImageInfo().getConsistencyScore()))
                    .orElse(null);
        }
        if (cover == null && !representativeImages.isEmpty())
        {
            cover = representativeImages.getFirst();
        }

        if (cover == null)
        {
            logger.warn("No cover image found for product : {}", data.gtin());
            data.setCoverImagePath(NO_IMAGE_PATH);
        }
        else
        {
            data.setCoverImagePath(cover.path());
        }
    }

    /**
     * Downloads a resource into the local cache.
     *
     * @param resource remote resource
     * @param target target cache file
     */
    private void downloadResource(Resource resource, File target)
    {
        logger.info("Downloading resource to local file: {}", target);
        try
        {
            Request.Get(resource.getUrl())
                    .userAgent(config.getDownloadUserAgent())
                    .connectTimeout(config.getConnectTimeoutMs())
                    .socketTimeout(config.getSocketTimeoutMs())
                    .execute()
                    .saveContent(target);
        }
        catch (ClientProtocolException e)
        {
            logger.error("Cannot download ({}) : {}", e.getMessage(), resource.getUrl());
            resource.setStatus(ResourceStatus.PROTOCOL_EXCEPTION);
            resource.setEvicted(true);
            deletePartialDownload(target);
        }
        catch (Exception e)
        {
            logger.error("Cannot download ({}) : {}", e.getMessage(), resource.getUrl());
            resource.setStatus(ResourceStatus.IO_EXCEPTION);
            resource.setEvicted(true);
            deletePartialDownload(target);
        }
    }

    /**
     * Removes a partial cache file after a failed download.
     *
     * @param target target cache file
     */
    private void deletePartialDownload(File target)
    {
        if (target.exists() && !target.delete())
        {
            logger.warn("Could not delete partial resource download: {}", target);
        }
    }

    /**
     * Computes and stores a resource MD5 checksum.
     *
     * @param resource resource to update
     * @param target cached file
     * @return true when checksum computation succeeded
     */
    private boolean computeMd5(Resource resource, File target)
    {
        try (FileInputStream inputStream = new FileInputStream(target))
        {
            resource.setMd5(DigestUtils.md5Hex(inputStream));
            return true;
        }
        catch (Exception e)
        {
            logger.error("Cannot compute MD5 hash", e);
            resource.setStatus(ResourceStatus.MD5_CHECKSUM_FAIL);
            resource.setEvicted(true);
            return false;
        }
    }

    /**
     * Detects MIME type and extension using Tika.
     *
     * @param resource resource to update
     * @param target cached file
     * @return true when MIME detection succeeded
     */
    private boolean detectMimeType(Resource resource, File target)
    {
        try
        {
            resource.setMimeType(TIKA.detect(target));
            org.apache.tika.mime.MimeType mimeType = TIKA_CONFIG.getMimeRepository().forName(resource.getMimeType());
            String extension = mimeType.getExtension();
            resource.setExtension(StringUtils.removeStart(extension, "."));
            return true;
        }
        catch (Exception e)
        {
            logger.error("Cannot get mimetype ({}) : {}", e.getMessage(), resource.getUrl());
            resource.setStatus(ResourceStatus.NO_MIME_TYPE);
            resource.setEvicted(true);
            return false;
        }
    }

    /**
     * Sets basic metadata for video resources.
     *
     * @param resource resource to update
     * @param target local video file
     */
    private void processVideo(Resource resource, File target)
    {
        resource.setResourceType(ResourceType.VIDEO);
    }

    /**
     * Extracts PDF metadata, first-page title candidate and document language.
     *
     * @param resource resource to update
     * @param target local PDF file
     */
    private void processPdf(Resource resource, File target)
    {
        resource.setResourceType(ResourceType.PDF);
        try (PDDocument document = Loader.loadPDF(target))
        {
            PdfInfo pdfInfo = new PdfInfo();
            extractPdfMetadata(document, pdfInfo);
            extractPdfTitle(document, pdfInfo);
            detectPdfLanguage(document, pdfInfo, resource);
            resource.setPdfInfo(pdfInfo);
        }
        catch (IOException e)
        {
            logger.error("Failed to parse PDF: {}", e.getMessage());
            resource.setStatus(ResourceStatus.PDF_PARSING_ERROR);
            resource.setEvicted(true);
        }
    }

    /**
     * Copies standard PDF metadata into the resource model.
     *
     * @param document parsed PDF document
     * @param pdfInfo target PDF metadata object
     */
    private void extractPdfMetadata(PDDocument document, PdfInfo pdfInfo)
    {
        PDDocumentInformation info = document.getDocumentInformation();
        pdfInfo.setMetadataTitle(info.getTitle());
        pdfInfo.setAuthor(info.getAuthor());
        pdfInfo.setSubject(info.getSubject());
        pdfInfo.setKeywords(info.getKeywords());
        pdfInfo.setCreationDate(info.getCreationDate() != null ? info.getCreationDate().getTimeInMillis() : null);
        pdfInfo.setModificationDate(info.getModificationDate() != null ? info.getModificationDate().getTimeInMillis() : null);
        pdfInfo.setProducer(info.getProducer());
        pdfInfo.setNumberOfPages(document.getNumberOfPages());
    }

    /**
     * Extracts a title candidate from the largest first-page text.
     *
     * @param document parsed PDF document
     * @param pdfInfo target PDF metadata object
     * @throws IOException when PDF text extraction fails
     */
    private void extractPdfTitle(PDDocument document, PdfInfo pdfInfo) throws IOException
    {
        MultiLineTitleStripper stripper = new MultiLineTitleStripper(
                config.getPdfTitleMaxLines(), config.getPdfTitleFontSizeTolerance());
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        stripper.getText(document);
        pdfInfo.setExtractedTitle(stripper.getTitle());
    }

    /**
     * Detects PDF language from a bounded text sample.
     *
     * @param document parsed PDF document
     * @param pdfInfo target PDF metadata object
     * @param resource source resource for logging context
     */
    private void detectPdfLanguage(PDDocument document, PdfInfo pdfInfo, Resource resource)
    {
        try
        {
            String text = extractPdfText(document);
            if (StringUtils.isBlank(text))
            {
                return;
            }

            List<LanguageResult> results = pdfLanguageDetector().detectAll(text);
            if (results.isEmpty())
            {
                logger.warn("No language detected for PDF");
                return;
            }

            long distinctLanguages = results.stream()
                    .filter(result -> result.getRawScore() >= config.getPdfLanguageMinConfidence())
                    .map(LanguageResult::getLanguage)
                    .distinct()
                    .count();

            if (distinctLanguages > 1)
            {
                pdfInfo.setLanguage("Multilingue");
                pdfInfo.setLanguageConfidence(1.0);
            }
            else
            {
                LanguageResult primary = results.getFirst();
                pdfInfo.setLanguage(primary.getLanguage());
                pdfInfo.setLanguageConfidence(primary.getRawScore());
                logger.info("Language detected for PDF {}: {} (confidence: {})",
                        resource.getFileName(), primary.getLanguage(), primary.getRawScore());
            }
        }
        catch (Exception e)
        {
            logger.warn("Failed to detect document language: {}", e.getMessage());
        }
    }

    /**
     * Lazily loads and reuses PDF language detector models.
     *
     * @return language detector
     * @throws IOException when Tika cannot load language models
     */
    private LanguageDetector pdfLanguageDetector() throws IOException
    {
        LanguageDetector detector = pdfLanguageDetector;
        if (detector == null)
        {
            synchronized (this)
            {
                detector = pdfLanguageDetector;
                if (detector == null)
                {
                    detector = new OptimaizeLangDetector().loadModels();
                    pdfLanguageDetector = detector;
                }
            }
        }
        return detector;
    }

    /**
     * Extracts bounded text from a PDF for language detection.
     *
     * @param document parsed PDF document
     * @return text sample
     * @throws IOException when PDF text extraction fails
     */
    private String extractPdfText(PDDocument document) throws IOException
    {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(Math.min(document.getNumberOfPages(), config.getPdfLanguageMaxPages()));
        String text = stripper.getText(document);
        return text.length() > config.getPdfLanguageMaxChars()
                ? text.substring(0, config.getPdfLanguageMaxChars())
                : text;
    }

    /**
     * First-page stripper that keeps text rendered in the largest font.
     */
    private static class MultiLineTitleStripper extends PDFTextStripper
    {

        private final StringBuilder largestText = new StringBuilder();
        private final int maxLinesToRead;
        private final float fontSizeTolerance;
        private float largestFontSize = 0;
        private int currentLine = 0;

        MultiLineTitleStripper(int maxLinesToRead, float fontSizeTolerance) throws IOException
        {
            super();
            this.maxLinesToRead = maxLinesToRead;
            this.fontSizeTolerance = fontSizeTolerance;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException
        {
            if (currentLine >= maxLinesToRead || StringUtils.isBlank(text) || textPositions.isEmpty())
            {
                return;
            }
            currentLine++;

            float fontSize = 0;
            for (TextPosition position : textPositions)
            {
                fontSize += position.getFontSizeInPt();
            }
            fontSize /= textPositions.size();

            if (Math.abs(fontSize - largestFontSize) <= fontSizeTolerance)
            {
                largestText.append(text).append(' ');
            }
            else if (fontSize > largestFontSize)
            {
                largestFontSize = fontSize;
                largestText.setLength(0);
                largestText.append(text).append(' ');
            }
        }

        String getTitle()
        {
            return largestText.toString().trim();
        }
    }

    /**
     * Extracts image dimensions, perceptive hash and optional embedding.
     *
     * @param resource resource to update
     * @param src local image file
     */
    private void processImage(Resource resource, File src)
    {
        resource.setResourceType(ResourceType.IMAGE);
        ImageInfo imageInfo = imageService.buildImageInfo(src);
        if (imageInfo == null || imageInfo.getHeight() == null || imageInfo.getWidth() == null)
        {
            logger.error("Cannot analyse image : {}", resource.getUrl());
            resource.setStatus(ResourceStatus.CANNOT_ANALYSE);
            resource.setEvicted(true);
            return;
        }

        try
        {
            Hash hash;
            synchronized (hasher)
            {
                hash = hasher.hash(src);
            }
            imageInfo.setpHashValue(hash.getHashValue().longValue());
            imageInfo.setpHashLength(hash.getBitResolution());
        }
        catch (Exception e)
        {
            logger.error("Cannot compute perceptive hash ({}) : {}", e.getMessage(), resource.getUrl());
            resource.setStatus(ResourceStatus.PERCEPTIV_HASH_FAIL);
        }

        try
        {
            if (embeddingService != null)
            {
                imageInfo.setEmbedding(embeddingService.embed(src.toPath()));
            }
        }
        catch (Exception e)
        {
            logger.error("Cannot compute embedding ({}) : {}", e.getMessage(), resource.getUrl());
            if (e.getMessage() != null && e.getMessage().contains("attention_mask"))
            {
                logger.warn("Configured vision model may not support image-only inference; check embedding.vision-model-url.");
            }
            else if (e.getMessage() != null && e.getMessage().contains("NDManager"))
            {
                logger.warn("NDManager lifecycle issue detected during image embedding.");
            }
        }

        resource.setImageInfo(imageInfo);
    }

    /**
     * Groups image resources using embedding cosine similarity.
     *
     * @param images non-evicted image resources
     * @return sorted clusters
     */
    private List<List<Resource>> classifyWithEmbeddings(List<Resource> images)
    {
        logger.info("Starting image embedding-based clusterisation ({} images)", images.size());
        List<List<Resource>> clusters = new ArrayList<>();

        for (Resource resource : images)
        {
            float[] embedding = getEmbeddingSafe(resource);
            if (embedding == null)
            {
                // First image or images without embedding start their own clusters
                clusters.add(new ArrayList<>(List.of(resource)));
                continue;
            }

            int bestCluster = -1;
            double bestSimilarity = -1.0;
            for (int i = 0; i < clusters.size(); i++)
            {
                // Compare with the representative image (the first one) of each existing cluster
                float[] reference = getEmbeddingSafe(clusters.get(i).getFirst());
                if (reference == null || reference.length != embedding.length)
                {
                    continue;
                }
                double similarity = cosine(embedding, reference);
                if (similarity > bestSimilarity)
                {
                    bestSimilarity = similarity;
                    bestCluster = i;
                }
            }

            // Assign to the cluster with the highest cosine similarity, provided it exceeds the similarity threshold
            if (bestCluster == -1 || bestSimilarity < config.getEmbeddingSimilarityThreshold())
            {
                clusters.add(new ArrayList<>(List.of(resource)));
            }
            else
            {
                clusters.get(bestCluster).add(resource);
            }
        }

        // Sort images in each cluster by size (descending) so the largest image is representative
        clusters.forEach(cluster -> cluster.sort(Comparator.comparingInt(this::safePixels).reversed()));
        // Sort the clusters by size (descending) so the most popular image cluster comes first
        clusters.sort(Comparator.<List<Resource>>comparingInt(List::size).reversed());
        assignGroupsAndConsistency(clusters);
        return clusters;
    }

    /**
     * Assigns image group ids and consistency scores against the main cluster.
     *
     * @param clusters image clusters sorted by priority
     */
    private void assignGroupsAndConsistency(List<List<Resource>> clusters)
    {
        if (clusters.isEmpty())
        {
            return;
        }
        double[] mainCentroid = centroid(clusters.getFirst());
        for (int clusterId = 0; clusterId < clusters.size(); clusterId++)
        {
            for (Resource resource : clusters.get(clusterId))
            {
                resource.setGroup(clusterId);
                float[] embedding = getEmbeddingSafe(resource);
                if (embedding != null && embedding.length == mainCentroid.length && resource.getImageInfo() != null)
                {
                    resource.getImageInfo().setConsistencyScore(cosine(embedding, mainCentroid));
                }
            }
        }
    }

    /**
     * Returns a usable image embedding, if one is available.
     *
     * @param resource resource to inspect
     * @return embedding vector, or null
     */
    private float[] getEmbeddingSafe(Resource resource)
    {
        if (resource == null || resource.getImageInfo() == null)
        {
            return null;
        }
        float[] embedding = resource.getImageInfo().getEmbedding();
        return embedding == null || embedding.length == 0 ? null : embedding;
    }

    /**
     * Computes the mean embedding vector for a cluster.
     *
     * @param cluster resources in a cluster
     * @return centroid vector, or an empty vector when unavailable
     */
    private double[] centroid(List<Resource> cluster)
    {
        int dimension = cluster.stream()
                .map(this::getEmbeddingSafe)
                .filter(embedding -> embedding != null)
                .mapToInt(embedding -> embedding.length)
                .findFirst()
                .orElse(0);
        if (dimension == 0)
        {
            return new double[0];
        }

        double[] sum = new double[dimension];
        int count = 0;
        for (Resource resource : cluster)
        {
            float[] embedding = getEmbeddingSafe(resource);
            if (embedding == null || embedding.length != dimension)
            {
                continue;
            }
            for (int i = 0; i < dimension; i++)
            {
                sum[i] += embedding[i];
            }
            count++;
        }
        if (count == 0)
        {
            return new double[0];
        }
        for (int i = 0; i < dimension; i++)
        {
            sum[i] /= count;
        }
        return sum;
    }

    /**
     * Computes cosine similarity between a float vector and a double vector.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity, or zero for incompatible vectors
     */
    private double cosine(float[] a, double[] b)
    {
        if (a == null || b == null || a.length == 0 || a.length != b.length)
        {
            return 0.0;
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++)
        {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0.0 || nb == 0.0 ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Computes cosine similarity between two float vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity, or zero for incompatible vectors
     */
    private double cosine(float[] a, float[] b)
    {
        if (a == null || b == null || a.length == 0 || a.length != b.length)
        {
            return 0.0;
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++)
        {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0.0 || nb == 0.0 ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * Safely computes image pixel count.
     *
     * @param resource image resource
     * @return pixel count, or zero when metadata is missing
     */
    private int safePixels(Resource resource)
    {
        ImageInfo imageInfo = resource == null ? null : resource.getImageInfo();
        if (imageInfo == null || imageInfo.getHeight() == null || imageInfo.getWidth() == null)
        {
            return 0;
        }
        return imageInfo.getHeight() * imageInfo.getWidth();
    }

    /**
     * Returns whether vertical resources should be forcibly reprocessed.
     *
     * @param vertical vertical configuration
     * @return true when override is enabled
     */
    private boolean overrideResources(VerticalConfig vertical)
    {
        return Boolean.TRUE.equals(vertical.getResourcesConfig().getOverrideResources());
    }
}
