package org.open4goods.nudgerfrontapi.service.share;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.open4goods.nudgerfrontapi.config.properties.ShareResolutionProperties;
import org.open4goods.nudgerfrontapi.dto.product.ProductAggregatedPriceDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductBaseDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoresDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoreDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareCandidateDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareExtractionDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionRequestDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionResponseDto;
import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionStatus;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.nudgerfrontapi.service.SearchService;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchHit;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.nudgerfrontapi.service.exception.InvalidAffiliationTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orchestrates asynchronous share resolution while enforcing SLA constraints.
 */
@Service
public class ShareResolutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareResolutionService.class);

    private final ShareExtractionService extractionService;
    private final SearchService searchService;
    private final ProductMappingService productMappingService;
    private final ShareResolutionStore store;
    private final ShareResolutionProperties properties;
    private final Clock clock;
    private final Executor executor;

    public ShareResolutionService(ShareExtractionService extractionService, SearchService searchService,
            ProductMappingService productMappingService, ShareResolutionStore store,
            ShareResolutionProperties properties, Clock clock,
            @Qualifier("shareResolutionExecutor") Executor executor) {
        this.extractionService = extractionService;
        this.searchService = searchService;
        this.productMappingService = productMappingService;
        this.store = store;
        this.properties = properties;
        this.clock = clock;
        this.executor = executor;
    }

    /**
     * Accept a share resolution request and start asynchronous processing.
     *
     * @param request        incoming payload
     * @param domainLanguage localisation hint forwarded to downstream services
     * @return pending response containing the resolution token
     */
    public ShareResolutionResponseDto createResolution(ShareResolutionRequestDto request, DomainLanguage domainLanguage) {
        validateRequest(request);

        Instant startedAt = clock.instant();
        String token = UUID.randomUUID().toString();

        ShareResolutionResponseDto pending = new ShareResolutionResponseDto(token, ShareResolutionStatus.PENDING,
                request.url(), startedAt, null, null, List.of(), null);

        store.save(pending, startedAt.plus(properties.getStoreTtl()));

        executor.execute(() -> resolve(token, request, domainLanguage, startedAt));

        return pending;
    }

    /**
     * Retrieve an existing resolution snapshot.
     *
     * @param token unique token
     * @return optional snapshot
     */
    public Optional<ShareResolutionResponseDto> getResolution(String token) {
        return store.get(token);
    }

    /**
     * Execute share resolution asynchronously while respecting the SLA budget.
     *
     * @param token          resolution token
     * @param request        initial request payload
     * @param domainLanguage localisation hint
     * @param startedAt      creation timestamp
     */
    private void resolve(String token, ShareResolutionRequestDto request, DomainLanguage domainLanguage,
            Instant startedAt) {
        try {
            Optional<ShareExtractionDto> extraction = extractionService.extract(request.url(), request.title(),
                    request.text());

            if (extraction.isEmpty()) {
                updateAndStore(token, request.url(), ShareResolutionStatus.ERROR, startedAt, null, List.of(),
                        "No candidate could be extracted from the provided URL");
                return;
            }

            List<ShareCandidateDto> candidates = resolveCandidates(extraction.get(), domainLanguage);

            Instant now = clock.instant();
            ShareResolutionStatus status = now.isAfter(startedAt.plus(properties.getResolutionWindow()))
                    ? ShareResolutionStatus.TIMEOUT
                    : ShareResolutionStatus.RESOLVED;

            updateAndStore(token, request.url(), status, startedAt, extraction.get(), candidates,
                    status == ShareResolutionStatus.TIMEOUT ? "Resolution timed out" : null);
        } catch (Exception e) {
            LOGGER.error("Resolution failed for token {}: {}", token, e.getMessage(), e);
            updateAndStore(token, request.url(), ShareResolutionStatus.ERROR, startedAt, null, List.of(), e.getMessage());
        }
    }

    /**
     * Validate the share resolution payload and enforce size/scheme constraints.
     *
     * @param request incoming request
     */
    private void validateRequest(ShareResolutionRequestDto request) {
        if (request == null || !StringUtils.hasText(request.url())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The url parameter is required");
        }
        if (request.url().length() > properties.getMaxUrlLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shared URL is too long");
        }
        try {
            URI uri = new URI(request.url());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only HTTP/HTTPS URLs are accepted");
            }
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL");
        }

        if (StringUtils.hasText(request.text()) && request.text().length() > properties.getMaxTextLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shared text is too long");
        }
        if (StringUtils.hasText(request.title()) && request.title().length() > properties.getMaxTitleLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shared title is too long");
        }
    }

    /**
     * Resolve candidates either directly from a GTIN or using a search query.
     *
     * @param extraction     extraction payload
     * @param domainLanguage localisation hint
     * @return ordered list of candidates limited by configuration
     */
    private List<ShareCandidateDto> resolveCandidates(ShareExtractionDto extraction, DomainLanguage domainLanguage) {
        List<ShareCandidateDto> candidates = new ArrayList<>();

        if (StringUtils.hasText(extraction.gtin())) {
            mapProductByGtin(extraction.gtin(), domainLanguage).ifPresent(candidates::add);
        }

        if (candidates.isEmpty() && StringUtils.hasText(extraction.query())) {
            candidates.addAll(searchByQuery(extraction.query(), domainLanguage));
        }

        return candidates.stream()
                .limit(properties.getMaxCandidates())
                .toList();
    }

    /**
     * Map a GTIN to a candidate using the repository and mapping service.
     *
     * @param gtin           extracted GTIN
     * @param domainLanguage localisation hint
     * @return optional candidate
     */
    private Optional<ShareCandidateDto> mapProductByGtin(String gtin, DomainLanguage domainLanguage) {
        try {
            long numericGtin = Long.parseLong(gtin);
            ProductDto dto = productMappingService.getProduct(numericGtin,
                    Locale.forLanguageTag(domainLanguage.languageTag()), null, domainLanguage);
            return Optional.of(toCandidate(dto, null));
        } catch (InvalidAffiliationTokenException e) {
            LOGGER.warn("Invalid affiliation token while mapping GTIN {}: {}", gtin, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.warn("No product found for GTIN {}: {}", gtin, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Run a search query and convert the resulting hits into candidates.
     *
     * @param query          derived search query
     * @param domainLanguage localisation hint
     * @return ordered list of candidates
     */
    private List<ShareCandidateDto> searchByQuery(String query, DomainLanguage domainLanguage) {
        GlobalSearchResult searchResult = searchService.globalSearch(query, domainLanguage);
        List<ShareCandidateDto> mapped = new ArrayList<>();

        searchResult.verticalGroups().forEach(group -> group.results().stream()
                .sorted(Comparator.comparingDouble(GlobalSearchHit::score).reversed())
                .map(hit -> toCandidate(hit.product(), hit.score()))
                .forEach(mapped::add));

        searchResult.missingVerticalResults().stream()
                .sorted(Comparator.comparingDouble(GlobalSearchHit::score).reversed())
                .map(hit -> toCandidate(hit.product(), hit.score()))
                .forEach(mapped::add);

        return mapped;
    }

    /**
     * Convert a product DTO into a share candidate.
     *
     * @param product product DTO to convert
     * @param score   optional confidence score
     * @return populated candidate
     */
    private ShareCandidateDto toCandidate(ProductDto product, Double score) {
        ProductBaseDto base = product.base();
        ProductResourcesDto resources = product.resources();
        ProductOffersDto offers = product.offers();

        String image = base != null ? base.coverImagePath()
                : resources != null ? resources.coverImagePath() : null;
        ProductAggregatedPriceDto bestPrice = offers != null ? offers.bestPrice() : null;
        Double ecoScore = base != null ? base.ecoscoreValue() : null;
        Double impactScore = resolveImpactScore(product);

        String productId = product.slug() != null ? product.slug() : String.valueOf(product.gtin());
        String name = base != null ? base.bestName() : product.identity() != null ? product.identity().model() : productId;

        return new ShareCandidateDto(productId, name, image, ecoScore, impactScore, bestPrice, score);
    }

    /**
     * Extract the impact score when present in the score map.
     *
     * @param product product DTO containing scores
     * @return numeric impact value when available
     */
    private Double resolveImpactScore(ProductDto product) {
        if (product.scores() == null || product.scores().scores() == null) {
            return null;
        }
        ProductScoreDto score = product.scores().scores().get("IMPACT");
        if (score != null) {
            return score.value();
        }
        return null;
    }

    /**
     * Persist the latest resolution snapshot using the store abstraction.
     *
     * @param token       resolution token
     * @param originUrl   original URL provided by the caller
     * @param status      new status
     * @param startedAt   creation timestamp
     * @param extraction  extraction payload
     * @param candidates  resolved candidates
     * @param message     optional diagnostic message
     */
    private void updateAndStore(String token, String originUrl, ShareResolutionStatus status, Instant startedAt,
            ShareExtractionDto extraction, List<ShareCandidateDto> candidates, String message) {
        Instant resolvedAt = clock.instant();
        ShareResolutionResponseDto response = new ShareResolutionResponseDto(token, status, originUrl, startedAt, resolvedAt,
                extraction, candidates, message);
        store.save(response, resolvedAt.plus(properties.getStoreTtl()));
    }
}
