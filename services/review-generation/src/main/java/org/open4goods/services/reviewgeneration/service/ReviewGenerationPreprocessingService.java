package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.net.URL;
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
import java.util.stream.Collectors;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
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
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.UrlFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReviewGenerationPreprocessingService {

	private static final Logger logger = LoggerFactory.getLogger(ReviewGenerationPreprocessingService.class);

	private final ReviewGenerationConfig properties;
	private final GoogleSearchService googleSearchService;
	private final UrlFetchingService urlFetchingService;
	private final PromptService genAiService;
	private final ThreadPoolExecutor fetchExecutor;

	public ReviewGenerationPreprocessingService(ReviewGenerationConfig properties,
			GoogleSearchService googleSearchService, UrlFetchingService urlFetchingService, PromptService genAiService) {
		this.properties = properties;
		this.googleSearchService = googleSearchService;
		this.urlFetchingService = urlFetchingService;
		this.genAiService = genAiService;
		this.fetchExecutor = new ThreadPoolExecutor(properties.getMaxConcurrentFetch(), properties.getMaxQueueSize(),
				0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(properties.getMaxQueueSize() * 10),
				new ThreadPoolExecutor.AbortPolicy());
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
		String brand = product.brand();
		String primaryModel = product.model();
		Set<String> alternateModels = product.getAkaModels();

		// Build search queries.
		List<String> queries = new ArrayList<>();
		queries.add(String.format(properties.getQueryTemplate(), brand, primaryModel));
		if (alternateModels != null) {
			for (String akaModel : alternateModels) {
				queries.add(String.format(properties.getQueryTemplate(), brand, akaModel));
			}
		}

		status.addMessage("Searching the web...");
		int searchesMade = 0;
		List<GoogleSearchResult> allResults = new ArrayList<>();
		int maxSearch = properties.getMaxSearch();
		for (String query : queries) {
			if (searchesMade >= maxSearch) {
				break;
			}
			logger.debug("Executing search query: {}", query);
			status.addMessage("Executing search query: " + query);
			GoogleSearchRequest searchRequest = new GoogleSearchRequest(query, "lang_fr", "countryFR");
			GoogleSearchResponse searchResponse = googleSearchService.search(searchRequest);
			searchesMade++;
			if (searchResponse != null && searchResponse.results() != null) {
				allResults.addAll(searchResponse.results());
			}
		}

		status.setStatus(ReviewGenerationStatus.Status.FETCHING);

		// Sort and deduplicate results.
		List<GoogleSearchResult> sortedResults = allResults.stream()
				.filter(r -> r.link() != null && !r.link().isEmpty()).filter(r -> !r.link().endsWith(".pdf"))
				.filter(distinctByKey(r -> {
					try {
						return new URL(r.link()).getHost();
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

		// Fetch URL contents concurrently.
		Map<String, CompletableFuture<FetchResponse>> fetchFutures = new HashMap<>();
		for (GoogleSearchResult result : sortedResults) {
			String url = result.link();
			CompletableFuture<FetchResponse> future = CompletableFuture.supplyAsync(() -> {
				try {
					return urlFetchingService.fetchUrlAsync(url).get();
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
			String content = fetchResponse.markdownContent();
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
			try {
				String domain = new URL(url).getHost();
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

		return promptVariables;
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
        // Initialize standard variables to avoid NPE in template and service
        promptVariables.put("sources", new HashMap<>());
        promptVariables.put("tokens", new HashMap<>());
        promptVariables.put("TOTAL_TOKENS", 0);
        promptVariables.put("SOURCE_TOKENS", new HashMap<>());

		promptVariables.put("ATTRIBUTES", attributesList);
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
