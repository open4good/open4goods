package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.open4goods.model.review.ReviewGenerationStatus;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.googlesearch.dto.GoogleSearchRequest;
import org.open4goods.services.googlesearch.dto.GoogleSearchResponse;
import org.open4goods.services.googlesearch.dto.GoogleSearchResult;
import org.open4goods.services.googlesearch.exception.GoogleSearchException;
import org.open4goods.services.googlesearch.service.GoogleSearchService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Builds prompt variables by discovering and fetching web sources for a product.
 * <p>
 * The fetch workflow applies a deterministic fallback chain:
 * HTTP simple, then Playwright local rendering, then ZenRows anti-bot provider.
 * </p>
 */
@Service
public class ReviewGenerationPreprocessingService {

	private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationPreprocessingService.class);

	private final ReviewGenerationConfig properties;
	private final GoogleSearchService googleSearchService;
	private final UrlFetchingService urlFetchingService;
	private final PromptService genAiService;
	private final SerialisationService serialisationService;
	private final MeterRegistry meterRegistry;
	private final ThreadPoolExecutor fetchExecutor;

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
				"Review generation retrieval configured: maxSearch={}, resultsPerQuery={}, preferredDomains={}, lr={}, cr={}, gl={}, hl={}, safe={}",
				properties.getMaxSearch(), properties.getSearchResultsPerQuery(), properties.getPreferredDomains(),
				properties.getSearchLanguageRestrict(), properties.getSearchCountryRestrict(),
				properties.getSearchGeoLocation(), properties.getSearchHostLanguage(), properties.getSearchSafe());
	}

    /**
     * Validates configuration at startup and logs actionable warnings for common misconfigurations.
     */
    @PostConstruct
    public void validateConfig() {
        if (properties.getPreferredDomains() == null || properties.getPreferredDomains().isEmpty()) {
            logger.warn("review.generation.preferred-domains is empty. No preferred-domain SERP boost will be applied. "
                    + "Configure e.g. lesnumeriques.com, fnac.com to improve source quality.");
        }
        else {
            boolean hasInvalidDomain = properties.getPreferredDomains().stream()
                    .anyMatch(d -> d != null && (d.startsWith("http://") || d.startsWith("https://") || d.contains("/")));
            if (hasInvalidDomain) {
                logger.warn("review.generation.preferred-domains contains entries with scheme or path. "
                        + "Use bare hostnames only (e.g. 'lesnumeriques.com'), not full URLs.");
            }
        }
        if (properties.getMinUrlCount() > properties.getMaxUrlsPerProduct()) {
            logger.warn("review.generation.min-url-count ({}) exceeds max-urls-per-product ({}). "
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
			ReviewGenerationStatus status, Map<String, String> customHeaders) throws IOException, InterruptedException, ExecutionException,
			ResourceNotFoundException, SerialisationException, NotEnoughDataException, GoogleSearchException {
		String brand = product.brand();
		String primaryModel = product.model();
		Set<String> alternateModels = product.getAkaModels();
		validateSearchKeys(product, brand, primaryModel);

		// Build search queries.
		List<String> queries = buildSearchQueries(brand, primaryModel, alternateModels, verticalConfig);
		logger.info("SERP validation for UPC {}: brand='{}', model='{}', akaModels={}, preferredDomains={}, plannedQueries={}",
				product.getId(), brand, primaryModel, alternateModels == null ? 0 : alternateModels.size(),
				properties.getPreferredDomains(), queries.size());

		status.addMessage("Searching the web...");
		int searchesMade = 0;
		List<GoogleSearchResult> allResults = new ArrayList<>();
		int maxSearch = properties.getMaxSearch();
		for (String query : queries) {
			if (searchesMade >= maxSearch) {
				break;
			}

			logger.info("SERP query {}/{} for UPC {}: {}", searchesMade + 1, maxSearch, product.getId(), query);
			status.addMessage("Executing search query: " + query);
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

		status.setStatus(ReviewGenerationStatus.Status.FETCHING);

		// Sort and deduplicate results.
		List<GoogleSearchResult> sortedResults = allResults.stream()
				.filter(r -> r.link() != null && !r.link().isEmpty()).filter(r -> !r.link().endsWith(".pdf"))
				.filter(distinctByKey(r -> {
					try {
						return URI.create(r.link()).toURL().getHost();
					} catch (Exception e) {
						return r.link();
					}
				})).sorted((r1, r2) -> {
					boolean r1Preferred = properties.getPreferredDomains().stream()
							.anyMatch(domain -> r1.link().contains(domain));
					boolean r2Preferred = properties.getPreferredDomains().stream()
							.anyMatch(domain -> r2.link().contains(domain));
					if (r1Preferred && !r2Preferred)
						return -1;
					if (!r1Preferred && r2Preferred)
						return 1;
					return 0;
				}).toList();
		long preferredResultCount = sortedResults.stream()
				.filter(result -> properties.getPreferredDomains().stream().anyMatch(domain -> result.link().contains(domain)))
				.count();
		logger.info("SERP selection for UPC {}: eligibleResults={}, preferredResults={}, maxUrlsPerProduct={}",
				product.getId(), sortedResults.size(), preferredResultCount, properties.getMaxUrlsPerProduct());

		// Fetch URL contents concurrently.
		Map<String, CompletableFuture<FetchResponse>> fetchFutures = new HashMap<>();
		for (GoogleSearchResult result : sortedResults.stream().limit(properties.getMaxUrlsPerProduct()).toList()) {
			String url = result.link();
			CompletableFuture<FetchResponse> future = CompletableFuture.supplyAsync(() -> {
				try {
					return fetchWithFallbacks(url, customHeaders);
				} catch (Exception e) {
					logger.warn("Failed to fetch content from URL {}: {}", url, e.getMessage());
					return null;
				}
			}, fetchExecutor);
			fetchFutures.put(url, future);
		}

		status.setStatus(ReviewGenerationStatus.Status.ANALYSING);
		status.addMessage("Fetching URL content concurrently...");

		int maxTotalTokens = properties.getMaxTotalTokens();
		int minTokens = properties.getSourceMinTokens();
		int maxTokens = properties.getSourceMaxTokens();

		int accumulatedTokens = 0;
		Map<String, String> finalSourcesMap = new LinkedHashMap<>();
		Map<String, Integer> finalTokensMap = new LinkedHashMap<>();

		for (GoogleSearchResult result : sortedResults) {
			String url = result.link();
			CompletableFuture<FetchResponse> future = fetchFutures.get(url);
			if (future == null)
				continue;
			FetchResponse fetchResponse = future.get();
			if (fetchResponse == null || fetchResponse.markdownContent() == null
					|| fetchResponse.markdownContent().isEmpty()) {
				continue;
			}
			String content = sanitizeMarkdown(fetchResponse.markdownContent(), url);
			int tokenCount = genAiService.estimateTokens(content);
			if (tokenCount < minTokens) {
				logger.warn("Content from URL {} discarded due to insufficient tokens: {}", url, tokenCount);
				continue;
			}
			if (tokenCount > maxTokens) {
				logger.warn("Content from URL {} discarded, exceed tokens limit: {}", url, tokenCount);
				continue;
			}
			if (accumulatedTokens + tokenCount > maxTotalTokens) {
				logger.warn("Reached max tokens threshold. Current tokens: {}, URL tokens: {}, threshold: {}",
						accumulatedTokens, tokenCount, maxTotalTokens);
				break;
			}
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
		logger.info("Aggregated {} tokens from {} sources.", accumulatedTokens, finalSourcesMap.size());

		// Check for minimum required data.
		if (accumulatedTokens < properties.getMinGlobalTokens()
				|| finalSourcesMap.size() < properties.getMinUrlCount()) {
			throw new NotEnoughDataException("Insufficient data for review generation: accumulatedTokens="
					+ accumulatedTokens + ", sources=" + finalSourcesMap.size());
		}

		Map<String, Object> promptVariables = buildBasePromptVariables(product, verticalConfig);
		promptVariables.put("sources", finalSourcesMap);
		promptVariables.put("tokens", finalTokensMap);
		status.addMessage("AI generation");

		// Store aggregated tokens for convenience.
		promptVariables.put("TOTAL_TOKENS", accumulatedTokens);
		promptVariables.put("SOURCE_TOKENS", finalTokensMap);

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
			Map<String, ProductFact> merged = new LinkedHashMap<>();
			for (ProductFact fact : product.getReviewFacts()) {
				merged.put(fact.getUrl(), fact);
			}
			for (ProductFact fact : newFacts) {
				merged.put(fact.getUrl(), fact);
			}
			product.setReviewFacts(merged.values().stream().limit(properties.getFactsMaxStored()).toList());

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
		if (!missing.isEmpty()) {
			throw new IllegalStateException("Cannot build review SERP queries for UPC " + product.getId()
					+ ": missing " + String.join(", ", missing));
		}
	}

	private List<String> buildSearchQueries(String brand, String primaryModel, Set<String> alternateModels,
			VerticalConfig verticalConfig) {
		List<String> queries = new ArrayList<>();
		String modelExpression = modelExpression(brand, primaryModel, alternateModels);
		String preferredDomainExpression = domainExpression(properties.getPreferredDomains());
		if (!preferredDomainExpression.isBlank()) {
			queries.add(preferredDomainExpression + " " + modelExpression);
		}

		List<String> injectSites = verticalConfig.getInjectSitesResults();
		if (injectSites != null && !injectSites.isEmpty()) {
			for (String site : injectSites) {
				if (site != null && !site.isBlank()) {
					queries.add("site:" + site.trim() + " " + formatQuery(brand, primaryModel));
				}
			}
		}

		queries.add(formatQuery(brand, primaryModel));
		if (alternateModels != null) {
			for (String akaModel : alternateModels) {
				if (akaModel != null && !akaModel.isBlank()) {
					queries.add(formatQuery(brand, akaModel));
				}
			}
		}
		return queries.stream().distinct().toList();
	}

	private String modelExpression(String brand, String primaryModel, Set<String> alternateModels) {
		List<String> modelQueries = new ArrayList<>();
		modelQueries.add(quoted(brand + " " + primaryModel));
		if (alternateModels != null) {
			for (String akaModel : alternateModels) {
				if (akaModel != null && !akaModel.isBlank()) {
					modelQueries.add(quoted(brand + " " + akaModel));
				}
			}
		}
		return "(" + String.join(" OR ", modelQueries) + ")";
	}

	private String domainExpression(List<String> domains) {
		if (domains == null || domains.isEmpty()) {
			return "";
		}
		List<String> sites = domains.stream()
				.filter(domain -> domain != null && !domain.isBlank())
				.map(domain -> "site:" + domain.trim())
				.toList();
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

	private FetchResponse fetchWithFallbacks(String url, Map<String, String> customHeaders) {
		FetchResponse response = fetchWithHeaders(url, customHeaders, "HTTP_SIMPLE");
		if (isValidFetch(response)) {
			return response;
		}
		logger.info("HTTP_SIMPLE produced no usable content for {}; falling back to PLAYWRIGHT_HEADLESS.", url);

		Map<String, String> playwrightHeaders = new HashMap<>();
		if (customHeaders != null) {
			playwrightHeaders.putAll(customHeaders);
		}
		playwrightHeaders.put("X-Open4goods-Fetch-Mode", "playwright");
		response = fetchWithHeaders(url, playwrightHeaders, "PLAYWRIGHT_HEADLESS");
		if (isValidFetch(response)) {
			return response;
		}
		logger.info("PLAYWRIGHT_HEADLESS produced no usable content for {}; falling back to ZENROWS.", url);

		Map<String, String> antiBotHeaders = new HashMap<>(playwrightHeaders);
		antiBotHeaders.put("X-Open4goods-Fetch-Provider", "zenrows");
		response = fetchWithHeaders(url, antiBotHeaders, "ZENROWS");
		if (!isValidFetch(response)) {
			logger.warn("All fetch strategies failed for URL {}. Giving up on this source.", url);
		}
		return response;
	}

	private FetchResponse fetchWithHeaders(String url, Map<String, String> headers, String strategy) {
		try {
			logger.info("Fetching URL {} with requested review strategy {}", url, strategy);
			FetchResponse response = urlFetchingService.fetchUrlAsync(url, headers).get();
			if (response != null) {
				boolean valid = isValidFetch(response);
				meterRegistry.counter("review.fetch.attempts",
						"strategy", strategy,
						"outcome", valid ? "success" : "empty").increment();
				logger.info("Fetch completed for URL {}: requestedStrategy={}, actualStrategy={}, statusCode={}, markdownChars={}",
						url, strategy, response.fetchStrategy(), response.statusCode(),
						response.markdownContent() == null ? 0 : response.markdownContent().length());
			}
			else {
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
						.filter(pattern -> pattern != null && !pattern.isBlank())
						.map(Pattern::compile)
						.toList();
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
		return response != null && response.markdownContent() != null && !response.markdownContent().isBlank();
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
	private String resolveFetchStrategy(CompletableFuture<FetchResponse> future) {
		if (future == null) {
			return "UNKNOWN";
		}
		try {
			FetchResponse response = future.getNow(null);
			if (response == null || response.fetchStrategy() == null) {
				return "UNKNOWN";
			}
			return response.fetchStrategy().name();
		} catch (Exception e) {
			return "UNKNOWN";
		}
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
		// Default empty value so templates that reference EXTRACTED_ATTRIBUTES don't fail.
		promptVariables.put("EXTRACTED_ATTRIBUTES", "[]");

		// Inject IMPACTSCORE_POSITION
		String impactScorePosition = "Non classé";
		if (product.getRanking() != null && product.getRanking().getGlobalPosition() > 0 && product.getRanking().getGlobalCount() > 0) {
			impactScorePosition = String.format("Ce produit se classe %dème sur %d produits de la catégorie %s",
					product.getRanking().getGlobalPosition(), product.getRanking().getGlobalCount(), verticalConfig.i18n("fr").getH1Title().getPrefix());
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
