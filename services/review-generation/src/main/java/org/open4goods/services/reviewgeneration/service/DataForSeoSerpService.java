package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductSourceProvider;
import org.open4goods.model.product.ProductSourceQuery;
import org.open4goods.model.product.ProductSourceUrl;
import org.open4goods.model.product.ProductSourceUrlStatus;
import org.open4goods.model.product.ProductSourceUrlType;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.config.DataForSeoSerpConfig;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.dto.SourceDiscoveryJob;
import org.open4goods.services.reviewgeneration.dto.SourceDiscoveryJobStatus;
import org.open4goods.services.reviewgeneration.dto.SourceDiscoveryTask;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Discovers product source URLs through DataForSEO Standard SERP tasks.
 */
@Service
public class DataForSeoSerpService {

    private static final Logger logger = LoggerFactory.getLogger(DataForSeoSerpService.class);
    private static final String TASK_POST_PATH = "/v3/serp/google/organic/task_post";
    private static final String TASK_GET_PATH = "/v3/serp/google/organic/task_get/regular/";

    private final DataForSeoSerpConfig config;
    private final ReviewGenerationConfig reviewConfig;
    private final ProductRepository productRepository;
    @SuppressWarnings("unused")
    private final VerticalsConfigService verticalsConfigService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path trackingFolder;

    public DataForSeoSerpService(DataForSeoSerpConfig config, ReviewGenerationConfig reviewConfig,
            ProductRepository productRepository, VerticalsConfigService verticalsConfigService) {
        this.config = config;
        this.reviewConfig = reviewConfig;
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
        this.httpClient = HttpClient.newBuilder().connectTimeout(config.getRequestTimeout()).build();
        this.trackingFolder = Path.of(reviewConfig.getBatchFolder(), "source-discovery");
        try {
            Files.createDirectories(trackingFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create DataForSEO discovery tracking folder " + trackingFolder, e);
        }
    }

    /**
     * Submit URL discovery for one product.
     *
     * @param product product to discover
     * @param force submit even when source URLs already exist
     * @return local job descriptor
     * @throws IOException when the remote task or local tracking write fails
     * @throws InterruptedException when the remote call is interrupted
     */
    public SourceDiscoveryJob discoverUrls(Product product, boolean force) throws IOException, InterruptedException {
        if (!force && product.getSourceUrls() != null && !product.getSourceUrls().getUrls().isEmpty()) {
            SourceDiscoveryJob skipped = newJob(null);
            skipped.setStatus(SourceDiscoveryJobStatus.COMPLETED);
            skipped.setSubmittedTasks(0);
            skipped.setCompletedTasks(0);
            skipped.setDiscoveredUrls(product.getSourceUrls().getUrls().size());
            skipped.setError("Product already has source URLs; use force=true to rediscover.");
            persist(skipped);
            return skipped;
        }
        SourceDiscoveryTask task = new SourceDiscoveryTask(product.getId(), product.gtin(), buildDefaultQuery(product));
        SourceDiscoveryJob job = newJob(product.getVertical());
        job.setTasks(List.of(task));
        submitTasks(job);
        persist(job);
        return job;
    }

    /**
     * Submit URL discovery for products from a vertical.
     *
     * @param verticalId vertical identifier
     * @param limit maximum products to include
     * @param force include products that already carry source URLs
     * @return local job descriptor
     * @throws IOException when product loading, remote submission, or tracking fails
     * @throws InterruptedException when the remote call is interrupted
     */
    public SourceDiscoveryJob discoverUrlsForVertical(String verticalId, int limit, boolean force)
            throws IOException, InterruptedException {
        int effectiveLimit = Math.max(1, limit);
        List<Product> products;
        try (Stream<Product> productStream = productRepository.exportVerticalWithValidDateOrderByImpactScore(
                verticalId, effectiveLimit, false)) {
            products = productStream
                    .filter(product -> force || product.getSourceUrls() == null || product.getSourceUrls().getUrls().isEmpty())
                    .toList();
        }
        SourceDiscoveryJob job = newJob(verticalId);
        job.setTasks(products.stream()
                .map(product -> new SourceDiscoveryTask(product.getId(), product.gtin(), buildDefaultQuery(product)))
                .toList());
        submitTasks(job);
        persist(job);
        return job;
    }

    /**
     * Poll a local DataForSEO discovery job and persist discovered URLs on products.
     *
     * @param jobId local job identifier
     * @return updated job state
     * @throws IOException when tracking or product persistence fails
     * @throws InterruptedException when the remote call is interrupted
     */
    public SourceDiscoveryJob pollJob(String jobId) throws IOException, InterruptedException {
        SourceDiscoveryJob job = getJob(jobId);
        if (job.getStatus() == SourceDiscoveryJobStatus.COMPLETED || job.getStatus() == SourceDiscoveryJobStatus.FAILED) {
            return job;
        }
        job.setStatus(SourceDiscoveryJobStatus.POLLING);
        int discovered = job.getDiscoveredUrls();
        for (SourceDiscoveryTask task : job.getTasks()) {
            if (task.isCompleted() || task.getTaskId() == null || task.getTaskId().isBlank()) {
                continue;
            }
            try {
                List<ProductSourceUrl> urls = fetchTaskUrls(task);
                if (urls.isEmpty()) {
                    continue;
                }
                Product product = productRepository.getById(task.getProductId());
                product.getSourceUrls().setMaxStoredUrls(config.getMaxStoredUrls());
                urls.forEach(product.getSourceUrls()::add);
                productRepository.forceIndex(product);
                task.setCompleted(true);
                discovered += urls.size();
            } catch (Exception e) {
                task.setError(e.getMessage());
                logger.warn("DataForSEO discovery poll failed for job {}, task {}: {}", jobId, task.getTaskId(),
                        e.getMessage());
            }
        }
        job.setCompletedTasks((int) job.getTasks().stream().filter(SourceDiscoveryTask::isCompleted).count());
        job.setDiscoveredUrls(discovered);
        if (job.getCompletedTasks() == job.getSubmittedTasks()) {
            job.setStatus(SourceDiscoveryJobStatus.COMPLETED);
        }
        job.setUpdatedAt(System.currentTimeMillis());
        persist(job);
        return job;
    }

    public SourceDiscoveryJob getJob(String jobId) throws IOException {
        Path file = jobPath(jobId);
        if (!Files.exists(file)) {
            throw new IOException("Discovery job not found: " + jobId);
        }
        return objectMapper.readValue(Files.readString(file), SourceDiscoveryJob.class);
    }

    @Scheduled(fixedDelayString = "${dataforseo.serp.discovery-poll-interval:PT10M}")
    public void pollPendingJobs() {
        try (Stream<Path> files = Files.list(trackingFolder)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .forEach(jobId -> {
                        try {
                            SourceDiscoveryJob job = getJob(jobId);
                            if (job.getStatus() == SourceDiscoveryJobStatus.SUBMITTED
                                    || job.getStatus() == SourceDiscoveryJobStatus.POLLING) {
                                pollJob(jobId);
                            }
                        } catch (Exception e) {
                            logger.warn("Scheduled DataForSEO poll failed for job {}: {}", jobId, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.warn("Cannot scan DataForSEO discovery tracking folder {}: {}", trackingFolder, e.getMessage());
        }
    }

    String buildDefaultQuery(Product product) {
        String brand = product == null || product.brand() == null ? "" : product.brand().trim();
        String model = product == null || product.model() == null ? "" : product.model().trim();
        return String.format(config.getDefaultQueryTemplate(), brand, model);
    }

    private void submitTasks(SourceDiscoveryJob job) throws IOException, InterruptedException {
        if (job.getTasks().isEmpty()) {
            job.setSubmittedTasks(0);
            job.setCompletedTasks(0);
            job.setStatus(SourceDiscoveryJobStatus.COMPLETED);
            return;
        }
        validateCredentials();
        List<SourceDiscoveryTask> tasks = job.getTasks();
        int chunkSize = Math.min(100, Math.max(1, config.getMaxTasksPerPost()));
        for (int start = 0; start < tasks.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, tasks.size());
            submitTaskChunk(tasks.subList(start, end));
        }
        job.setSubmittedTasks(tasks.size());
        job.setCompletedTasks(0);
        job.setStatus(SourceDiscoveryJobStatus.SUBMITTED);
        job.setUpdatedAt(System.currentTimeMillis());
    }

    private void submitTaskChunk(List<SourceDiscoveryTask> tasks) throws IOException, InterruptedException {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (SourceDiscoveryTask task : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("language_code", config.getLanguageCode());
            item.put("location_name", config.getLocationName());
            item.put("se_domain", config.getSeDomain());
            item.put("device", config.getDevice());
            item.put("priority", config.getPriority());
            item.put("depth", config.getDepth());
            item.put("keyword", task.getQuery());
            item.put("tag", task.getProductId() == null ? null : task.getProductId().toString());
            payload.add(item);
        }
        HttpResponse<String> response = send(TASK_POST_PATH, "POST", objectMapper.writeValueAsString(payload));
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode responseTasks = root.path("tasks");
        int index = 0;
        for (JsonNode responseTask : responseTasks) {
            if (index >= tasks.size()) {
                break;
            }
            SourceDiscoveryTask task = tasks.get(index++);
            String taskId = responseTask.path("id").asText(null);
            if (taskId == null || taskId.isBlank()) {
                task.setError(responseTask.path("status_message").asText("DataForSEO did not return a task id"));
                continue;
            }
            task.setTaskId(taskId);
        }
    }

    private List<ProductSourceUrl> fetchTaskUrls(SourceDiscoveryTask task) throws IOException, InterruptedException {
        HttpResponse<String> response = send(TASK_GET_PATH + task.getTaskId(), "GET", null);
        JsonNode root = objectMapper.readTree(response.body());
        List<ProductSourceUrl> urls = new ArrayList<>();
        for (JsonNode responseTask : root.path("tasks")) {
            for (JsonNode result : responseTask.path("result")) {
                for (JsonNode item : result.path("items")) {
                    String type = item.path("type").asText("");
                    if (!"organic".equals(type) && !"featured_snippet".equals(type)) {
                        continue;
                    }
                    String url = item.path("url").asText(null);
                    if (url == null || url.isBlank()) {
                        continue;
                    }
                    ProductSourceUrl sourceUrl = new ProductSourceUrl(url);
                    sourceUrl.setTitle(item.path("title").asText(null));
                    sourceUrl.setSnippet(item.path("description").asText(null));
                    sourceUrl.setSerpRank(item.path("rank_group").asInt(item.path("rank_absolute").asInt(9999)));
                    sourceUrl.setProvider(ProductSourceProvider.DATAFORSEO);
                    sourceUrl.setStatus(ProductSourceUrlStatus.DISCOVERED);
                    sourceUrl.setType(classify(item, url));
                    sourceUrl.setQuery(new ProductSourceQuery(task.getQuery(), task.getTaskId(),
                            ProductSourceProvider.DATAFORSEO, Instant.now().toEpochMilli()));
                    urls.add(sourceUrl);
                }
            }
        }
        return urls.stream().limit(config.getMaxStoredUrls()).toList();
    }

    private ProductSourceUrlType classify(JsonNode item, String url) {
        String text = (url + " " + item.path("title").asText("") + " " + item.path("description").asText(""))
                .toLowerCase(Locale.ROOT);
        if (text.endsWith(".pdf") || text.contains(".pdf")) {
            return ProductSourceUrlType.OFFICIAL_PDF;
        }
        if (text.contains("support") || text.contains("assistance") || text.contains("manual")
                || text.contains("notice")) {
            return ProductSourceUrlType.OFFICIAL_SUPPORT;
        }
        if (text.contains("comparatif") || text.contains("comparison") || text.contains("compare")) {
            return ProductSourceUrlType.COMPARISON_PRODUCT_PAGE;
        }
        if (text.contains("test") || text.contains("review") || text.contains("avis")) {
            return ProductSourceUrlType.REVIEW;
        }
        if (text.contains("guide")) {
            return ProductSourceUrlType.GUIDE;
        }
        return ProductSourceUrlType.UNKNOWN;
    }

    private HttpResponse<String> send(String path, String method, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + path))
                .timeout(config.getRequestTimeout())
                .header("Authorization", "Basic " + basicAuth())
                .header("Accept", "application/json");
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body == null ? "[]" : body));
        } else {
            builder.GET();
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("DataForSEO HTTP " + response.statusCode() + ": " + response.body());
        }
        return response;
    }

    private String basicAuth() {
        String credentials = config.getUsername() + ":" + config.getPassword();
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void validateCredentials() {
        if (config.getUsername() == null || config.getUsername().isBlank()
                || config.getPassword() == null || config.getPassword().isBlank()) {
            throw new IllegalStateException("DataForSEO credentials are required: dataforseo.serp.username/password");
        }
    }

    private SourceDiscoveryJob newJob(String verticalId) {
        SourceDiscoveryJob job = new SourceDiscoveryJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setVerticalId(verticalId);
        job.setCreatedAt(System.currentTimeMillis());
        job.setUpdatedAt(job.getCreatedAt());
        return job;
    }

    private void persist(SourceDiscoveryJob job) throws IOException {
        Files.writeString(jobPath(job.getJobId()), objectMapper.writeValueAsString(job), StandardCharsets.UTF_8);
    }

    private Path jobPath(String jobId) {
        return trackingFolder.resolve(jobId + ".json");
    }
}
