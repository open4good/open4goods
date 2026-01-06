package org.open4goods.api.services.completion;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.open4goods.api.config.yml.ResourceCompletionUrlTemplate;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.embedding.service.image.DjlImageEmbeddingService;
import org.open4goods.services.imageprocessing.service.ImageMagickService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.model.resource.PdfInfo;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceStatus;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.resource.ResourceType;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

/**
 * Service responsible for "completing" resources of a product:
 * <ul>
 *     <li>Generate additional resource URLs from templates (e.g. GTIN-based image URLs)</li>
 *     <li>Download and cache resources locally</li>
 *     <li>Extract technical metadata for images, PDFs and videos</li>
 *     <li>Filter out bad or duplicate resources (MD5 blacklist, too small images, etc.)</li>
 *     <li>Group visually similar images per product using modern embeddings (category-agnostic)</li>
 *     <li>Choose a cover image for the product</li>
 * </ul>
 *
 * <p>
 * This implementation is designed to be:
 * <ul>
 *     <li>Category-agnostic (works for TVs, remotes, shoes, etc.)</li>
 *     <li>Per-product (grouping is done within a product only)</li>
 *     <li>Fully offline (no external services required)</li>
 * </ul>
 */
public class ResourceCompletionService extends AbstractCompletionService {

    /** Default path used when no valid cover image can be found. */
    private static final String NO_IMAGE_PATH = "/icons/no-image.png";

    /**
     * If true, will regenerate a new file name for resources even if one already
     * exists. Use with caution in production as it may trigger 301 on existing URLs.
     */
    // TODO(p3, conf): expose via YAML configuration
    private static boolean forceEraseFileName = false;

    /**
     * Embedding similarity threshold used to decide whether two images should be
     * grouped in the same cluster.
     *
     * <p>Cosine similarity is in [-1, 1], but with standard embeddings of images,
     * values typically range in [0, 1]. A threshold of 0.80 means "only images
     * that are quite similar are clustered together".</p>
     */
    private static final double EMBEDDING_SIMILARITY_THRESHOLD = 0.80;

    /**
     * Perceptive hash size used by the legacy pHash-based logic. We keep this
     * both for backward compatibility and as a cheap fallback when embedding
     * computation fails.
     */
    private static final int PERCEPTIVE_HASH_SIZE = 32;

    private static final Logger logger = LoggerFactory.getLogger(ResourceCompletionService.class);

    private final ApiProperties apiProperties;
    private final ImageMagickService imageService;
    private final ResourceService resourceService;
    private final DjlImageEmbeddingService embeddingService;

    private static final Tika tika = new Tika();
    private static final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

    // NOTE: jImageHash hashing algorithm (probably not thread safe).
    private static final HashingAlgorithm hasher = new PerceptiveHash(PERCEPTIVE_HASH_SIZE);

    /**
     * Constructs a new ResourceCompletionService.
     *
     * @param imageService        image information extraction service (width, height, etc.)
     * @param verticalConfigService vertical configuration (per vertical resource rules)
     * @param resourceService     local resource cache service
     * @param dataRepository      product repository
     * @param apiProperties       API configuration properties
     * @param embeddingService    image embedding service (category-agnostic features)
     */
    public ResourceCompletionService(ImageMagickService imageService,
                                     VerticalsConfigService verticalConfigService,
                                     ResourceService resourceService,
                                     ProductRepository dataRepository,
                                     ApiProperties apiProperties,
                                     DjlImageEmbeddingService embeddingService) {

        // Log level and log folder configured via ApiProperties
        super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());

        this.apiProperties = apiProperties;
        this.imageService = imageService;
        this.resourceService = resourceService;
        this.embeddingService = embeddingService;

        // Configure pHash handling (legacy, for transparency/alpha)
        int alphaThreshold = 243;
        hasher.setOpaqueHandling(Color.WHITE, alphaThreshold);
    }

    /**
     * Determines whether this product still has resources that need processing.
     *
     * @param vertical vertical configuration
     * @param data     product
     * @return {@code true} if at least one resource is not processed and not evicted,
     *         or if vertical is configured to override resources.
     */
    @Override
    public boolean shouldProcess(VerticalConfig vertical, Product data) {
        boolean hasUnprocessed = data.getResources().stream()
                .filter(e -> !e.isProcessed())
                .filter(e -> !e.isEvicted())
                .findAny()
                .isPresent();

        return hasUnprocessed || vertical.getResourcesConfig().getOverrideResources();
    }

    /**
     * This completion service does not correspond to any external datasource.
     *
     * @return {@code null}
     */
    @Override
    public String getDatasourceName() {
        return null;
    }

    /**
     * Main entry point: completes all resources for a single product.
     *
     * <p>High-level steps:</p>
     * <ol>
     *     <li>Generate additional resource URLs from templates</li>
     *     <li>Download and analyze all non-processed / non-evicted resources</li>
     *     <li>Normalize resource file names</li>
     *     <li>Filter invalid/duplicate images (MD5 blacklist, minimum pixels)</li>
     *     <li>Cluster similar images per product using embeddings</li>
     *     <li>Pick best representative of each cluster and choose a cover image</li>
     * </ol>
     *
     * @param vertical vertical configuration
     * @param data     product being processed
     */
    @Override
    public void processProduct(VerticalConfig vertical, Product data) {

        //////////////////////////////
        // 1. Generate URL-based resources if configured
        //////////////////////////////

        apiProperties.getResourceCompletionConfig().getUrlTemplates().forEach(tpl -> {
            data.getResources().add(processUrlTemplate(tpl, String.valueOf(data.gtin())));
        });

        // Reset group information; we will recompute clusters
        data.getResources().forEach(r -> r.setGroup(null));

        //////////////////////////////
        // 2. Fetch and analyze new or overridden resources
        //////////////////////////////

        List<Resource> resourcesToProcess = data.getResources().stream()
                .filter(r -> vertical.getResourcesConfig().getOverrideResources() || !r.isProcessed())
                .filter(r -> vertical.getResourcesConfig().getOverrideResources() || !r.isEvicted())
                .map(r -> fetchResource(r, vertical))
                .toList();

        // Merge updated resources back into product
        data.getResources().removeAll(resourcesToProcess);
        data.getResources().addAll(resourcesToProcess);

        //////////////////////////////
        // 3. Generate / normalize file names
        //////////////////////////////

        resourcesToProcess.forEach(r -> {
            if (forceEraseFileName || StringUtils.isEmpty(r.getFileName())) {
                String name = buildResourceFileName(r, data);
                if (StringUtils.isEmpty(name)) {
                    // Last resort, use GTIN.
                    name = String.valueOf(data.gtin());
                }

                // Prepend hard tags for PDFs (e.g. MANUAL, SPEC, etc.)
                if (r.getResourceType() == ResourceType.PDF && !r.getHardTags().isEmpty()) {
                    String prefix = StringUtils.join(r.getHardTags(), "-").toLowerCase();
                    name = prefix + "-" + name;
                }

                r.setFileName(IdHelper.normalizeFileName(name));
            }
        });

        //////////////////////////////
        // 4. Filter images by validity (MD5, duplicates, minimum size)
        //////////////////////////////

        Set<String> md5s = new HashSet<>();

        List<Resource> filteredResources = data.getResources().stream()
                .filter(r -> !r.isEvicted())
                // 4.1 MD5 blacklist
                .map(r -> {
                    if (vertical.getResourcesConfig().getMd5Exclusions().contains(r.getMd5())) {
                        logger.info("Excluded because of blacklisted MD5 : {}", r.getUrl());
                        r.setStatus(ResourceStatus.MD5_EXCLUSION);
                        r.setEvicted(true);
                    }
                    return r;
                })
                // 4.2 MD5 duplicates (exact binary duplicates)
                .map(r -> {
                    if (md5s.contains(r.getMd5())) {
                        logger.info("Excluded because of duplicate MD5 : {}", r.getUrl());
                        r.setStatus(ResourceStatus.MD5_DUPLICATE);
                        r.setEvicted(true);
                    }
                    md5s.add(r.getMd5());
                    return r;
                })
                // 4.3 Minimum pixel count
                .map(r -> {
                    if (r.getResourceType() == ResourceType.IMAGE &&
                            r.getImageInfo() != null &&
                            r.getImageInfo().pixels() < vertical.getResourcesConfig().getMinPixelsEvictionSize()) {
                        logger.info("Excluded because image is too small : {}", r.getUrl());
                        r.setStatus(ResourceStatus.TOO_SMALL);
                        r.setEvicted(true);
                    }
                    return r;
                })
                .toList();

        // Update product resources with filtered state
        data.getResources().removeAll(filteredResources);
        data.getResources().addAll(filteredResources);

        // Physically remove evicted resources from product (but not from disk)
        data.setResources(
                data.getResources().stream()
                        .filter(r -> !r.isEvicted())
                        .collect(Collectors.toSet())
        );

        //////////////////////////////
        // 5. Per-product image grouping (embeddings-based)
        //////////////////////////////

        List<Resource> imageResources = filteredResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.IMAGE)
                .filter(r -> !r.isEvicted())
                .toList();

        ArrayList<List<Resource>> clusters = classifyWithEmbeddings(imageResources);

        logger.info("{} - {} resource links, {} processed, {} retained and classified in {} clusters",
                data.gtin(),
                data.getResources().size(),
                resourcesToProcess.size(),
                filteredResources.size(),
                clusters.size());

        // For each cluster, pick the best representative (first one = largest resolution).
        List<Resource> representativeImages = new ArrayList<>();
        for (List<Resource> cluster : clusters) {
            if (cluster.isEmpty()) {
                continue;
            }
            logger.info("{} images in cluster (sorted by resolution) \n  {}",
                    cluster.size(), StringUtils.join(cluster, "\n  "));
            representativeImages.add(cluster.get(0));
        }

        logger.info("{}/{} images selected as representatives for product {} : \n  {}",
                representativeImages.size(),
                filteredResources.size(),
                data.gtin(),
                StringUtils.join(representativeImages, "\n  "));

        //////////////////////////////
        // 6. Cover image selection
        //////////////////////////////

        Resource cover = representativeImages.stream()
                .filter(r -> r.getHardTags().contains(ResourceTag.PRIMARY))
                .max((a, b) -> a.getImageInfo().pixels().compareTo(b.getImageInfo().pixels()))
                .orElse(null);

        // If no PRIMARY-tagged image, fallback to "most consistent" or any image
        if (cover == null) {
            cover = representativeImages.stream()
                    .filter(r -> r.getImageInfo() != null && r.getImageInfo().getConsistencyScore() != null)
                    .max((a, b) -> Double.compare(
                            a.getImageInfo().getConsistencyScore(),
                            b.getImageInfo().getConsistencyScore()))
                    .orElse(null);
        }

        if (cover == null && !representativeImages.isEmpty()) {
            cover = representativeImages.get(0);
        }

        if (cover == null) {
            logger.warn("No cover image found for product : {}", data.gtin());
            data.setCoverImagePath(NO_IMAGE_PATH);
        } else {
            data.setCoverImagePath(cover.path());
        }

        // Optional: delete evicted files to save disk space (disabled by default, as it may lead
        // to re-download & re-analysis in future runs).
        // for (Resource r : data.getResources()) {
        //     if (r.isEvicted()) {
        //         File evicted = resourceService.getCacheFile(r);
        //         // if (!evicted.delete()) {
        //         //     logger.error("Could not delete evicted resource : {}", evicted);
        //         // }
        //     }
        // }
    }

    /**
     * Builds a {@link Resource} from a URL template and a GTIN.
     *
     * @param ut   URL template
     * @param gtin product GTIN
     * @return new {@link Resource} with hard tags and URL set
     */
    private Resource processUrlTemplate(ResourceCompletionUrlTemplate ut, String gtin) {
        Resource r = new Resource();
        r.getHardTags().addAll(ut.getHardTags());
        // TODO(p3, i18n): add resource language if needed
        r.setUrl(ut.getUrl().replace("{GTIN}", gtin));
        return r;
    }

    /**
     * Creates a normalized file name for a resource based on product information.
     *
     * <p>Priority:</p>
     * <ol>
     *     <li>Random offer name (when available)</li>
     *     <li>Brand + model</li>
     *     <li>Derived from URL file name (without query & extension)</li>
     * </ol>
     *
     * @param resource resource
     * @param product  product
     * @return base file name (without extension)
     */
    private String buildResourceFileName(Resource resource, Product product) {
        List<String> offerNames = product.getOfferNames().stream().toList();
        String name = null;

        if (!offerNames.isEmpty()) {
            Random rand = new Random();
            name = offerNames.get(rand.nextInt(offerNames.size()));
        } else if (!StringUtils.isEmpty(product.brand()) && !StringUtils.isEmpty(product.model())) {
            name = product.brand() + "-" + product.randomModel();
        } else {
            // Fallback to filename part of URL
            name = resource.getUrl();
            if (name.contains("/")) {
                name = name.substring(name.lastIndexOf('/') + 1);
            }

            int qPos = name.indexOf('?');
            if (qPos != -1) {
                name = name.substring(0, qPos);
            }

            int extPos = name.indexOf('.');
            if (extPos != -1) {
                name = name.substring(0, extPos);
            }
        }
        return name;
    }

    /**
     * Download the resource if not cached yet, then detect type and extract
     * metadata depending on the mime type.
     *
     * @param resource resource to fetch
     * @param vertical vertical configuration
     * @return the enriched resource
     */
    public Resource fetchResource(Resource resource, VerticalConfig vertical) {
        logger.info("Handling resource : {} ", resource);

        resource.setProcessed(true);
        resource.setCacheKey(IdHelper.generateResourceId(resource.getUrl()));
        resource.setTimeStamp(System.currentTimeMillis());

        File target = resourceService.getCacheFile(resource);

        // 1. Download if not already cached
        if (target.exists()) {
            logger.info("Resource found in file cache: {}", target);
        } else {
            logger.info("Downloading resource to local file: {}", target);
            try {
                Request.Get(resource.getUrl())
                        // TODO(p2, conf): user agent & timeouts from configuration
                        .userAgent("Mozilla/5.0 (Windows NT 5.1; rv:5.0.1) Gecko/20100101 Firefox/5.0.1")
                        .connectTimeout(1000)
                        .socketTimeout(1000)
                        .execute()
                        .saveContent(target);
            } catch (ClientProtocolException e) {
                logger.error("Cannot download ({}) : {}", e.getMessage(), resource.getUrl());
                resource.setStatus(ResourceStatus.PROTOCOL_EXCEPTION);
                resource.setEvicted(true);
                return resource;
            } catch (Exception e) {
                logger.error("Cannot download ({}) : {}", e.getMessage(), resource.getUrl());
                resource.setStatus(ResourceStatus.IO_EXCEPTION);
                resource.setEvicted(true);
                return resource;
            }
        }

        // 2. File size
        resource.setFileSize(target.length());

        // 3. Compute MD5 checksum
        try (FileInputStream fis = new FileInputStream(target)) {
            String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
            resource.setMd5(md5);
        } catch (Exception e) {
            logger.error("Cannot compute MD5 hash", e);
            resource.setStatus(ResourceStatus.MD5_CHECKSUM_FAIL);
            resource.setEvicted(true);
            return resource;
        }

        // 4. Detect mime type via Tika
        try {
            resource.setMimeType(tika.detect(target));
            org.apache.tika.mime.MimeType mimeType = tikaConfig.getMimeRepository().forName(resource.getMimeType());
            resource.setExtension(mimeType.getExtension().substring(1));
        } catch (Exception e) {
            logger.error("Cannot get mimetype ({}) : {}", e.getMessage(), resource.getUrl());
            resource.setStatus(ResourceStatus.NO_MIME_TYPE);
            resource.setEvicted(true);
            return resource;
        }

        // 5. Type-specific processing
        try {
            switch (resource.getMimeType()) {
                case "image/png":
                case "image/jpg":
                case "image/jpeg":
                case "image/webp":
                case "image/gif":
                    processImage(resource, target);
                    resource.setProcessed(true);
                    break;

                case "application/pdf":
                    processPdf(resource, target);
                    resource.setProcessed(true);
                    break;

                case "video/quicktime":
                    processVideo(resource, target);
                    break;

                default:
                    logger.warn("Unknown resource type : {} : {}", resource.getMimeType(), resource.getUrl());
                    resource.setResourceType(ResourceType.UNKNOWN);
            }

            logger.debug("Fetching and analysis done : {}", resource);
        } catch (Exception e) {
            logger.warn("Resource integration failed : {} : {}", e.getMessage(), resource);
        }

        return resource;
    }

    /**
     * Sets basic metadata for video resources. Currently minimal.
     *
     * @param resource resource
     * @param target   local video file
     */
    private void processVideo(Resource resource, File target) {
        resource.setResourceType(ResourceType.VIDEO);
        // Placeholder: could extract duration, resolution, etc. in future.
    }

    /**
     * Process a PDF file to extract metadata, detect language and identify the title.
     *
     * <p>This method performs three main operations:</p>
     * <ol>
     *     <li>Extract standard PDF metadata (title, author, dates, etc.)</li>
     *     <li>Detect the document's language using text content analysis</li>
     *     <li>Extract the most prominent text from the first page as a potential title</li>
     * </ol>
     *
     * @param resource resource to update
     * @param target   local PDF file
     */
    private void processPdf(Resource resource, File target) {
        resource.setResourceType(ResourceType.PDF);

        try (PDDocument document = Loader.loadPDF(target)) {
            logger.info("PDF loaded successfully: {}", target.getName());
            PdfInfo pdfInfo = new PdfInfo();

            extractPdfMetadata(document, pdfInfo);
            extractPdfTitle(document, pdfInfo);
            detectPdfLanguage(document, pdfInfo, resource);

            resource.setPdfInfo(pdfInfo);
            resource.setProcessed(true);

            logger.info("PDF processed successfully: {}", target.getName());

        } catch (IOException e) {
            logger.error("Failed to parse PDF: {}", e.getMessage());
            resource.setStatus(ResourceStatus.PDF_PARSING_ERROR);
            resource.setEvicted(true);
        }
    }

    /**
     * Extracts standard PDF metadata.
     */
    private void extractPdfMetadata(PDDocument document, PdfInfo pdfInfo) {
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
     * Extracts a candidate title by taking the largest font text from the first page.
     */
    private void extractPdfTitle(PDDocument document, PdfInfo pdfInfo) throws IOException {
        MultiLineTitleStripper stripper = new MultiLineTitleStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        stripper.getText(document);
        pdfInfo.setExtractedTitle(stripper.getTitle().trim());
        logger.info("Manually extracted title: {}", pdfInfo.getExtractedTitle());
    }

    /**
     * Detects the language of a PDF using its text content.
     */
    private void detectPdfLanguage(PDDocument document, PdfInfo pdfInfo, Resource resource) {
        try {
            String text = extractPdfText(document);

            LanguageDetector detector = new OptimaizeLangDetector().loadModels();
            List<LanguageResult> results = detector.detectAll(text);

            if (results.isEmpty()) {
                logger.warn("No language detected for PDF");
                return;
            }

            // TODO : From conf
            double MIN_CONFIDENCE = 0.5;

            long distinctLanguages = results.stream()
                    .filter(r -> r.getRawScore() >= MIN_CONFIDENCE)
                    .map(LanguageResult::getLanguage)
                    .distinct()
                    .count();

            if (distinctLanguages > 1) {
                pdfInfo.setLanguage("Multilingue");
                pdfInfo.setLanguageConfidence(1.0);
                logger.info("Multiple languages detected for PDF: MULTILINGUE");
            } else {
                LanguageResult primary = results.get(0);
                pdfInfo.setLanguage(primary.getLanguage());
                pdfInfo.setLanguageConfidence(primary.getRawScore());
                logger.info("Language detected for PDF {}: {} (confidence: {})",
                        resource.getFileName(), primary.getLanguage(), primary.getRawScore());
            }

        } catch (Exception e) {
            logger.warn("Failed to detect document language: {}", e.getMessage());
        }
    }

    /**
     * Extracts all text from a PDF document. Instantiates a new PDFTextStripper
     * each time since it is not thread-safe.
     */
    private String extractPdfText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(document);
    }

    /**
     * PDFTextStripper specialization that keeps track of the text with largest
     * font size over a limited number of lines. Used to guess PDF titles.
     */
    private static class MultiLineTitleStripper extends PDFTextStripper {

        private StringBuilder largestText = new StringBuilder();
        private float largestFontSize = 0;
        private final float fontSizeTolerance = 0.8f;
        private int maxLinesToRead = 10;
        private int currentLine = 0;

        public MultiLineTitleStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (currentLine >= maxLinesToRead) {
                return;
            }

            currentLine++;

            if (text.trim().isEmpty()) {
                return;
            }

            float fontSize = 0;
            for (TextPosition tp : textPositions) {
                fontSize += tp.getFontSizeInPt();
            }
            fontSize /= textPositions.size();

            if (Math.abs(fontSize - largestFontSize) <= fontSizeTolerance) {
                largestText.append(text).append(" ");
            } else if (fontSize > largestFontSize) {
                largestFontSize = fontSize;
                largestText.setLength(0);
                largestText.append(text).append(" ");
            }
        }

        public String getTitle() {
            return largestText.toString().trim();
        }
    }

    /**
     * Processes an image file to extract:
     * <ul>
     *     <li>Basic image metadata (width, height, pixel count)</li>
     *     <li>Perceptive hash (pHash) for cheap similarity checks (legacy)</li>
     *     <li>Embeddings via {@link DjlImageEmbeddingService} for modern, category-agnostic grouping</li>
     * </ul>
     *
     * @param resource resource to update
     * @param src      local image file
     */
    private void processImage(Resource resource, File src) {
        resource.setResourceType(ResourceType.IMAGE);

        // 1. Basic image info (dimensions, pixels)
        ImageInfo imageInfo = imageService.buildImageInfo(src);
        if (imageInfo == null) {
            logger.error("Cannot analyse image : {}", resource.getUrl());
            resource.setStatus(ResourceStatus.CANNOT_ANALYSE);
            resource.setEvicted(true);
            return;
        }

        // 2. Perceptive hash (legacy, can be helpful for rare cases / debugging)
        try {
            Hash hash = hasher.hash(src);
            imageInfo.setpHashValue(hash.getHashValue().longValue());
            imageInfo.setpHashLength(hash.getBitResolution());
        } catch (IOException e) {
            logger.error("Cannot compute perceptive hash ({}) : {}", e.getMessage(), resource.getUrl());
            resource.setStatus(ResourceStatus.PERCEPTIV_HASH_FAIL);
            // We don't necessarily evict the resource here; we can still use embeddings.
        }

        // 3. Embedding (modern, category-agnostic representation)
        try {
            float[] embedding = embeddingService.embed(src.toPath());
            imageInfo.setEmbedding(embedding);
        } catch (Exception e) {
            logger.error("Cannot compute embedding ({}) : {}", e.getMessage(), resource.getUrl());

            // Provide helpful hint for common issues
            if (e.getMessage() != null && e.getMessage().contains("attention_mask")) {
                logger.warn("Model compatibility issue detected. The configured model may not support image-only inference. " +
                           "Consider using a pure vision model (e.g., ResNet, EfficientNet) or a different CLIP export. " +
                           "See the embedding.vision-model-url configuration for alternatives.");
            } else if (e.getMessage() != null && e.getMessage().contains("NDManager")) {
                logger.warn("NDManager lifecycle issue detected. This may indicate a concurrency problem or resource leak.");
            }

            // Decide policy: for now we keep the image even without embedding, but
            // it will be treated as its own singleton cluster later.
        }

        resource.setImageInfo(imageInfo);
    }

    /**
     * Groups images of a product based on embeddings.
     *
     * <p>Algorithm:</p>
     * <ol>
     *     <li>Iterate over images one by one</li>
     *     <li>For each image, find the existing cluster whose representative is most similar (cosine)</li>
     *     <li>If best similarity &lt; threshold, create a new cluster; otherwise, add to that cluster</li>
     *     <li>Sort images inside each cluster by resolution (pixels) descending</li>
     *     <li>Sort clusters by size descending</li>
     *     <li>Assign group ids, and compute per-image "consistencyScore" as similarity to main cluster centroid</li>
     * </ol>
     *
     * @param images non-evicted image resources of a product
     * @return list of clusters (each cluster is a list of resources)
     */
    private ArrayList<List<Resource>> classifyWithEmbeddings(List<Resource> images) {
        logger.info("Starting image embedding-based clusterisation ({} images)", images.size());

        // 1. Build initial clusters
        List<List<Resource>> clusters = new ArrayList<>();

        for (Resource r : images) {
            float[] emb = getEmbeddingSafe(r);
            // If no embedding, treat as its own cluster (best-effort).
            if (emb == null) {
                List<Resource> singleton = new ArrayList<>();
                singleton.add(r);
                clusters.add(singleton);
                continue;
            }

            int bestClusterIdx = -1;
            double bestSim = -1.0;

            // 2. Compare to cluster representatives (first element in each cluster)
            for (int i = 0; i < clusters.size(); i++) {
                Resource ref = clusters.get(i).get(0);
                float[] refEmb = getEmbeddingSafe(ref);
                if (refEmb == null) {
                    continue;
                }

                double sim = cosine(emb, refEmb);
                if (sim > bestSim) {
                    bestSim = sim;
                    bestClusterIdx = i;
                }
            }

            // 3. Decide whether to join an existing cluster or create a new one
            if (bestClusterIdx == -1 || bestSim < EMBEDDING_SIMILARITY_THRESHOLD) {
                List<Resource> newCluster = new ArrayList<>();
                newCluster.add(r);
                clusters.add(newCluster);
            } else {
                clusters.get(bestClusterIdx).add(r);
            }
        }

        // 4. Sort each cluster by resolution (pixels) descending
        for (List<Resource> cluster : clusters) {
            cluster.sort((o1, o2) ->
                    o2.getImageInfo().pixels().compareTo(o1.getImageInfo().pixels()));
        }

        // 5. Sort clusters by size (largest first)
        clusters.sort((o1, o2) -> Integer.compare(o2.size(), o1.size()));

        // 6. Assign group ids and compute consistencyScore vs. main cluster centroid
        if (!clusters.isEmpty()) {
            double[] mainCentroid = centroid(clusters.get(0));

            for (int clusterId = 0; clusterId < clusters.size(); clusterId++) {
                List<Resource> cluster = clusters.get(clusterId);
                double[] clusterCentroid = centroid(cluster);

                for (Resource r : cluster) {
                    r.setGroup(clusterId);
                    float[] emb = getEmbeddingSafe(r);
                    if (emb != null) {
                        double score = cosine( emb, mainCentroid);
                        if (r.getImageInfo() != null) {
                            r.getImageInfo().setConsistencyScore(score);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(clusters);
    }

    /**
     * Safely returns the embedding of a resource's image, if available.
     *
     * @param r resource
     * @return embedding or {@code null} if not available
     */
    private float[] getEmbeddingSafe(Resource r) {
        if (r == null || r.getImageInfo() == null) {
            return null;
        }
        return r.getImageInfo().getEmbedding();
    }

    /**
     * Computes the centroid (mean vector) of the embeddings of all images in a cluster.
     *
     * @param cluster resources in the cluster
     * @return centroid vector, or zero vector if no embeddings available
     */
    private double[] centroid(List<Resource> cluster) {
        float[] firstEmb = null;
        for (Resource r : cluster) {
            firstEmb = getEmbeddingSafe(r);
            if (firstEmb != null) {
                break;
            }
        }
        if (firstEmb == null) {
            // No embedding at all: return empty centroid (treated as zero)
            return new double[0];
        }

        int dim = firstEmb.length;
        double[] sum = new double[dim];
        int count = 0;

        for (Resource r : cluster) {
            float[] emb = getEmbeddingSafe(r);
            if (emb == null) {
                continue;
            }
            for (int i = 0; i < dim; i++) {
                sum[i] += emb[i];
            }
            count++;
        }

        if (count == 0) {
            return new double[dim];
        }

        for (int i = 0; i < dim; i++) {
            sum[i] /= count;
        }
        return sum;
    }



    /**
     * Computes cosine similarity where the embedding is float[] and centroid is double[].
     *
     * @param a image embedding (float vector)
     * @param b centroid vector (double vector)
     * @return cosine similarity in [-1, 1], or 0 if any norm is zero
     */
    private double cosine(float[] a, double[] b) {

        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0;
        }

        int dim = Math.min(a.length, b.length);

        double dot = 0.0;
        double na = 0.0;   // norm of a (float)
        double nb = 0.0;   // norm of b (double)

        for (int i = 0; i < dim; i++) {
            double av = a[i];  // promote to double
            double bv = b[i];

            dot += av * bv;
            na  += av * av;
            nb  += bv * bv;
        }

        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }


    /**
     * Computes cosine similarity where the embedding is float[] and centroid is double[].
     *
     * @param a image embedding (float vector)
     * @param b centroid vector (double vector)
     * @return cosine similarity in [-1, 1], or 0 if any norm is zero
     */
    private double cosine(float[] a, float[] b) {

        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0;
        }

        int dim = Math.min(a.length, b.length);

        double dot = 0.0;
        double na = 0.0;   // norm of a (float)
        double nb = 0.0;   // norm of b (double)

        for (int i = 0; i < dim; i++) {
            double av = a[i];  // promote to double
            double bv = b[i];

            dot += av * bv;
            na  += av * av;
            nb  += bv * bv;
        }

        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }


    /**
     * Reconstructs a jImageHash {@link Hash} from stored pHash data.
     *
     * <p>Kept for backward compatibility and for debugging. The main logic now
     * relies on embeddings for grouping.</p>
     *
     * @param r resource
     * @return pHash representation
     */
    @SuppressWarnings("unused")
    private Hash getHash(Resource r) {
        if (r.getImageInfo() == null) {
            return null;
        }
        return new Hash(
                BigInteger.valueOf(r.getImageInfo().getpHashValue()),
                r.getImageInfo().getpHashLength(),
                0);
    }
}
