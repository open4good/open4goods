package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

		// Build search queries.
		List<String> queries = buildSearchQueries(brand, primaryModel, alternateModels, verticalConfig);
		logger.info(
				"SERP validation for UPC {}: brand='{}', model='{}', akaModels={}, preferredDomains={}, plannedQueries={}",
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
		List<GoogleSearchResult> templatedSourceResults = sourceUrlTemplateResults(product, brand, primaryModel,
				alternateModels);
		if (!templatedSourceResults.isEmpty()) {
			allResults.addAll(0, templatedSourceResults);
			logger.info("Added {} templated source URL candidates for UPC {}.", templatedSourceResults.size(),
					product.getId());
		}

		status.setStatus(ReviewGenerationStatus.Status.FETCHING);
		identifyOfficialUrl(product, allResults).ifPresent(officialUrl -> {
			product.setOfficialUrl(officialUrl);
			status.addMessage("Official manufacturer page identified: " + officialUrl);
			logger.info("Official manufacturer page identified for UPC {}: {}", product.getId(), officialUrl);
		});

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
					boolean r1Official = isOfficialUrl(product, r1);
					boolean r2Official = isOfficialUrl(product, r2);
					if (r1Official && !r2Official)
						return -1;
					if (!r1Official && r2Official)
						return 1;
					if (r1Preferred && !r2Preferred)
						return -1;
					if (!r1Preferred && r2Preferred)
						return 1;
					return 0;
				}).toList();
		long preferredResultCount = sortedResults.stream().filter(
				result -> properties.getPreferredDomains().stream().anyMatch(domain -> result.link().contains(domain)))
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
                    || fetchResponse.markdownContent().isEmpty())
            {
                continue;
            }
            String content = sanitizeMarkdown(fetchResponse.markdownContent(), url);
            if (!isRelevantContent(content, brand, primaryModel, alternateModels))
            {
                logger.warn("Content from URL {} discarded due to irrelevance: does not contain model/akaModels for brand {}", url, brand);
                continue;
            }
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
		product.setReviewFacts(newFacts.stream().limit(properties.getFactsMaxStored()).toList());

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
     * Checks if the fetched content is relevant to the product by ensuring it contains
     * the brand name and at least one model or akaModel identifier.
     *
     * @param content the fetched markdown content
     * @param brand the product brand
     * @param primaryModel the primary model name
     * @param alternateModels the set of alternate model aliases
     * @return true if the content is relevant, false otherwise
     */
    private boolean isRelevantContent(String content, String brand, String primaryModel, Set<String> alternateModels)
    {
        if (content == null || content.isBlank())
        {
            return false;
        }
        String lowerContent = content.toLowerCase();

        // 1. Brand check: must contain the brand name
        if (brand != null && !brand.isBlank())
        {
            if (!lowerContent.contains(brand.toLowerCase()))
            {
                return false;
            }
        }

        // 2. Model check: must contain at least one model or akaModel identifier
        List<String> modelsToCheck = orderedModels(primaryModel, alternateModels);
        if (modelsToCheck.isEmpty())
        {
            return true;
        }

        for (String model : modelsToCheck)
        {
            if (model == null || model.isBlank())
            {
                continue;
            }
            if (lowerContent.contains(model.toLowerCase()))
            {
                return true;
            }
        }

        // Fallback check on alphanumeric tokens of the primary model
        if (primaryModel != null && !primaryModel.isBlank())
        {
            String[] tokens = primaryModel.split("[\\s_\\-\\./\\\\]+");
            for (String token : tokens)
            {
                if (token.length() >= 4)
                {
                    boolean hasLetter = false;
                    boolean hasDigit = false;
                    for (int i = 0; i < token.length(); i++)
                    {
                        char c = token.charAt(i);
                        if (Character.isLetter(c))
                        {
                            hasLetter = true;
                        }
                        if (Character.isDigit(c))
                        {
                            hasDigit = true;
                        }
                    }
                    if ((hasLetter && hasDigit) || (hasDigit && token.length() >= 5))
                    {
                        if (lowerContent.contains(token.toLowerCase()))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

	private List<String> buildSearchQueries(String brand, String primaryModel, Set<String> alternateModels,
			VerticalConfig verticalConfig) {
		List<String> queries = new ArrayList<>();
		List<String> orderedModels = orderedModels(primaryModel, alternateModels);
		String modelExpression = modelExpression(brand, orderedModels);
		String officialDomainExpression = domainExpression(officialDomainsForBrand(brand));
		if (!officialDomainExpression.isBlank()) {
			queries.add(officialDomainExpression + " " + modelExpression);
		}
		queries.add(officialDiscoveryQuery(brand, orderedModels.getFirst()));
		String preferredDomainExpression = domainExpression(properties.getPreferredDomains());
		if (!preferredDomainExpression.isBlank()) {
			queries.add(preferredDomainExpression + " " + modelExpression);
		}
		List<String> injectSites = verticalConfig.getInjectSitesResults();
		if (injectSites != null && !injectSites.isEmpty()) {
			for (String site : injectSites) {
				if (site != null && !site.isBlank()) {
					queries.add("site:" + site.trim() + " " + formatQuery(brand, orderedModels.getFirst()));
				}
			}
		}

		for (String model : orderedModels) {
			queries.add(formatQuery(brand, model));
		}
		return queries.stream().distinct().toList();
	}

	private List<String> orderedModels(String primaryModel, Set<String> alternateModels) {
		List<String> models = new ArrayList<>();
		if (primaryModel != null && !primaryModel.isBlank()) {
			models.add(primaryModel);
		}
		if (alternateModels != null) {
			models.addAll(alternateModels.stream().filter(model -> model != null && !model.isBlank())
					.sorted(Comparator.comparingInt(String::length).reversed()).toList());
		}
		return models.stream().distinct().sorted((left, right) -> {
			boolean leftExtendsPrimary = primaryModel != null && left.length() > primaryModel.length()
					&& normalizeForUrlMatching(left).contains(normalizeForUrlMatching(primaryModel));
			boolean rightExtendsPrimary = primaryModel != null && right.length() > primaryModel.length()
					&& normalizeForUrlMatching(right).contains(normalizeForUrlMatching(primaryModel));
			if (leftExtendsPrimary && !rightExtendsPrimary)
				return -1;
			if (!leftExtendsPrimary && rightExtendsPrimary)
				return 1;
			return Integer.compare(right.length(), left.length());
		}).toList();
	}

	private String modelExpression(String brand, List<String> orderedModels) {
		List<String> modelQueries = orderedModels.stream().map(model -> quoted(brand + " " + model)).toList();
		return "(" + String.join(" OR ", modelQueries) + ")";
	}

	private List<String> officialDomainsForBrand(String brand) {
		if (brand == null || brand.isBlank() || properties.getOfficialDomainsByBrand() == null) {
			return List.of();
		}
		String normalizedBrand = normalizeForUrlMatching(brand);
		return properties.getOfficialDomainsByBrand().entrySet().stream()
				.filter(entry -> normalizeForUrlMatching(entry.getKey()).equals(normalizedBrand))
				.findFirst().map(Map.Entry::getValue).orElse(List.of());
	}

	private String officialDiscoveryQuery(String brand, String model) {
		return brand + " " + quoted(model) + " (official OR officiel OR product OR produit)";
	}

	private List<GoogleSearchResult> sourceUrlTemplateResults(Product product, String brand, String primaryModel,
			Set<String> alternateModels) {
		if (brand == null || brand.isBlank() || properties.getSourceUrlTemplatesByBrand() == null) {
			return List.of();
		}
		String normalizedBrand = normalizeForUrlMatching(brand);
		List<String> templates = properties.getSourceUrlTemplatesByBrand().entrySet().stream()
				.filter(entry -> normalizeForUrlMatching(entry.getKey()).equals(normalizedBrand))
				.findFirst().map(Map.Entry::getValue).orElse(List.of());
		if (templates.isEmpty()) {
			return List.of();
		}
		String model = orderedModels(primaryModel, alternateModels).getFirst();
		String productCode = productCode(product, alternateModels);
		return templates.stream().filter(template -> template != null && !template.isBlank())
				.map(template -> applyUrlTemplate(template, brand, model, productCode, product.gtin()))
				.filter(url -> url != null && !url.isBlank())
				.map(url -> new GoogleSearchResult("Templated product source " + brand + " " + model, url))
				.toList();
	}

	private String applyUrlTemplate(String template, String brand, String model, String productCode, String gtin) {
		if (template.contains("{PRODUCT_CODE}") && productCode.isBlank()) {
			return null;
		}
		return template.replace("{BRAND}", slug(brand))
				.replace("{BRAND_SLUG}", slug(brand))
				.replace("{MODEL}", model)
				.replace("{MODEL_SLUG}", slug(model))
				.replace("{MODEL_SLUG_UNDERSCORE}", slug(model).replace("-", "_"))
				.replace("{PRODUCT_CODE}", productCode)
				.replace("{GTIN}", gtin == null ? "" : gtin);
	}

	private String productCode(Product product, Set<String> alternateModels) {
		List<String> candidates = new ArrayList<>();
		if (alternateModels != null) {
			candidates.addAll(alternateModels);
		}
		if (product.gtin() != null) {
			candidates.add(product.gtin());
		}
		return candidates.stream().filter(candidate -> candidate != null && candidate.matches("\\d{6,12}"))
				.findFirst().orElse("");
	}

	private String slug(String value) {
		if (value == null) {
			return "";
		}
		return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-+|-+$)", "");
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

	private FetchResponse fetchWithFallbacks(String url, Map<String, String> customHeaders) {
		Map<String, String> httpHeaders = new HashMap<>();
		if (customHeaders != null) {
			httpHeaders.putAll(customHeaders);
		}
		httpHeaders.put("X-Open4goods-Fetch-Mode", "http");
		FetchResponse response = fetchWithHeaders(url, httpHeaders, "HTTP_SIMPLE");
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
		logger.info("PLAYWRIGHT_HEADLESS produced no usable content for {}; replaying PLAYWRIGHT_HEADLESS with proxy.", url);

		Map<String, String> antiBotHeaders = new HashMap<>(playwrightHeaders);
		antiBotHeaders.put("X-Open4goods-Playwright-Proxy", "true");
		response = fetchWithHeaders(url, antiBotHeaders, "PLAYWRIGHT_PROXY");
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

	private Optional<String> identifyOfficialUrl(Product product, List<GoogleSearchResult> results) {
		if (results == null || results.isEmpty()) {
			return Optional.empty();
		}
		return results.stream().filter(result -> isOfficialUrl(product, result))
				.max(Comparator.comparingInt(result -> officialUrlScore(product, result))).map(GoogleSearchResult::link);
	}

	private boolean isOfficialUrl(Product product, GoogleSearchResult result) {
		return officialUrlScore(product, result) >= 10;
	}

	private int officialUrlScore(Product product, GoogleSearchResult result) {
		if (product == null || result == null || result.link() == null || result.link().isBlank()) {
			return 0;
		}
		String brand = normalizeForUrlMatching(product.brand());
		String model = normalizeForUrlMatching(product.model());
		if (brand.isBlank() || model.isBlank()) {
			return 0;
		}
		try {
			URL url = URI.create(result.link()).toURL();
			String host = normalizeForUrlMatching(url.getHost());
			String path = normalizeForUrlMatching(url.getPath());
			if (isExcludedOfficialHost(host)) {
				return 0;
			}
			int score = 0;
			if (host.contains(brand)) {
				score += 8;
			}
			if (path.contains(model)) {
				score += 5;
			}
			if (normalizeForUrlMatching(result.title()).contains(model)) {
				score += 2;
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
