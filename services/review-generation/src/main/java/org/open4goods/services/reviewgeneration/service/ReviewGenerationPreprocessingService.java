package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductFact;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceType;
import org.open4goods.model.util.ProductModelCandidateHelper;
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.exception.GoogleSearchException;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig.FetchQualityThreshold;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationFailureDetails;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.services.urlfetching.dto.ExtractedMetadataAttribute;
import org.open4goods.services.urlfetching.dto.ExtractedResource;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Builds prompt variables by discovering and fetching web sources for a
 * product.
 * <p>
 * The fetch workflow applies a deterministic fallback chain: HTTP simple, then
 * Playwright local rendering, then ZenRows anti-bot provider.
 * </p>
 */
@Service
public class ReviewGenerationPreprocessingService {

	private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationPreprocessingService.class);
	private static final List<Pattern> BLOCKED_FETCH_CONTENT_PATTERNS = List.of(
			Pattern.compile("<title>\\s*Just a moment\\.\\.\\.\\s*</title>", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Enable JavaScript and cookies to continue", Pattern.CASE_INSENSITIVE),
			Pattern.compile("/cdn-cgi/challenge-platform/", Pattern.CASE_INSENSITIVE),
			Pattern.compile("__cf_chl_(?:tk|rt_tk|f_tk)", Pattern.CASE_INSENSITIVE),
			Pattern.compile("window\\._cf_chl_opt", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Checking if the site connection is secure", Pattern.CASE_INSENSITIVE),
			Pattern.compile("Attention Required! \\| Cloudflare", Pattern.CASE_INSENSITIVE));
	private static final Pattern OFFICIAL_MODEL_LINE_PATTERN = Pattern.compile(
			"(?i)\\b(?:model|modele|reference|mpn|sku|code produit|product code)\\b\\s*[:#\\-]?\\s*([A-Za-z0-9][A-Za-z0-9._/\\- ]{2,48})");
	private static final Pattern URL_MODEL_TOKEN_PATTERN = Pattern.compile(
			"[A-Za-z]{1,10}\\d[A-Za-z0-9]{2,}(?:[-_/][A-Za-z0-9]{1,10}){0,3}|\\d[A-Za-z]{1,10}[A-Za-z0-9]{2,}(?:[-_/][A-Za-z0-9]{1,10}){0,3}");
	private static final int OFFICIAL_MODEL_PROMOTION_THRESHOLD = 7;

	private final ReviewGenerationConfig properties;
	private final GoogleSearchService googleSearchService;
	private final UrlFetchingService urlFetchingService;
	private final PromptService genAiService;
	private final SerialisationService serialisationService;
	private final MeterRegistry meterRegistry;
	private final ThreadPoolExecutor fetchExecutor;

	private record FetchOutcome(FetchResponse response, String rejectionReason) {
	}

	private record ModelEvidence(String candidate, int score, List<String> reasons) {
	}

	private enum FetchResultQuality {
		COMPLETE,
		PARTIAL_USABLE,
		FAILED
	}

	public ReviewGenerationPreprocessingService(ReviewGenerationConfig properties,
			GoogleSearchService googleSearchService, UrlFetchingService urlFetchingService, PromptService genAiService,
			SerialisationService serialisationService, MeterRegistry meterRegistry) {
		this.properties = properties;
		this.googleSearchService = googleSearchService;
		this.urlFetchingService = urlFetchingService;
		this.genAiService = genAiService;
		this.serialisationService = serialisationService;
		this.meterRegistry = meterRegistry;
		this.fetchExecutor = new ThreadPoolExecutor(properties.getMaxConcurrentFetch(), properties.getMaxQueueSize(),
				0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(properties.getMaxQueueSize() * 10),
				new ThreadPoolExecutor.AbortPolicy());
		logger.info(
				"Review generation retrieval configured: maxSearch={}, resultsPerQuery={}, preferredDomains={}, preferredDomainsByVertical={}, lr={}, cr={}, gl={}, hl={}, safe={}",
				properties.getMaxSearch(), properties.getSearchResultsPerQuery(), properties.getPreferredDomains(),
				properties.getPreferredDomainsByVertical().keySet(),
				properties.getSearchLanguageRestrict(), properties.getSearchCountryRestrict(),
				properties.getSearchGeoLocation(), properties.getSearchHostLanguage(), properties.getSearchSafe());
	}

	/**
	 * Validates configuration at startup and logs actionable warnings for common
	 * misconfigurations.
	 */
	@PostConstruct
	public void validateConfig() {
		if (properties.getPreferredDomains() == null || properties.getPreferredDomains().isEmpty()) {
			logger.warn("review.generation.preferred-domains is empty. No preferred-domain SERP boost will be applied. "
					+ "Configure e.g. lesnumeriques.com, fnac.com to improve source quality.");
		} else {
			boolean hasInvalidDomain = properties.getPreferredDomains().stream().anyMatch(
					d -> d != null && (d.startsWith("http://") || d.startsWith("https://") || d.contains("/")));
			if (hasInvalidDomain) {
				logger.warn("review.generation.preferred-domains contains entries with scheme or path. "
						+ "Use bare hostnames only (e.g. 'lesnumeriques.com'), not full URLs.");
			}
		}
		if (properties.getPreferredDomainsByVertical() != null) {
			properties.getPreferredDomainsByVertical().forEach((vertical, domains) -> {
				if (domains == null) {
					return;
				}
				boolean hasInvalidDomain = domains.stream().anyMatch(
						d -> d != null && (d.startsWith("http://") || d.startsWith("https://") || d.contains("/")));
				if (hasInvalidDomain) {
					logger.warn("review.generation.preferred-domains-by-vertical[{}] contains entries with scheme or path. "
							+ "Use bare hostnames only.", vertical);
				}
			});
		}
		if (properties.getMinUrlCount() > properties.getMaxUrlsPerProduct()) {
			logger.warn(
					"review.generation.min-url-count ({}) exceeds max-urls-per-product ({}). "
							+ "Generation will always fail with NotEnoughDataException.",
					properties.getMinUrlCount(), properties.getMaxUrlsPerProduct());
		}
	}

	/**
	 * Prepares the prompt variables used for review generation.
	 * <p>
	 * This common code builds search queries from the product’s brand and model,
	 * performs Google searches via GoogleSearchService, fetches URL content
	 * concurrently via UrlFetchingService, aggregates markdown sources along with
	 * token counts, and finally composes the prompt variables.
	 * </p>
	 *
	 * @param product        the product.
	 * @param verticalConfig the vertical configuration.
	 * @param status         the process status to update.
	 * @return a map of prompt variables.
	 * @throws IOException,          InterruptedException, ExecutionException,
	 *                               SerialisationException,
	 *                               ResourceNotFoundException,
	 *                               NotEnoughDataException
	 * @throws GoogleSearchException
	 */

	public Map<String, Object> preparePromptVariables(Product product, VerticalConfig verticalConfig,
			ReviewGenerationStatus status) throws IOException, InterruptedException, ExecutionException,
			ResourceNotFoundException, SerialisationException, NotEnoughDataException, GoogleSearchException {
		return preparePromptVariables(product, verticalConfig, status, null);
	}

	public Map<String, Object> preparePromptVariables(Product product, VerticalConfig verticalConfig,
			ReviewGenerationStatus status, Map<String, String> customHeaders)
			throws IOException, InterruptedException, ExecutionException, ResourceNotFoundException,
			SerialisationException, NotEnoughDataException, GoogleSearchException {
		String brand = product.brand();
		String primaryModel = product.model();
		Set<String> alternateModels = product.getAkaModels();
		validateSearchKeys(product, brand, primaryModel);
		List<String> preferredDomains = effectivePreferredDomains(verticalConfig);

		// Build search queries.
		List<String> queries = buildSearchQueries(product, brand, primaryModel, alternateModels, verticalConfig,
				preferredDomains);
		logger.info(
				"SERP validation for UPC {}: brand='{}', model='{}', akaModels={}, preferredDomains={}, plannedQueries={}",
				product.getId(), brand, primaryModel, alternateModels == null ? 0 : alternateModels.size(),
				preferredDomains, queries.size());

		status.addMessage("Searching the web...");
		int searchesMade = 0;
		List<String> searchedQueries = new ArrayList<>();
		List<GoogleSearchResult> allResults = new ArrayList<>();
		int maxSearch = properties.getMaxSearch();
		for (String query : queries) {
			if (searchesMade >= maxSearch) {
				break;
			}

			logger.info("SERP query {}/{} for UPC {}: {}", searchesMade + 1, maxSearch, product.getId(), query);
			status.addMessage("Executing search query: " + query);
			searchedQueries.add(query);
			GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, properties.getSearchResultsPerQuery(),
					properties.getSearchLanguageRestrict(), properties.getSearchCountryRestrict(),
					properties.getSearchSafe(), null, properties.getSearchGeoLocation(),
					properties.getSearchHostLanguage());
			GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
			searchesMade++;
			if (searchResponse != null && searchResponse.results() != null) {
				logger.info("SERP query returned {} results for UPC {}: {}", searchResponse.results().size(),
						product.getId(), query);
				allResults.addAll(searchResponse.results());
			} else {
				logger.warn("SERP query returned no response for UPC {}: {}", product.getId(), query);
			}
		}
		logger.info("SERP aggregation for UPC {}: searchesMade={}, rawResults={}", product.getId(), searchesMade,
				allResults.size());

		// GTIN fallback: when the primary brand+model queries returned no SERP results,
		// try GTIN-based queries (pure GTIN, brand+GTIN, cleaned model) as a second pass.
		if (allResults.isEmpty() && searchesMade < maxSearch) {
			List<String> gtinFallbackQueries = buildGtinFallbackQueries(product, brand);
			if (!gtinFallbackQueries.isEmpty()) {
				status.addMessage("Primary SERP yielded no results — trying GTIN fallback queries...");
				logger.info("GTIN fallback activated for UPC {} (0 primary results, {} fallback queries planned)",
						product.getId(), gtinFallbackQueries.size());
				for (String query : gtinFallbackQueries) {
					if (searchesMade >= maxSearch) {
						break;
					}
					logger.info("GTIN fallback query {}/{} for UPC {}: {}", searchesMade + 1, maxSearch,
							product.getId(), query);
					status.addMessage("GTIN fallback query: " + query);
					searchedQueries.add(query);
					GoogleSearchRequest searchRequest = new GoogleSearchRequest(query,
							properties.getSearchResultsPerQuery(), properties.getSearchLanguageRestrict(),
							properties.getSearchCountryRestrict(), properties.getSearchSafe(), null,
							properties.getSearchGeoLocation(), properties.getSearchHostLanguage());
					GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
					searchesMade++;
					if (searchResponse != null && searchResponse.results() != null) {
						logger.info("GTIN fallback query returned {} results for UPC {}: {}",
								searchResponse.results().size(), product.getId(), query);
						allResults.addAll(searchResponse.results());
					} else {
						logger.warn("GTIN fallback query returned no response for UPC {}: {}", product.getId(), query);
					}
				}
				logger.info("SERP after GTIN fallback for UPC {}: searchesMade={}, rawResults={}",
						product.getId(), searchesMade, allResults.size());
			}
		}

		status.setStatus(ReviewGenerationStatus.Status.FETCHING);
		identifyOfficialProductUrl(product, allResults).ifPresent(officialUrl -> {
			product.setOfficialUrl(officialUrl);
			status.addMessage("Official manufacturer page identified: " + officialUrl);
			logger.info("Official manufacturer page identified for UPC {}: {}", product.getId(), officialUrl);
		});
		identifyOfficialSupportUrls(product, allResults).forEach(supportUrl -> {
			String language = resolveOfficialUrlLanguage(supportUrl);
			product.addOfficialSupportUrl(language, supportUrl);
			status.addMessage("Official manufacturer support page identified: " + supportUrl);
			logger.info("Official manufacturer support page identified for UPC {}: language={}, url={}", product.getId(),
					language, supportUrl);
		});
		if (fetchOfficialEvidence(product, allResults, customHeaders)) {
			primaryModel = product.model();
			alternateModels = product.getAkaModels();
			status.addMessage("Product model refined from official manufacturer evidence: " + primaryModel);
		}

		// Sort and deduplicate results.
		List<GoogleSearchResult> sortedResults = sortSearchResults(product, allResults, preferredDomains);
		long preferredResultCount = sortedResults.stream().filter(
				result -> preferredDomains.stream().anyMatch(domain -> result.link().contains(domain)))
				.count();
		logger.info("SERP selection for UPC {}: eligibleResults={}, preferredResults={}, maxUrlsPerProduct={}",
				product.getId(), sortedResults.size(), preferredResultCount, properties.getMaxUrlsPerProduct());

		// Fetch URL contents concurrently. PDFs are skipped: they will be persisted as
		// resources for attributes extraction, not fed to the review prompt.
		Map<String, CompletableFuture<FetchOutcome>> fetchFutures = scheduleFetches(sortedResults, customHeaders);

		status.setStatus(ReviewGenerationStatus.Status.ANALYSING);
		status.addMessage("Fetching URL content concurrently...");

		int accumulatedTokens = 0;
		Map<String, String> finalSourcesMap = new LinkedHashMap<>();
		Map<String, Integer> finalTokensMap = new LinkedHashMap<>();
		Map<String, String> rejectedUrls = new LinkedHashMap<>();

		accumulatedTokens = collectFetchedSources(product, sortedResults, fetchFutures, finalSourcesMap, finalTokensMap,
				rejectedUrls, accumulatedTokens, brand, primaryModel, alternateModels, status);
		if (isBelowCompleteThreshold(verticalConfig, accumulatedTokens, finalSourcesMap)
				&& hasOfficialFetchEvidence(product, finalSourcesMap)) {
			accumulatedTokens = runPartialRetry(product, brand, primaryModel, alternateModels, preferredDomains,
					searchedQueries, sortedResults, fetchFutures, finalSourcesMap, finalTokensMap, rejectedUrls,
					accumulatedTokens, customHeaders, status);
		}
		logger.info("Aggregated {} tokens from {} sources.", accumulatedTokens, finalSourcesMap.size());

		// Persist fetched facts before threshold check so partial results survive
		// failed runs and are available for future EPREL/Icecat enrichment.
		List<ProductFact> newFacts = new ArrayList<>();
		for (Map.Entry<String, String> entry : finalSourcesMap.entrySet()) {
			String content = entry.getValue();
			String normalized = content.length() > properties.getFactMaxMarkdownChars()
					? content.substring(0, properties.getFactMaxMarkdownChars())
					: content;
			newFacts.add(new ProductFact(entry.getKey(), normalized, detectLanguage(normalized),
					System.currentTimeMillis(), resolveFetchStrategy(fetchFutures.get(entry.getKey())),
					finalTokensMap.get(entry.getKey()), sha256(normalized)));
		}
		product.setReviewFacts(newFacts.stream().limit(properties.getFactsMaxStored()).toList());

		FetchResultQuality resultQuality = classifyFetchResult(verticalConfig, accumulatedTokens, finalSourcesMap);
		if (resultQuality == FetchResultQuality.FAILED) {
			ReviewGenerationFailureDetails details = new ReviewGenerationFailureDetails(finalSourcesMap.size(),
					accumulatedTokens, searchedQueries, new ArrayList<>(finalSourcesMap.keySet()), rejectedUrls);
			throw new NotEnoughDataException("Insufficient data for review generation: accumulatedTokens="
					+ accumulatedTokens + ", sources=" + finalSourcesMap.size(), details);
		}

		Map<String, Object> promptVariables = buildBasePromptVariables(product, verticalConfig);
		promptVariables.put("sources", finalSourcesMap);
		promptVariables.put("tokens", finalTokensMap);
		promptVariables.put("SEARCHED_QUERIES", searchedQueries);
		promptVariables.put("ACCEPTED_URLS", new ArrayList<>(finalSourcesMap.keySet()));
		promptVariables.put("REJECTED_URLS", rejectedUrls);
		promptVariables.put("RESULT_QUALITY", resultQuality.name());
		status.addMessage("AI generation");

		// Store aggregated tokens for convenience.
		promptVariables.put("TOTAL_TOKENS", accumulatedTokens);
		promptVariables.put("SOURCE_TOKENS", finalTokensMap);

		return promptVariables;
	}

	private void validateSearchKeys(Product product, String brand, String primaryModel) {
		List<String> missing = new ArrayList<>();
		if (brand == null || brand.isBlank()) {
			missing.add("brand");
		}
		if (primaryModel == null || primaryModel.isBlank()) {
			missing.add("model");
		}
        if (!missing.isEmpty())
        {
            throw new IllegalStateException("Cannot build review SERP queries for UPC " + product.getId() + ": missing "
                    + String.join(", ", missing));
        }
    }

	/**
	 * Checks if fetched content is relevant to a product. A model mention buried in
	 * recommendations or seller boilerplate is not sufficient; the model must match
	 * in the SERP title, URL, H1/early page body, or concise primary content zone.
	 */
	private boolean isRelevantContent(String content, GoogleSearchResult result, String brand, String primaryModel,
			Set<String> alternateModels) {
		if (content == null || content.isBlank() || brand == null || brand.isBlank()) {
			return false;
		}
		String normalizedBrand = normalizeForTextMatching(brand);
		List<String> modelsToCheck = orderedModels(primaryModel, alternateModels);
		if (modelsToCheck.isEmpty()) {
			return normalizeForTextMatching(content).contains(normalizedBrand);
		}

		String title = result == null ? "" : result.title();
		String url = result == null ? "" : result.link();
		String earlyContent = firstContentZone(content);
		String searchableZone = normalizeForTextMatching(String.join("\n", title, url, earlyContent));
		if (!searchableZone.contains(normalizedBrand)) {
			// Compound brand names (e.g. "LG Electronics", "Samsung Electronics") often appear as
			// individual words on product pages. Fall back to checking each word of the brand.
			boolean anyBrandWordMatches = Arrays.stream(brand.split("\\s+"))
					.map(this::normalizeForTextMatching)
					.filter(w -> w.length() >= 2)
					.anyMatch(searchableZone::contains);
			if (!anyBrandWordMatches) {
				return false;
			}
		}
		return modelsToCheck.stream().anyMatch(model -> modelMatchesZone(model, searchableZone));
	}

	private boolean isRelevantContent(String content, String brand, String primaryModel, Set<String> alternateModels) {
		return isRelevantContent(content, new GoogleSearchResult("", ""), brand, primaryModel, alternateModels);
	}

	private String firstContentZone(String content) {
		if (content == null) {
			return "";
		}
		String[] lines = content.split("\\R");
		List<String> selected = new ArrayList<>();
		int chars = 0;
		for (String line : lines) {
			String trimmed = line == null ? "" : line.trim();
			if (trimmed.isBlank()) {
				continue;
			}
			if (trimmed.startsWith("#") || selected.size() < 80) {
				selected.add(trimmed);
				chars += trimmed.length();
			}
			if (chars >= 4000) {
				break;
			}
		}
		return String.join("\n", selected);
	}

	private boolean modelMatchesZone(String model, String normalizedZone) {
		if (model == null || model.isBlank()) {
			return false;
		}
		String normalizedModel = normalizeForTextMatching(model);
		if (normalizedModel.length() >= 4 && normalizedZone.contains(normalizedModel)) {
			return true;
		}
		for (String token : model.split("[\\s_\\-\\./\\\\]+")) {
			if (isStrongModelToken(token) && normalizedZone.contains(normalizeForTextMatching(token))) {
				return true;
			}
		}
		return false;
	}

	private List<String> effectivePreferredDomains(VerticalConfig verticalConfig) {
		Map<String, List<String>> domainsByVertical = properties.getPreferredDomainsByVertical() == null ? Map.of()
				: properties.getPreferredDomainsByVertical();
		if (verticalConfig != null && verticalConfig.getId() != null) {
			List<String> verticalDomains = sanitizeDomains(domainsByVertical.get(verticalConfig.getId()));
			if (!verticalDomains.isEmpty()) {
				return verticalDomains;
			}
		}
		return sanitizeDomains(properties.getPreferredDomains());
	}

	private List<String> sanitizeDomains(List<String> domains) {
		if (domains == null) {
			return List.of();
		}
		return domains.stream()
				.filter(domain -> domain != null && !domain.isBlank())
				.map(String::trim)
				.distinct()
				.toList();
	}

	private List<GoogleSearchResult> sortSearchResults(Product product, List<GoogleSearchResult> results,
			List<String> preferredDomains) {
		List<String> fetchExcludedDomains = properties.getExcludedDomains() == null ? List.of()
				: properties.getExcludedDomains();
		List<String> domains = preferredDomains == null ? List.of() : preferredDomains;
		return results.stream()
				.filter(r -> r.link() != null && !r.link().isEmpty())
				.filter(r -> {
					try {
						String host = URI.create(r.link()).toURL().getHost();
						return fetchExcludedDomains.stream().noneMatch(host::contains);
					} catch (Exception e) {
						return true;
					}
				})
				.filter(distinctByKey(r -> {
					try {
						URL url = URI.create(r.link()).toURL();
						return url.getHost() + url.getPath();
					} catch (Exception e) {
						return r.link();
					}
				})).sorted((r1, r2) -> {
					boolean r1Preferred = domains.stream().anyMatch(domain -> r1.link().contains(domain));
					boolean r2Preferred = domains.stream().anyMatch(domain -> r2.link().contains(domain));
					boolean r1Official = isOfficialUrl(product, r1);
					boolean r2Official = isOfficialUrl(product, r2);
					if (r1Official && !r2Official) {
						return -1;
					}
					if (!r1Official && r2Official) {
						return 1;
					}
					if (r1Preferred && !r2Preferred) {
						return -1;
					}
					if (!r1Preferred && r2Preferred) {
						return 1;
					}
					return 0;
				}).toList();
	}

	private Map<String, CompletableFuture<FetchOutcome>> scheduleFetches(List<GoogleSearchResult> sortedResults,
			Map<String, String> customHeaders) {
		Map<String, CompletableFuture<FetchOutcome>> fetchFutures = new HashMap<>();
		for (GoogleSearchResult result : sortedResults.stream().limit(properties.getMaxUrlsPerProduct()).toList()) {
			String url = result.link();
			if (isPdfUrl(url)) {
				continue;
			}
			CompletableFuture<FetchOutcome> future = CompletableFuture.supplyAsync(() -> {
				try {
					return fetchWithFallbacks(url, customHeaders);
				} catch (Exception e) {
					logger.warn("Failed to fetch content from URL {}: {}", url, e.getMessage());
					return new FetchOutcome(null, "fetch exception: " + e.getMessage());
				}
			}, fetchExecutor);
			fetchFutures.put(url, future);
		}
		return fetchFutures;
	}

	private int collectFetchedSources(Product product, List<GoogleSearchResult> sortedResults,
			Map<String, CompletableFuture<FetchOutcome>> fetchFutures, Map<String, String> finalSourcesMap,
			Map<String, Integer> finalTokensMap, Map<String, String> rejectedUrls, int accumulatedTokens,
			String brand, String primaryModel, Set<String> alternateModels, ReviewGenerationStatus status)
			throws InterruptedException, ExecutionException {
		int maxTotalTokens = properties.getMaxTotalTokens();
		int minTokens = properties.getSourceMinTokens();
		int maxTokens = properties.getSourceMaxTokens();
		for (GoogleSearchResult result : sortedResults) {
			String url = result.link();
			if (finalSourcesMap.containsKey(url) || rejectedUrls.containsKey(url)) {
				continue;
			}
			if (isPdfUrl(url)) {
				if (isProductRelevantResource(product, url, result.title())) {
					persistOfficialResources(product, result, null);
					rejectedUrls.put(url, "pdf source excluded from review prompt; persisted for attributes extraction");
				} else {
					rejectedUrls.put(url, "pdf source excluded: not specific enough to the product");
				}
				continue;
			}
			CompletableFuture<FetchOutcome> future = fetchFutures.get(url);
			if (future == null) {
				continue;
			}
			FetchOutcome outcome = future.get();
			FetchResponse fetchResponse = outcome == null ? null : outcome.response();
			persistOfficialResources(product, result, fetchResponse);
			if (fetchResponse == null || fetchResponse.markdownContent() == null
					|| fetchResponse.markdownContent().isEmpty()) {
				rejectedUrls.put(url, outcome == null ? "fetch returned no response" : outcome.rejectionReason());
				continue;
			}
			String content = sanitizeMarkdown(fetchResponse.markdownContent(), url);
			if (!isRelevantContent(content, result, brand, primaryModel, alternateModels)) {
				String reason = "irrelevant: missing brand/model match in title, h1/main content, or URL";
				rejectedUrls.put(url, reason);
				logger.warn("Content from URL {} discarded due to irrelevance for brand {} and model {}", url, brand,
						primaryModel);
				continue;
			}
			int tokenCount = genAiService.estimateTokens(content);
			if (tokenCount < minTokens) {
				rejectedUrls.put(url, "insufficient tokens: " + tokenCount);
				logger.warn("Content from URL {} discarded due to insufficient tokens: {}", url, tokenCount);
				continue;
			}
			if (tokenCount > maxTokens) {
				if (isOfficialUrl(product, result)) {
					String trimmedContent = trimToTokenLimit(content, tokenCount, maxTokens, minTokens);
					int trimmedTokenCount = genAiService.estimateTokens(trimmedContent);
					if (trimmedTokenCount >= minTokens && trimmedTokenCount <= maxTokens) {
						logger.info("Official source for UPC {} trimmed from {} to {} tokens: {}", product.getId(),
								tokenCount, trimmedTokenCount, url);
						content = trimmedContent;
						tokenCount = trimmedTokenCount;
					} else {
						rejectedUrls.put(url, "too many tokens after official-page trimming: " + trimmedTokenCount);
						logger.warn("Content from official URL {} discarded after trimming, token count: {}", url,
								trimmedTokenCount);
						continue;
					}
				} else {
					rejectedUrls.put(url, "too many tokens: " + tokenCount);
					logger.warn("Content from URL {} discarded, exceed tokens limit: {}", url, tokenCount);
					continue;
				}
			}
			if (accumulatedTokens + tokenCount > maxTotalTokens) {
				logger.warn("Reached max tokens threshold. Current tokens: {}, URL tokens: {}, threshold: {}",
						accumulatedTokens, tokenCount, maxTotalTokens);
				break;
			}
			persistAcceptedOfficialUrl(product, result);
			finalSourcesMap.put(url, content);
			finalTokensMap.put(url, tokenCount);
			accumulatedTokens += tokenCount;
			logger.info("Accepted source for UPC {}: url={}, strategy={}, tokens={}, accumulatedTokens={}",
					product.getId(), url, resolveFetchStrategy(fetchFutures.get(url)), tokenCount, accumulatedTokens);
			try {
				String domain = URI.create(url).toURL().getHost();
				status.addMessage("Analysing " + domain);
			} catch (Exception e) {
				status.addMessage("Analysing " + url);
			}
		}
		return accumulatedTokens;
	}

	private void persistAcceptedOfficialUrl(Product product, GoogleSearchResult result) {
		if (product == null || result == null || result.link() == null || result.link().isBlank()) {
			return;
		}
		if (product.getOfficialUrl() != null || isOfficialSupportUrl(result) || !isOfficialUrl(product, result)) {
			return;
		}
		product.setOfficialUrl(result.link());
		logger.info("Official manufacturer page persisted from accepted source for UPC {}: {}",
				product.getId(), result.link());
	}

	private boolean isBelowCompleteThreshold(VerticalConfig verticalConfig, int accumulatedTokens,
			Map<String, String> finalSourcesMap) {
		return classifyFetchResult(verticalConfig, accumulatedTokens, finalSourcesMap) != FetchResultQuality.COMPLETE;
	}

	private FetchResultQuality classifyFetchResult(VerticalConfig verticalConfig, int accumulatedTokens,
			Map<String, String> finalSourcesMap) {
		int sourceCount = finalSourcesMap == null ? 0 : finalSourcesMap.size();
		FetchQualityThreshold threshold = fetchThreshold(verticalConfig);
		if (accumulatedTokens >= threshold.getMinGlobalTokens() && sourceCount >= threshold.getMinUrlCount()) {
			return FetchResultQuality.COMPLETE;
		}
		if (accumulatedTokens >= threshold.getPartialMinGlobalTokens()
				&& sourceCount >= threshold.getPartialMinUrlCount()) {
			return FetchResultQuality.PARTIAL_USABLE;
		}
		return FetchResultQuality.FAILED;
	}

	private FetchQualityThreshold fetchThreshold(VerticalConfig verticalConfig) {
		String verticalId = verticalConfig == null ? null : verticalConfig.getId();
		Map<String, FetchQualityThreshold> thresholdsByVertical = properties.getFetchThresholdsByVertical();
		FetchQualityThreshold configured = verticalId == null || thresholdsByVertical == null
				? null
				: thresholdsByVertical.get(verticalId);
		if (configured != null) {
			return normalizeThreshold(configured);
		}
		int completeTokens = properties.getMinGlobalTokens();
		int completeSources = properties.getMinUrlCount();
		int partialTokens = Math.max(properties.getSourceMinTokens(), completeTokens / 2);
		int partialSources = Math.max(1, Math.min(2, completeSources));
		return new FetchQualityThreshold(completeTokens, completeSources, partialTokens, partialSources);
	}

	private FetchQualityThreshold normalizeThreshold(FetchQualityThreshold threshold) {
		int completeTokens = threshold.getMinGlobalTokens() > 0
				? threshold.getMinGlobalTokens()
				: properties.getMinGlobalTokens();
		int completeSources = threshold.getMinUrlCount() > 0
				? threshold.getMinUrlCount()
				: properties.getMinUrlCount();
		int partialTokens = threshold.getPartialMinGlobalTokens() > 0
				? threshold.getPartialMinGlobalTokens()
				: Math.max(properties.getSourceMinTokens(), completeTokens / 2);
		int partialSources = threshold.getPartialMinUrlCount() > 0
				? threshold.getPartialMinUrlCount()
				: Math.max(1, Math.min(2, completeSources));
		return new FetchQualityThreshold(completeTokens, completeSources, partialTokens, partialSources);
	}

	private boolean hasOfficialFetchEvidence(Product product, Map<String, String> finalSourcesMap) {
		if (product == null || finalSourcesMap == null || finalSourcesMap.isEmpty()) {
			return false;
		}
		if (product.getOfficialUrl() != null && finalSourcesMap.containsKey(product.getOfficialUrl())) {
			return true;
		}
		if (product.getOfficialSupportUrls() != null
				&& product.getOfficialSupportUrls().values().stream().flatMap(Set::stream)
						.anyMatch(finalSourcesMap::containsKey)) {
			return true;
		}
		return product.getResources() != null && product.getResources().stream()
				.anyMatch(resource -> resource != null && "manufacturer".equals(resource.getDatasourceName())
						&& resource.getTags() != null && resource.getTags().contains("official"));
	}

	private int runPartialRetry(Product product, String brand, String primaryModel, Set<String> alternateModels,
			List<String> preferredDomains, List<String> searchedQueries, List<GoogleSearchResult> alreadySortedResults,
			Map<String, CompletableFuture<FetchOutcome>> fetchFutures, Map<String, String> finalSourcesMap,
			Map<String, Integer> finalTokensMap, Map<String, String> rejectedUrls, int accumulatedTokens,
			Map<String, String> customHeaders, ReviewGenerationStatus status)
			throws IOException, GoogleSearchException, InterruptedException, ExecutionException {
		int retryMaxSearch = Math.max(0, properties.getPartialRetryMaxSearch());
		if (retryMaxSearch == 0) {
			return accumulatedTokens;
		}
		List<String> retryQueries = buildPartialRetryQueries(product, brand, primaryModel).stream()
				.filter(query -> !searchedQueries.contains(query))
				.limit(retryMaxSearch)
				.toList();
		if (retryQueries.isEmpty()) {
			return accumulatedTokens;
		}
		status.addMessage("Official data found, searching targeted review and guide sources...");
		List<GoogleSearchResult> retryResults = new ArrayList<>();
		for (String query : retryQueries) {
			logger.info("Partial source retry query for UPC {}: {}", product.getId(), query);
			searchedQueries.add(query);
			GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, properties.getSearchResultsPerQuery(),
					properties.getSearchLanguageRestrict(), properties.getSearchCountryRestrict(),
					properties.getSearchSafe(), null, properties.getSearchGeoLocation(),
					properties.getSearchHostLanguage());
			GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
			if (searchResponse != null && searchResponse.results() != null) {
				retryResults.addAll(searchResponse.results());
			}
		}
		if (retryResults.isEmpty()) {
			return accumulatedTokens;
		}
		identifyOfficialProductUrl(product, retryResults).ifPresent(product::setOfficialUrl);
		identifyOfficialSupportUrls(product, retryResults).forEach(supportUrl ->
				product.addOfficialSupportUrl(resolveOfficialUrlLanguage(supportUrl), supportUrl));
		Set<String> knownUrls = new HashSet<>();
		alreadySortedResults.stream().map(GoogleSearchResult::link).forEach(knownUrls::add);
		List<GoogleSearchResult> sortedRetryResults = sortSearchResults(product, retryResults, preferredDomains).stream()
				.filter(result -> !knownUrls.contains(result.link()))
				.toList();
		Map<String, CompletableFuture<FetchOutcome>> retryFutures = scheduleFetches(sortedRetryResults, customHeaders);
		fetchFutures.putAll(retryFutures);
		return collectFetchedSources(product, sortedRetryResults, fetchFutures, finalSourcesMap, finalTokensMap,
				rejectedUrls, accumulatedTokens, brand, primaryModel, alternateModels, status);
	}

	private List<String> buildPartialRetryQueries(Product product, String brand, String primaryModel) {
		List<String> queries = new ArrayList<>();
		String model = primaryModel == null ? "" : primaryModel;
		if (brand != null && !brand.isBlank() && !model.isBlank()) {
			queries.add(brand + " " + quoted(model) + " (manual OR notice OR \"fiche produit\" OR datasheet OR \"energy label\")");
			queries.add(brand + " " + quoted(model) + " (avis OR review OR test OR guide)");
		}
		if (product != null && product.gtin() != null && product.gtin().matches("\\d{8,14}")) {
			queries.add(brand + " " + quoted(product.gtin()) + " (manual OR notice OR review OR test)");
		}
		return queries.stream().distinct().toList();
	}

	private List<String> buildSearchQueries(Product product, String brand, String primaryModel, Set<String> alternateModels,
			VerticalConfig verticalConfig, List<String> preferredDomains) {
		List<String> queries = new ArrayList<>();
		List<String> orderedModels = rankedModelCandidates(product, primaryModel, alternateModels);
		List<String> searchModels = orderedModels.stream().limit(6).toList();
		if (searchModels.isEmpty()) {
			throw new IllegalStateException("Cannot build review SERP queries for UPC " + product.getId()
					+ ": no usable model candidate");
		}
		String modelExpression = modelExpression(brand, searchModels);
		queries.add(officialDiscoveryQuery(brand, searchModels.getFirst()));
		queries.add(officialSupportQuery(brand, searchModels.getFirst()));
		String preferredDomainExpression = domainExpression(preferredDomains);
		if (!preferredDomainExpression.isBlank()) {
			queries.add(preferredDomainExpression + " " + modelExpression);
		}
		List<String> userIntentModels = userIntentModels(product, primaryModel, alternateModels).stream()
				.limit(4)
				.toList();
		for (String model : userIntentModels) {
			queries.add(reviewIntentQuery(brand, model));
			queries.add(supportIntentQuery(brand, model));
		}
		List<String> injectSites = verticalConfig == null ? List.of() : verticalConfig.getInjectSitesResults();
		if (injectSites != null && !injectSites.isEmpty()) {
			for (String site : injectSites) {
				if (site != null && !site.isBlank()) {
					queries.add("site:" + site.trim() + " " + formatQuery(brand, orderedModels.getFirst()));
				}
			}
		}

		for (String model : searchModels) {
			queries.add(formatQuery(brand, model));
		}
		return queries.stream().distinct().toList();
	}

	private List<String> userIntentModels(Product product, String primaryModel, Set<String> alternateModels) {
		List<String> candidates = new ArrayList<>();
		if (primaryModel != null && ProductModelCandidateHelper.isHumanSearchCandidate(primaryModel)) {
			candidates.add(primaryModel);
		}
		if (alternateModels != null) {
			alternateModels.stream()
					.filter(ProductModelCandidateHelper::isHumanSearchCandidate)
					.forEach(candidates::add);
		}
		if (product != null && product.shortestOfferName() != null
				&& ProductModelCandidateHelper.isHumanSearchCandidate(product.shortestOfferName())) {
			candidates.add(product.shortestOfferName());
		}
		return candidates.stream()
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.filter(value -> value.length() <= 96)
				.collect(Collectors.toMap(value -> value.toLowerCase(Locale.ROOT), Function.identity(),
						(left, right) -> left, LinkedHashMap::new))
				.values().stream()
				.sorted(Comparator.comparingInt((String model) -> modelCandidateScore(product, primaryModel, model))
						.reversed().thenComparingInt(String::length))
				.toList();
	}

	/**
	 * Builds GTIN-based fallback search queries for products where standard brand+model
	 * queries produced no SERP results. The GTIN is a reliable universal product identifier
	 * that surfaces distributor and manufacturer pages even when the model name is obscure
	 * or contains non-standard characters.
	 *
	 * @param product the product
	 * @param brand   the resolved brand name
	 * @return ordered list of fallback queries, empty when GTIN is unavailable or non-numeric
	 */
	private List<String> buildGtinFallbackQueries(Product product, String brand) {
		List<String> queries = new ArrayList<>();
		String gtin = product.gtin();
		if (gtin == null || !gtin.matches("\\d{8,14}")) {
			return queries;
		}
		// Pure GTIN: finds any site that indexed this barcode
		queries.add("\"" + gtin + "\"");
		// Brand + GTIN: narrows to brand-related pages
		if (brand != null && !brand.isBlank()) {
			queries.add(brand + " \"" + gtin + "\"");
		}
		// Cleaned model (brand-name tokens stripped) + brand: helps when the model string
		// contains the brand as a suffix (e.g. "MCF8604GR_ATOSA" → "MCF8604GR")
		String primaryModel = product.model();
		if (primaryModel != null && !primaryModel.isBlank() && brand != null && !brand.isBlank()) {
			String cleaned = stripBrandTokensFromModel(primaryModel, brand);
			if (!cleaned.equalsIgnoreCase(primaryModel) && !cleaned.isBlank()) {
				queries.add(brand + " \"" + cleaned + "\"");
			}
		}
		return queries.stream().distinct().toList();
	}

	/**
	 * Removes tokens from a model name that exactly match any token of the brand name
	 * (case-insensitive). Handles suffixes like "MCF8604GR_ATOSA" → "MCF8604GR" when
	 * brand is "ATOSA".
	 *
	 * @param model the raw model string
	 * @param brand the brand name
	 * @return model with brand tokens stripped, or the original model when nothing changes
	 */
	private String stripBrandTokensFromModel(String model, String brand) {
		if (model == null || brand == null || brand.isBlank()) {
			return model == null ? "" : model;
		}
		Set<String> brandTokens = Arrays.stream(brand.split("\\s+"))
				.map(t -> t.toLowerCase(Locale.ROOT))
				.filter(t -> !t.isBlank())
				.collect(Collectors.toCollection(HashSet::new));
		String cleaned = Arrays.stream(model.split("[\\s_\\-\\./\\\\]+"))
				.filter(t -> !brandTokens.contains(t.toLowerCase(Locale.ROOT)))
				.collect(Collectors.joining(" "))
				.trim();
		return cleaned.isBlank() ? model : cleaned;
	}

	private List<String> orderedModels(String primaryModel, Set<String> alternateModels) {
		List<String> models = new ArrayList<>();
		if (primaryModel != null && !primaryModel.isBlank()) {
			models.add(primaryModel);
		}
		if (alternateModels != null) {
			models.addAll(alternateModels);
		}
		return models.stream()
				.filter(model -> model != null && !model.isBlank())
				.collect(Collectors.toMap(model -> model.trim().toLowerCase(Locale.ROOT), Function.identity(), (left, right) -> left,
						LinkedHashMap::new))
				.values().stream()
				.sorted(Comparator.comparingInt((String model) -> modelCandidateScore(null, primaryModel, model))
							.reversed().thenComparingInt(String::length))
				.toList();
	}

	private String trimToTokenLimit(String content, int tokenCount, int maxTokens, int minTokens) {
		if (content == null || content.isBlank() || tokenCount <= maxTokens) {
			return content;
		}
		double ratio = Math.max(0.1, (double) maxTokens / tokenCount);
		int targetLength = Math.max(properties.getMinMarkdownChars(), (int) (content.length() * ratio * 0.9));
		targetLength = Math.min(targetLength, content.length());
		String trimmed = content.substring(0, targetLength).trim();
		int lastParagraph = trimmed.lastIndexOf("\n\n");
		if (lastParagraph > properties.getMinMarkdownChars()) {
			trimmed = trimmed.substring(0, lastParagraph).trim();
		}
		if (trimmed.length() < properties.getMinMarkdownChars() && content.length() >= properties.getMinMarkdownChars()) {
			trimmed = content.substring(0, properties.getMinMarkdownChars()).trim();
		}
		return trimmed + "\n\n[Official source truncated to stay within the per-source token budget.]";
	}

	private List<String> rankedModelCandidates(Product product, String primaryModel, Set<String> alternateModels) {
		List<String> models = new ArrayList<>();
		if (primaryModel != null && !primaryModel.isBlank()) {
			models.add(primaryModel);
		}
		if (product != null && product.getExternalIds() != null) {
			if (product.getExternalIds().getMpn() != null) {
				models.addAll(product.getExternalIds().getMpn());
			}
			if (product.gtin() != null && product.gtin().matches("\\d{8,14}")) {
				models.add(product.gtin());
			}
		}
		if (alternateModels != null) {
			models.addAll(alternateModels);
		}
		if (product != null && product.shortestOfferName() != null) {
			models.addAll(extractModelCodeCandidates(product.shortestOfferName()));
		}
		return models.stream().filter(model -> model != null && !model.isBlank())
				.collect(Collectors.toMap(model -> model.trim().toLowerCase(Locale.ROOT), Function.identity(), (left, right) -> left,
						LinkedHashMap::new))
				.values().stream()
				.filter(model -> isSearchableModelCandidate(product, primaryModel, model))
				.sorted(Comparator.comparingInt((String model) -> modelCandidateScore(product, primaryModel, model))
						.reversed().thenComparingInt(String::length))
				.toList();
	}

	private List<String> extractModelCodeCandidates(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<String> candidates = URL_MODEL_TOKEN_PATTERN.matcher(value).results()
				.map(match -> match.group())
				.filter(candidate -> candidate != null && !candidate.isBlank())
				.toList();
		List<String> adjacentCandidates = Pattern.compile("(?i)\\b[A-Z]{2,8}\\s+\\d{3,5}\\b").matcher(value).results()
				.map(match -> match.group().trim())
				.toList();
		List<String> allCandidates = new ArrayList<>(candidates);
		allCandidates.addAll(adjacentCandidates);
		return allCandidates;
	}

	private boolean isSearchableModelCandidate(Product product, String primaryModel, String candidate) {
		String normalizedCandidate = normalizeForUrlMatching(candidate);
		if (normalizedCandidate.isBlank()) {
			return false;
		}
		if (candidate != null && candidate.matches("\\d+") && !candidate.matches("\\d{8,14}")) {
			return false;
		}
		String normalizedPrimary = normalizeForUrlMatching(primaryModel);
		if (!normalizedPrimary.isBlank() && normalizedCandidate.equals(normalizedPrimary)) {
			return true;
		}
		if (product != null && product.getExternalIds() != null && product.getExternalIds().getMpn() != null
				&& product.getExternalIds().getMpn().stream()
						.anyMatch(mpn -> normalizeForUrlMatching(mpn).equals(normalizedCandidate))) {
			return true;
		}
		if (product != null && product.gtin() != null && normalizeForUrlMatching(product.gtin()).equals(normalizedCandidate)
				&& candidate.matches("\\d{8,14}")) {
			return true;
		}
		if (looksLikeStorageVariant(candidate) || looksLikeMerchantTitle(candidate)) {
			return false;
		}
		if (normalizedPrimary.isBlank()) {
			return isConciseModelCode(candidate);
		}
		return normalizedPrimary.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedPrimary);
	}

	private int modelCandidateScore(Product product, String primaryModel, String candidate) {
		String normalizedCandidate = normalizeForUrlMatching(candidate);
		String normalizedPrimary = normalizeForUrlMatching(primaryModel);
		int score = 0;
		if (!normalizedPrimary.isBlank() && normalizedCandidate.equals(normalizedPrimary)) {
			score += 25;
		}
		if (product != null && product.getExternalIds() != null && product.getExternalIds().getMpn() != null
				&& product.getExternalIds().getMpn().stream()
						.anyMatch(mpn -> normalizeForUrlMatching(mpn).equals(normalizedCandidate))) {
			score += 80;
		}
		if (product != null && product.gtin() != null && normalizeForUrlMatching(product.gtin()).equals(normalizedCandidate)) {
			score += 20;
		}
		if (isConciseModelCode(candidate)) {
			score += 60;
		}
		if (ProductModelCandidateHelper.isPersistableModelCandidate(candidate)) {
			score += 30;
		}
		if (!normalizedPrimary.isBlank() && normalizedCandidate.contains(normalizedPrimary)
				&& normalizedCandidate.length() > normalizedPrimary.length()) {
			score += 10;
		}
		if (looksLikeStorageVariant(candidate)) {
			score -= 35;
		}
		if (looksLikeMerchantTitle(candidate)) {
			score -= 60;
		}
		score -= Math.max(0, candidate.length() - 32);
		return score;
	}

	private boolean isConciseModelCode(String value) {
		if (value == null) {
			return false;
		}
		String trimmed = value.trim();
		return trimmed.length() >= 4 && trimmed.length() <= 24 && trimmed.matches("(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9/_\\-.]+");
	}

	private boolean looksLikeStorageVariant(String value) {
		if (value == null) {
			return false;
		}
		return value.toUpperCase(Locale.ROOT).matches(".*\\b\\d+\\s*(GO|GB|TO|TB)(/\\d+\\s*(GO|GB|TO|TB))?\\b.*");
	}

	private boolean looksLikeMerchantTitle(String value) {
		if (value == null) {
			return false;
		}
		String lower = value.toLowerCase(Locale.ROOT);
		return value.length() > 48 || lower.contains("smartphone ") || lower.contains("refrigerateur ")
				|| lower.contains("réfrigérateur ") || lower.contains("lave-linge ") || lower.contains("congelateur ")
				|| lower.contains("congélateur ") || lower.contains(" reconditionne")
				|| lower.contains(" reconditionné");
	}

	private String modelExpression(String brand, List<String> orderedModels) {
		List<String> modelQueries = orderedModels.stream().map(model -> quoted(brand + " " + model)).toList();
		return "(" + String.join(" OR ", modelQueries) + ")";
	}

	private String officialDiscoveryQuery(String brand, String model) {
		return brand + " " + quoted(model) + " (official OR officiel OR product OR produit)";
	}

	private String officialSupportQuery(String brand, String model) {
		return brand + " " + quoted(model)
				+ " (support OR assistance OR manual OR notice OR datasheet OR \"fiche produit\")";
	}

	private String reviewIntentQuery(String brand, String model) {
		return brand + " " + quoted(model) + " (avis OR review OR test OR guide)";
	}

	private String supportIntentQuery(String brand, String model) {
		return brand + " " + quoted(model) + " (manual OR notice OR support OR datasheet OR \"fiche produit\")";
	}

	private String domainExpression(List<String> domains) {
		if (domains == null || domains.isEmpty()) {
			return "";
		}
		List<String> sites = domains.stream().filter(domain -> domain != null && !domain.isBlank())
				.map(domain -> "site:" + domain.trim()).toList();
		if (sites.isEmpty()) {
			return "";
		}
		return "(" + String.join(" OR ", sites) + ")";
	}

	private String formatQuery(String brand, String model) {
		return String.format(properties.getQueryTemplate(), brand, model);
	}

	private String quoted(String value) {
		return "\"" + value.replace("\"", "") + "\"";
	}

	private FetchOutcome fetchWithFallbacks(String url, Map<String, String> customHeaders) {
		Map<String, String> httpHeaders = new HashMap<>();
		if (customHeaders != null) {
			httpHeaders.putAll(customHeaders);
		}
		httpHeaders.put("X-Open4goods-Fetch-Mode", "http");
		FetchResponse response = fetchWithHeaders(url, httpHeaders, "HTTP_SIMPLE");
		if (isValidFetch(response)) {
			return new FetchOutcome(response, null);
		}
		String rejectionReason = invalidFetchReason(response);
		logger.info("HTTP_SIMPLE produced no usable content for {}; falling back to PLAYWRIGHT_HEADLESS.", url);

		Map<String, String> playwrightHeaders = new HashMap<>();
		if (customHeaders != null) {
			playwrightHeaders.putAll(customHeaders);
		}
		playwrightHeaders.put("X-Open4goods-Fetch-Mode", "playwright");
		response = fetchWithHeaders(url, playwrightHeaders, "PLAYWRIGHT_HEADLESS");
		if (isValidFetch(response)) {
			return new FetchOutcome(response, null);
		}
		rejectionReason = invalidFetchReason(response);
		logger.info("PLAYWRIGHT_HEADLESS produced no usable content for {}; replaying PLAYWRIGHT_HEADLESS with proxy.", url);

		Map<String, String> antiBotHeaders = new HashMap<>(playwrightHeaders);
		antiBotHeaders.put("X-Open4goods-Playwright-Proxy", "true");
		response = fetchWithHeaders(url, antiBotHeaders, "PLAYWRIGHT_PROXY");
		if (!isValidFetch(response)) {
			rejectionReason = invalidFetchReason(response);
			logger.warn("All fetch strategies failed for URL {}. Giving up on this source.", url);
			return new FetchOutcome(null, rejectionReason);
		}
		return new FetchOutcome(response, null);
	}

	private FetchResponse fetchWithHeaders(String url, Map<String, String> headers, String strategy) {
		try {
			logger.info("Fetching URL {} with requested review strategy {}", url, strategy);
			FetchResponse response = urlFetchingService.fetchUrlAsync(url, headers).get();
			if (response != null) {
				boolean valid = isValidFetch(response);
				meterRegistry
						.counter("review.fetch.attempts", "strategy", strategy, "outcome", valid ? "success" : "empty")
						.increment();
				logger.info(
						"Fetch completed for URL {}: requestedStrategy={}, actualStrategy={}, statusCode={}, markdownChars={}",
						url, strategy, response.fetchStrategy(), response.statusCode(),
						response.markdownContent() == null ? 0 : response.markdownContent().length());
			} else {
				meterRegistry.counter("review.fetch.attempts", "strategy", strategy, "outcome", "null").increment();
			}
			return response;
		} catch (Exception e) {
			meterRegistry.counter("review.fetch.attempts", "strategy", strategy, "outcome", "error").increment();
			logger.warn("Fetch strategy {} failed for {}: {}", strategy, url, e.getMessage());
			return null;
		}
	}

	private String sanitizeMarkdown(String markdown, String url) {
		if (markdown == null || markdown.isBlank()) {
			return markdown;
		}
		List<Pattern> patterns = properties.getMarkdownLineRemovalPatterns() == null ? List.of()
				: properties.getMarkdownLineRemovalPatterns().stream()
						.filter(pattern -> pattern != null && !pattern.isBlank()).map(Pattern::compile).toList();
		if (patterns.isEmpty()) {
			return markdown.trim();
		}
		String[] lines = markdown.split("\\R");
		List<String> kept = new ArrayList<>();
		int removed = 0;
		for (String line : lines) {
			boolean remove = patterns.stream().anyMatch(pattern -> pattern.matcher(line).find());
			if (remove) {
				removed++;
			} else {
				kept.add(line);
			}
		}
		String sanitized = String.join("\n", kept).trim();
		if (removed > 0) {
			logger.info("Markdown cleanup for URL {} removed {} likely header/footer/noise lines (chars {} -> {}).",
					url, removed, markdown.length(), sanitized.length());
		}
		return sanitized;
	}

	private boolean isValidFetch(FetchResponse response) {
		if (response == null || response.markdownContent() == null || response.markdownContent().isBlank()) {
			return false;
		}
		if (response.markdownContent().strip().length() < Math.max(0, properties.getMinMarkdownChars())) {
			return false;
		}
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			return false;
		}
		return !containsBlockedFetchContent(response.markdownContent())
				&& !containsBlockedFetchContent(response.htmlContent());
	}

	private String invalidFetchReason(FetchResponse response) {
		if (response == null) {
			return "fetch returned no response";
		}
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			return "fetch returned HTTP " + response.statusCode();
		}
		if (response.markdownContent() == null || response.markdownContent().isBlank()) {
			return "fetch returned empty markdown";
		}
		if (response.markdownContent().strip().length() < Math.max(0, properties.getMinMarkdownChars())) {
			return "fetch returned short markdown: " + response.markdownContent().strip().length() + " chars";
		}
		if (containsBlockedFetchContent(response.markdownContent()) || containsBlockedFetchContent(response.htmlContent())) {
			return "fetch returned anti-bot challenge content";
		}
		return "fetch rejected";
	}

	/**
	 * Detects anti-bot challenge pages that contain text but no usable product
	 * review content.
	 */
	private boolean containsBlockedFetchContent(String content) {
		if (content == null || content.isBlank()) {
			return false;
		}
		String normalized = content.toLowerCase(Locale.ROOT);
		return BLOCKED_FETCH_CONTENT_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(normalized).find());
	}

	private String detectLanguage(String markdown) {
		if (markdown == null || markdown.isBlank()) {
			return "unknown";
		}
		String lower = markdown.toLowerCase();
		if (lower.contains(" le ") || lower.contains(" la ") || lower.contains(" les ")) {
			return "fr";
		}
		if (lower.contains(" the ") || lower.contains(" and ")) {
			return "en";
		}
		return "unknown";
	}

	/**
	 * Returns the fetch strategy name for an already-completed future.
	 * {@code getNow(null)} is safe here because this method is only called after
	 * {@code future.get()} has already been awaited in the aggregation loop.
	 */
	private String resolveFetchStrategy(CompletableFuture<FetchOutcome> future) {
		if (future == null) {
			return "UNKNOWN";
		}
		try {
			FetchOutcome outcome = future.getNow(null);
			FetchResponse response = outcome == null ? null : outcome.response();
			if (response == null || response.fetchStrategy() == null) {
				return "UNKNOWN";
			}
			return response.fetchStrategy().name();
		} catch (Exception e) {
			return "UNKNOWN";
		}
	}

	private Optional<String> identifyOfficialProductUrl(Product product, List<GoogleSearchResult> results) {
		if (results == null || results.isEmpty()) {
			return Optional.empty();
		}
		return results.stream()
				.filter(result -> isOfficialUrl(product, result))
				.filter(result -> !isOfficialSupportUrl(result))
				.filter(result -> !isPdfUrl(result.link()))
				.max(Comparator.comparingInt(result -> officialUrlScore(product, result))).map(GoogleSearchResult::link);
	}

	private List<String> identifyOfficialSupportUrls(Product product, List<GoogleSearchResult> results) {
		if (results == null || results.isEmpty()) {
			return List.of();
		}
		return results.stream()
				.filter(result -> isOfficialUrl(product, result))
				.filter(this::isOfficialSupportUrl)
				.sorted(Comparator.comparingInt((GoogleSearchResult result) -> officialUrlScore(product, result)).reversed())
				.map(GoogleSearchResult::link)
				.filter(link -> link != null && !link.isBlank())
				.distinct()
				.toList();
	}

	private boolean isOfficialUrl(Product product, GoogleSearchResult result) {
		return officialUrlScore(product, result) >= 10;
	}

	private boolean isOfficialSupportUrl(GoogleSearchResult result) {
		if (result == null || result.link() == null || result.link().isBlank()) {
			return false;
		}
		String link = result.link().toLowerCase(Locale.ROOT);
		String title = result.title() == null ? "" : result.title().toLowerCase(Locale.ROOT);
		return link.contains("/support/") || link.contains("/assistance/") || link.contains("/help/")
				|| link.contains("/manual/") || link.contains("/manuals/") || link.contains("/ondersteuning/")
				|| link.contains("/product-ondersteuning/") || link.contains("/supporto/")
				|| link.contains("/soporte/") || link.contains("/supportdetail/")
				|| title.contains("support") || title.contains("assistance") || title.contains("ondersteuning")
				|| title.contains("mode d'emploi") || title.contains("manuel") || title.contains("manual");
	}

	private boolean isPdfUrl(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			String path = URI.create(url).getPath();
			return path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf");
		} catch (Exception e) {
			return url.toLowerCase(Locale.ROOT).contains(".pdf");
		}
	}

	private String resolveOfficialUrlLanguage(String url) {
		try {
			String path = URI.create(url).getPath();
			if (path != null) {
				for (String segment : path.split("/")) {
					if (segment.matches("[a-zA-Z]{2}([_-][a-zA-Z]{2})?")) {
						return segment.substring(0, 2).toLowerCase(Locale.ROOT);
					}
				}
			}
		} catch (Exception e) {
			logger.debug("Cannot resolve official URL language from {}: {}", url, e.getMessage());
		}
		String configuredLanguage = properties.getSearchHostLanguage();
		if (configuredLanguage != null && configuredLanguage.matches("[a-zA-Z]{2}([_-][a-zA-Z]{2})?")) {
			return configuredLanguage.substring(0, 2).toLowerCase(Locale.ROOT);
		}
		return "default";
	}

	private void persistOfficialResources(Product product, GoogleSearchResult result, FetchResponse fetchResponse) {
		String language = resolveOfficialUrlLanguage(result.link());
		if (isPdfUrl(result.link())) {
			addOfficialResource(product, result.link(), ResourceType.PDF, language, "serp", result.title());
		}
		if (fetchResponse == null) {
			return;
		}
		if (!isOfficialUrl(product, result)) {
			return;
		}
		if (fetchResponse.resources() == null || fetchResponse.resources().isEmpty()) {
			return;
		}
		for (ExtractedResource extractedResource : fetchResponse.resources()) {
			if (extractedResource == null || extractedResource.url() == null || extractedResource.url().isBlank()) {
				continue;
			}
			if (toProductResourceType(extractedResource.type()) == ResourceType.PDF
					&& !isProductRelevantResource(product, extractedResource.url(), extractedResource.label())) {
				logger.debug("Skipping unrelated official PDF for UPC {}: url={}, label={}", product.getId(),
						extractedResource.url(), extractedResource.label());
				continue;
			}
			addOfficialResource(product, extractedResource.url(), toProductResourceType(extractedResource.type()), language,
					extractedResource.source(), extractedResource.label());
		}
	}

	private boolean fetchOfficialEvidence(Product product, List<GoogleSearchResult> results, Map<String, String> customHeaders) {
		if (product == null || results == null || results.isEmpty()) {
			return false;
		}
		boolean modelPromoted = false;
		List<GoogleSearchResult> officialResults = results.stream()
				.filter(result -> result != null && result.link() != null && !result.link().isBlank())
				.filter(result -> isOfficialUrl(product, result))
				.filter(distinctByKey(GoogleSearchResult::link))
				.limit(Math.max(3, properties.getMaxUrlsPerProduct()))
				.toList();
		for (GoogleSearchResult result : officialResults) {
			String url = result.link();
			if (isPdfUrl(url)) {
				persistOfficialResources(product, result, null);
				continue;
			}
			try {
				FetchOutcome outcome = fetchWithFallbacks(url, customHeaders);
				FetchResponse response = outcome == null ? null : outcome.response();
				if (response == null) {
					logger.info("Official evidence fetch produced no response for UPC {}: url={}, reason={}",
							product.getId(), url, outcome == null ? "unknown" : outcome.rejectionReason());
					continue;
				}
				modelPromoted = promoteModelFromOfficialEvidence(product, result, response) || modelPromoted;
				persistOfficialResources(product, result, response);
			} catch (Exception e) {
				logger.warn("Official evidence fetch failed for UPC {}: url={}, reason={}", product.getId(), url,
						e.getMessage());
			}
		}
		return modelPromoted;
	}

	private boolean promoteModelFromOfficialEvidence(Product product, GoogleSearchResult result, FetchResponse response) {
		if (product == null || result == null || response == null || !isOfficialUrl(product, result)) {
			return false;
		}
		Map<String, Integer> scores = new LinkedHashMap<>();
		Map<String, List<String>> reasons = new LinkedHashMap<>();
		if (response.metadataAttributes() != null) {
			for (ExtractedMetadataAttribute attribute : response.metadataAttributes()) {
				if (!isModelMetadataAttribute(attribute)) {
					continue;
				}
				addModelEvidence(scores, reasons, attribute.value(), metadataModelScore(attribute),
						"metadata:" + attribute.name());
			}
		}

		String markdownZone = firstContentZone(response.markdownContent());
		if (!markdownZone.isBlank()) {
			OFFICIAL_MODEL_LINE_PATTERN.matcher(markdownZone).results()
					.map(match -> match.group(1))
					.forEach(candidate -> addModelEvidence(scores, reasons, candidate, 4, "official-text"));
		}

		String urlAndTitle = String.join(" ", safeString(response.url()), safeString(result.link()),
				safeString(result.title()));
		URL_MODEL_TOKEN_PATTERN.matcher(urlAndTitle).results()
				.map(match -> match.group())
				.forEach(candidate -> addModelEvidence(scores, reasons, candidate, 2, "official-url"));

		if (response.resources() != null) {
			for (ExtractedResource resource : response.resources()) {
				if (resource != null && resource.url() != null) {
					URL_MODEL_TOKEN_PATTERN.matcher(resource.url()).results()
							.map(match -> match.group())
							.forEach(candidate -> addModelEvidence(scores, reasons, candidate, 2, "official-resource"));
				}
			}
		}

		String combinedEvidence = String.join(" ", safeString(response.url()), safeString(result.link()),
				safeString(result.title()), markdownZone,
				response.resources() == null ? "" : response.resources().stream()
						.map(ExtractedResource::url)
						.filter(url -> url != null)
						.collect(Collectors.joining(" ")));
		for (String candidate : new ArrayList<>(scores.keySet())) {
			if (containsModelCandidate(combinedEvidence, candidate)) {
				addModelEvidence(scores, reasons, candidate, 2, "confirmed-in-page");
			}
		}
		if (response.extractedGtins() != null && product.gtin() != null
				&& response.extractedGtins().stream().anyMatch(gtin -> product.gtin().equals(gtin))) {
			for (String candidate : new ArrayList<>(scores.keySet())) {
				addModelEvidence(scores, reasons, candidate, 1, "matching-gtin");
			}
		}

		Optional<ModelEvidence> bestEvidence = scores.entrySet().stream()
				.map(entry -> new ModelEvidence(entry.getKey(), entry.getValue(),
						reasons.getOrDefault(entry.getKey(), List.of())))
				.filter(evidence -> evidence.score() >= OFFICIAL_MODEL_PROMOTION_THRESHOLD)
				.filter(evidence -> hasAuthoritativeOfficialModelEvidence(evidence.reasons()))
				.max(Comparator.comparingInt(ModelEvidence::score)
						.thenComparingInt(evidence -> evidence.candidate().length()));
		if (bestEvidence.isEmpty()) {
			return false;
		}
		boolean promoted = product.promoteModel(bestEvidence.get().candidate());
		if (promoted) {
			logger.info("Promoted official model for UPC {}: model={}, score={}, reasons={}", product.getId(),
					bestEvidence.get().candidate(), bestEvidence.get().score(), bestEvidence.get().reasons());
		}
		return promoted;
	}

	private boolean isModelMetadataAttribute(ExtractedMetadataAttribute attribute) {
		if (attribute == null || attribute.name() == null || attribute.value() == null || attribute.value().isBlank()) {
			return false;
		}
		String name = attribute.name().toLowerCase(Locale.ROOT);
		return name.equals("model") || name.equals("mpn") || name.equals("sku") || name.endsWith(":model")
				|| name.endsWith(":mpn") || name.endsWith(":sku") || name.contains("product:model")
				|| name.contains("product:mpn") || name.contains("product:sku");
	}

	private int metadataModelScore(ExtractedMetadataAttribute attribute) {
		String source = attribute.source() == null ? "" : attribute.source().toLowerCase(Locale.ROOT);
		String name = attribute.name() == null ? "" : attribute.name().toLowerCase(Locale.ROOT);
		int score = source.contains("jsonld") || source.contains("itemprop") ? 5 : 4;
		if (name.contains("model") || name.contains("mpn")) {
			score += 1;
		}
		return score;
	}

	private void addModelEvidence(Map<String, Integer> scores, Map<String, List<String>> reasons, String rawCandidate,
			int score, String reason) {
		String candidate = ProductModelCandidateHelper.cleanForStorage(rawCandidate);
		if (candidate == null) {
			return;
		}
		List<String> candidateReasons = reasons.computeIfAbsent(candidate, ignored -> new ArrayList<>());
		if (candidateReasons.contains(reason)) {
			return;
		}
		scores.merge(candidate, score, Integer::sum);
		candidateReasons.add(reason);
	}

	private boolean hasAuthoritativeOfficialModelEvidence(List<String> reasons) {
		if (reasons == null || reasons.isEmpty()) {
			return false;
		}
		return reasons.stream().anyMatch(reason -> reason != null
				&& (reason.startsWith("metadata:") || "official-text".equals(reason)));
	}

	private boolean containsModelCandidate(String haystack, String candidate) {
		if (haystack == null || haystack.isBlank() || candidate == null || candidate.isBlank()) {
			return false;
		}
		return normalizeForUrlMatching(haystack).contains(normalizeForUrlMatching(candidate))
				|| modelMatchesZone(candidate, normalizeForTextMatching(haystack));
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private boolean isProductRelevantResource(Product product, String url, String label) {
		if (product == null || url == null || url.isBlank()) {
			return false;
		}
		String haystack = normalizeForUrlMatching(url);
		if (haystack.isBlank()) {
			return false;
		}
		String gtin = product.gtin();
		if (gtin != null && gtin.matches("\\d{8,14}") && haystack.contains(normalizeForUrlMatching(gtin))) {
			return true;
		}
		String model = normalizeForUrlMatching(product.model());
		if (!model.isBlank() && haystack.contains(model)) {
			return true;
		}
		return product.getAkaModels() != null && product.getAkaModels().stream()
				.filter(candidate -> candidate != null && !candidate.isBlank())
				.map(this::normalizeForUrlMatching)
				.anyMatch(candidate -> candidate.length() >= 4 && haystack.contains(candidate));
	}

	private ResourceType toProductResourceType(org.open4goods.services.urlfetching.dto.ResourceType resourceType) {
		if (resourceType == null) {
			return ResourceType.UNKNOWN;
		}
		return switch (resourceType) {
		case PDF -> ResourceType.PDF;
		case VIDEO -> ResourceType.VIDEO;
		case IMAGE -> ResourceType.IMAGE;
		};
	}

	private void addOfficialResource(Product product, String url, ResourceType resourceType, String language, String source,
			String label) {
		try {
			Resource resource = new Resource(url);
			resource.setResourceType(resourceType == null ? ResourceType.UNKNOWN : resourceType);
			resource.setDatasourceName("manufacturer");
			resource.getTags().add("official");
			resource.getTags().add("official:" + language);
			if (source != null && !source.isBlank()) {
				resource.getTags().add("source:" + source);
			}
			if (label != null && !label.isBlank()) {
				resource.getTags().add("label:" + label);
			}
			product.addResource(resource);
		} catch (Exception e) {
			logger.warn("Cannot persist official resource for UPC {}: url={}, reason={}", product.getId(), url,
					e.getMessage());
		}
	}

	private int officialUrlScore(Product product, GoogleSearchResult result) {
		if (product == null || result == null || result.link() == null || result.link().isBlank()) {
			return 0;
		}
		String brand = normalizeForUrlMatching(product.brand());
		List<String> modelCandidates = rankedModelCandidates(product, product.model(), product.getAkaModels());
		if (brand.isBlank() || modelCandidates.isEmpty()) {
			return 0;
		}
		try {
			URL url = URI.create(result.link()).toURL();
			String host = normalizeForUrlMatching(url.getHost());
			String path = normalizeForUrlMatching(url.getPath());
			String title = normalizeForUrlMatching(result.title());
			String pathAndTitleText = normalizeForTextMatching(url.getPath() + " " + safeString(result.title()));
			if (isExcludedOfficialHost(host)) {
				return 0;
			}
			int score = 0;
			// Split compound brands (e.g. "LG Electronics") and check each word
			// individually so "lg.com" matches even though "lgelectronics" does not.
			boolean brandInHost = java.util.Arrays.stream(product.brand().split("\\s+"))
					.map(this::normalizeForUrlMatching)
					.filter(w -> w.length() >= 2)
					.anyMatch(host::contains);
			if (brandInHost) {
				score += 8;
			}
			for (String modelCandidate : modelCandidates) {
				String model = normalizeForUrlMatching(modelCandidate);
				if (!model.isBlank() && path.contains(model)) {
					score += 5;
					break;
				}
				if (modelMatchesZone(modelCandidate, pathAndTitleText)) {
					score += 5;
					break;
				}
			}
			for (String modelCandidate : modelCandidates) {
				String model = normalizeForUrlMatching(modelCandidate);
				if (!model.isBlank() && title.contains(model)) {
					score += 2;
					break;
				}
				if (modelMatchesZone(modelCandidate, normalizeForTextMatching(result.title()))) {
					score += 2;
					break;
				}
			}
			if (path.contains("product") || path.contains("produit") || path.contains("lave") || path.contains("linge")) {
				score += 1;
			}
			return score;
		} catch (Exception e) {
			return 0;
		}
	}

	private boolean isExcludedOfficialHost(String host) {
		if (host == null || host.isBlank()) {
			return true;
		}
		List<String> excludedDomains = properties.getOfficialUrlExcludedDomains() == null ? List.of()
				: properties.getOfficialUrlExcludedDomains();
		return excludedDomains.stream().filter(domain -> domain != null && !domain.isBlank())
				.map(this::normalizeForUrlMatching).anyMatch(host::contains);
	}

	private String normalizeForUrlMatching(String value) {
		if (value == null) {
			return "";
		}
		return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]", "");
	}

	private String normalizeForTextMatching(String value) {
		if (value == null) {
			return "";
		}
		return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", " ")
				.trim();
	}

	private boolean isStrongModelToken(String token) {
		if (token == null || token.isBlank() || token.length() < 4) {
			return false;
		}
		boolean hasLetter = false;
		boolean hasDigit = false;
		for (int i = 0; i < token.length(); i++) {
			char c = token.charAt(i);
			if (Character.isLetter(c)) {
				hasLetter = true;
			}
			if (Character.isDigit(c)) {
				hasDigit = true;
			}
		}
		return (hasLetter && hasDigit) || (hasDigit && token.length() >= 5);
	}

	private String sha256(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (Exception e) {
			return Integer.toHexString(content.hashCode());
		}
	}

	public Map<String, Object> buildBasePromptVariables(Product product, VerticalConfig verticalConfig) {
		Map<String, Object> promptVariables = new HashMap<>();
		promptVariables.put("PRODUCT_NAME", product.shortestOfferName());
		promptVariables.put("PRODUCT_BRAND", product.brand());
		promptVariables.put("PRODUCT_MODEL", product.model());
		promptVariables.put("PRODUCT_GTIN", product.gtin());
		promptVariables.put("PRODUCT", product);
		promptVariables.put("VERTICAL_NAME", verticalConfig.i18n("fr").getH1Title().getPrefix());

		String attributesList = verticalConfig.getAttributesConfig().getConfigs().stream()
				.filter(attrConf -> !attrConf.getSynonyms().isEmpty())
				.map(attrConf -> String.format("        - %s (%s)", attrConf.getKey(), attrConf.getName().get("fr")))
				.collect(Collectors.joining("\n"));
		// Initialize standard variables to avoid NPE in template and service.
		promptVariables.put("sources", new HashMap<>());
		promptVariables.put("tokens", new HashMap<>());
		promptVariables.put("TOTAL_TOKENS", 0);
		promptVariables.put("SOURCE_TOKENS", new HashMap<>());

		promptVariables.put("ATTRIBUTES", attributesList);
		// Default empty value so templates that reference EXTRACTED_ATTRIBUTES don't
		// fail.
		promptVariables.put("EXTRACTED_ATTRIBUTES", "[]");

		// Inject IMPACTSCORE_POSITION
		String impactScorePosition = "Non classé";
		if (product.getRanking() != null && product.getRanking().getGlobalPosition() > 0
				&& product.getRanking().getGlobalCount() > 0) {
			impactScorePosition = String.format("Ce produit se classe %dème sur %d produits de la catégorie %s",
					product.getRanking().getGlobalPosition(), product.getRanking().getGlobalCount(),
					verticalConfig.i18n("fr").getH1Title().getPrefix());
		}
		promptVariables.put("IMPACTSCORE_POSITION", impactScorePosition);

		// Inject COMMON_ATTRIBUTES
		List<String> commonAttributes = verticalConfig.getCommonAttributes();
		String commonAttributesStr = "";
		if (commonAttributes != null && !commonAttributes.isEmpty()) {
			commonAttributesStr = String.join("\n", commonAttributes);
		}
		promptVariables.put("COMMON_ATTRIBUTES", commonAttributesStr);

		// Inject OFFER_NAMES
		String offerNamesStr = "";
		if (product.getOfferNames() != null && !product.getOfferNames().isEmpty()) {
			offerNamesStr = String.join("\n", product.getOfferNames());
		}
		promptVariables.put("OFFER_NAMES", offerNamesStr);

		// Inject Ecoscore and Scores as JSON
		try {
			promptVariables.put("PRODUCT_ECOSCORE_JSON", serialisationService.toJson(product.ecoscore()));

			List<String> criteriaKeys = verticalConfig.getAvailableImpactScoreCriterias();
			Map<String, org.open4goods.model.product.Score> filteredScores = product.getScores().entrySet().stream()
					.filter(entry -> criteriaKeys.contains(entry.getKey()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			promptVariables.put("PRODUCT_SCORES_JSON", serialisationService.toJson(filteredScores));
		} catch (Exception e) {
			logger.error("Error serializing product scores for UPC {}: {}", product.getId(), e.getMessage());
			promptVariables.put("PRODUCT_ECOSCORE_JSON", "{}");
			promptVariables.put("PRODUCT_SCORES_JSON", "{}");
		}

		return promptVariables;
	}

	/**
	 * Builds prompt variables from markdown facts already persisted on the product.
	 *
	 * @param product        the product carrying {@link ProductFact} review facts
	 * @param verticalConfig the vertical configuration
	 * @param requireFacts   whether to fail when no usable facts exist
	 * @return prompt variables compatible with review-generation prompts
	 * @throws NotEnoughDataException when facts are required but absent
	 */
	public Map<String, Object> buildPromptVariablesFromReviewFacts(Product product, VerticalConfig verticalConfig,
			boolean requireFacts) throws NotEnoughDataException {
		Map<String, Object> promptVariables = buildBasePromptVariables(product, verticalConfig);
		Map<String, String> sources = new LinkedHashMap<>();
		Map<String, Integer> tokens = new LinkedHashMap<>();
		int totalTokens = 0;

		if (product.getReviewFacts() != null) {
			for (ProductFact fact : product.getReviewFacts()) {
				if (fact == null || fact.getUrl() == null || fact.getUrl().isBlank() || fact.getMarkdown() == null
						|| fact.getMarkdown().isBlank()) {
					continue;
				}
				sources.put(fact.getUrl(), fact.getMarkdown());
				int tokenCount = fact.getTokenCount() == null ? genAiService.estimateTokens(fact.getMarkdown())
						: fact.getTokenCount();
				tokens.put(fact.getUrl(), tokenCount);
				totalTokens += tokenCount;
			}
		}

		if (requireFacts && sources.isEmpty()) {
			throw new NotEnoughDataException("No persisted review facts available for UPC " + product.getId()
					+ ". Run the remote fetching stage first.");
		}

		promptVariables.put("sources", sources);
		promptVariables.put("tokens", tokens);
		promptVariables.put("TOTAL_TOKENS", totalTokens);
		promptVariables.put("SOURCE_TOKENS", tokens);
		return promptVariables;
	}

	/**
	 * Returns a predicate to filter distinct elements based on a key extractor.
	 *
	 * @param keyExtractor a function to extract the key.
	 * @param <T>          the element type.
	 * @return a predicate that yields true only for the first occurrence.
	 */
	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}
}
