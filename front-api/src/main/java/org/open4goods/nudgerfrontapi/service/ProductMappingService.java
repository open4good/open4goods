package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.open4goods.icecat.model.AttributesFeatureGroups;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.Localisable;
import org.open4goods.model.ai.AiDescriptions;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.ExternalIds;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.model.resource.PdfInfo;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.vertical.ImpactScoreCriteria;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.PageMetaDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAggregatedPriceDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiDescriptionDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiTextsDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAttributesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductBaseDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardinalityDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductClassifiedAttributeGroupDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDatasourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductExternalIdsDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductGtinInfoDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductImageDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductIdentityDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductIndexedAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductNamesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductPriceHistoryDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductPriceHistoryEntryDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductPdfDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductRankingDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoreDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoresDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductSourcedAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductVideoDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ibm.icu.util.ULocale;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps {@link Product} domain entities to DTOs consumed by the frontend API.
 * <p>
 * The service centralises every projection required by the Nuxt application: conditional component
 * inclusion, localisation of textual content and enrichment of affiliation links. By keeping the
 * conversion logic here we ensure controllers stay declarative and consistent.
 * </p>
 */
@Service
public class ProductMappingService {

    private static final Logger logger = LoggerFactory.getLogger(ProductMappingService.class);
    private static final String DEFAULT_LANGUAGE_KEY = "default";
    private static final String IMAGES_PATH = "/images/";
    private static final String PDFS_PATH = "/pdfs/";
    private static final String VIDEOS_PATH = "/videos/";
	private static final String IMAGE_WEBP_MEDIATYPE = "image/webp";

    private final ProductRepository repository;
    private final ApiProperties apiProperties;
    private final CategoryMappingService categoryMappingService;
    private final VerticalsConfigService verticalsConfigService;
    private final SearchService searchService;
    private final AffiliationService affiliationService;
    private final IcecatService icecatService;


    public ProductMappingService(ProductRepository repository,
            ApiProperties apiProperties,
            CategoryMappingService categoryMappingService,
            VerticalsConfigService verticalsConfigService,
            SearchService searchService,
            AffiliationService affiliationService,
            IcecatService icecatService) {
        this.repository = repository;
        this.apiProperties = apiProperties;
        this.categoryMappingService = categoryMappingService;
        this.verticalsConfigService = verticalsConfigService;
        this.searchService = searchService;
        this.affiliationService = affiliationService;
        this.icecatService = icecatService;
    }

    /**
     * Retrieve a product from the repository and project it to the frontend DTO.
     *
     * @param gtin           GTIN identifier used to lookup the product
     * @param locale         locale requested by the caller
     * @param includes       optional set of DTO components to include
     * @param domainLanguage domain language requested by the caller
     * @return populated DTO ready to be serialised
     * @throws ResourceNotFoundException when the product does not exist
     */
    public ProductDto getProduct(long gtin, Locale locale, Set<String> includes, DomainLanguage domainLanguage)
            throws ResourceNotFoundException {
        Product product = repository.getById(gtin);
        return mapProduct(product, locale, includes, domainLanguage);
    }

    /**
     * Map the product domain model to the DTO expected by the frontend while applying localisation rules.
     *
     * @param product        product retrieved from the repository
     * @param locale         locale requested by the caller
     * @param includes       requested components (defaults to all when {@code null})
     * @param domainLanguage domain language requested by the caller
     * @return DTO representing the product or {@code null} when no vertical configuration matches
     */
    public ProductDto mapProduct(Product product, Locale locale, Set<String> includes, DomainLanguage domainLanguage) {

        VerticalConfig vConfig = verticalsConfigService.getConfigById(product.getVertical());
        if (vConfig == null) {
            vConfig = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        }
        EnumSet<ProductDtoComponent> components = resolveComponents(includes);

        ProductBaseDto base = components.contains(ProductDtoComponent.base) ? mapBase(product, domainLanguage, locale) : null;
        ProductIdentityDto identity = components.contains(ProductDtoComponent.identity) ? mapIdentity(product) : null;
        ProductNamesDto names = components.contains(ProductDtoComponent.names) ? mapNames(product, domainLanguage, locale) : null;
        ProductAttributesDto attributes = components.contains(ProductDtoComponent.attributes) ? mapAttributes(product, vConfig, domainLanguage) : null;
        ProductResourcesDto resources = components.contains(ProductDtoComponent.resources) ? mapResources(product) : null;
        ProductDatasourcesDto datasources = components.contains(ProductDtoComponent.datasources) ? mapDatasources(product) : null;
        ProductScoresDto scores = components.contains(ProductDtoComponent.scores) ? mapScores(product, domainLanguage, vConfig) : null;
        ProductAiTextsDto aiTexts = components.contains(ProductDtoComponent.aiTexts) ? mapAiTexts(product, domainLanguage, locale) : null;
        ProductOffersDto offers = components.contains(ProductDtoComponent.offers) ? mapOffers(product) : null;

        String slug = null;
        if (product.getNames() != null) {
            // Slug is derived from the vertical specific URL mapping defined in the product names block.
            slug = resolveLocalisedString(product.getNames().getUrl(), domainLanguage, locale);
        }
        String fullSlug = null;
        if (StringUtils.hasText(slug)) {
            String verticalHomeUrl = resolveVerticalHomeUrl(product.getVertical(), domainLanguage, vConfig);
            if (StringUtils.hasText(verticalHomeUrl)) {
                fullSlug = "/" + verticalHomeUrl + "/" + slug;
            }
        }

        return new ProductDto(
                product.getId(),
                slug,
                fullSlug,
                base,
                identity,
                names,
                attributes,
                resources,
                datasources,
                scores,
                aiTexts,
                offers);
    }

    /**
     * Translate the user provided includes into the enum set used internally.
     *
     * @param includes includes provided by the caller
     * @return resolved set of components, defaults to all when empty or {@code null}
     */
    private EnumSet<ProductDtoComponent> resolveComponents(Set<String> includes) {
        if (includes == null || includes.isEmpty()) {
            return EnumSet.allOf(ProductDtoComponent.class);
        }
        EnumSet<ProductDtoComponent> resolved = EnumSet.noneOf(ProductDtoComponent.class);
        for (String include : includes) {
            if (include == null || include.isBlank()) {
                continue;
            }
            try {
                resolved.add(ProductDtoComponent.valueOf(include.trim()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown component requested: " + include, ex);
            }
        }
        return resolved.isEmpty() ? EnumSet.allOf(ProductDtoComponent.class) : resolved;
    }

    /**
     * Map immutable product fields describing core metadata (creation timestamps, vertical etc.).
     */
    private ProductBaseDto mapBase(Product product, DomainLanguage domainLanguage, Locale locale) {
        Score ecoscore = product.ecoscore();
        return new ProductBaseDto(
                product.getId(),
                product.getCreationDate(),
                product.getLastChange(),
                product.getVertical(),
                mapExternalIds(product.getExternalIds()),
                product.getGoogleTaxonomyId(),
                product.isExcluded(),
                product.getExcludedCauses() == null ? Collections.emptySet() : new LinkedHashSet<>(product.getExcludedCauses()),
                mapGtinInfo(product.getGtinInfos(), domainLanguage, locale),
                resolveCoverImageUrl(product.getCoverImagePath()),
                product.bestName(),
                ecoscore == null ? null : ecoscore.getValue());
    }

    /**
     * Map identity information such as alternate brand/model names.
     */
    private ProductIdentityDto mapIdentity(Product product) {
        Map<String, String> akaBrandsByDatasource = product.getAkaBrands() == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(product.getAkaBrands());
        Set<String> akaBrands = product.akaBrands() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(product.akaBrands());
        Set<String> akaModels = product.getAkaModels() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(product.getAkaModels());

        return new ProductIdentityDto(
                product.brand(),
                product.model(),
                product.bestName(),
                safeCall(product::randomModel),
                safeCall(product::shortestModel),
                akaModels,
                akaBrandsByDatasource,
                akaBrands);
    }

    /**
     * Map the localised names and SEO metadata associated with the product.
     */
    private ProductNamesDto mapNames(Product product, DomainLanguage domainLanguage, Locale locale) {
        if (product.getNames() == null) {
            return null;
        }
        return new ProductNamesDto(
                resolveLocalisedString(product.getNames().getH1Title(), domainLanguage, locale),
                resolveLocalisedString(product.getNames().getMetaDescription(), domainLanguage, locale),
                resolveLocalisedString(product.getNames().getProductMetaOpenGraphTitle(), domainLanguage, locale),
                resolveLocalisedString(product.getNames().getProductMetaOpenGraphDescription(), domainLanguage, locale),
                product.getOfferNames() == null ? Collections.emptySet() : new LinkedHashSet<>(product.getOfferNames()),
                safeCall(product::longestOfferName),
                safeCall(product::shortestOfferName));
    }

    /**
     * Map the detailed attributes block including indexed and referential values.
     */
    private ProductAttributesDto mapAttributes(Product product, VerticalConfig vConfig, DomainLanguage domainLanguage) {
        ProductAttributes attributes = product.getAttributes();
        List<ProductClassifiedAttributeGroupDto> classifiedAttributes = mapClassifiedAttributes(product, vConfig, domainLanguage);
        if (attributes == null) {
            return new ProductAttributesDto(Collections.emptyMap(), Collections.emptyMap(),  classifiedAttributes);
        }
        Map<String, ProductIndexedAttributeDto> indexed;
        if (attributes.getIndexed() == null) {
            indexed = Collections.emptyMap();
        } else {
            indexed = new LinkedHashMap<>();
            attributes.getIndexed().forEach((key, value) -> indexed.put(key, mapIndexedAttribute(value, vConfig, domainLanguage)));
        }
        Map<String, String> referential;
        if (attributes.getReferentielAttributes() == null) {
            referential = Collections.emptyMap();
        } else {
            referential = new LinkedHashMap<>();
            attributes.getReferentielAttributes().forEach((key, value) -> referential.put(key.toString(), value));
        }

        return new ProductAttributesDto(referential, indexed, classifiedAttributes);
    }

    private List<ProductClassifiedAttributeGroupDto> mapClassifiedAttributes(Product product, VerticalConfig vConfig, DomainLanguage domainLanguage) {
        if (product == null || product.getAttributes() == null || vConfig == null) {
            return Collections.emptyList();
        }
        List<AttributesFeatureGroups> groups = icecatService.features(vConfig, resolveIcecatLanguage(domainLanguage), product);
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        return groups.stream()
                .map(group -> new ProductClassifiedAttributeGroupDto(
                        group.getName(),
                        mapAttributeList(group.getAttributes()),
                        mapAttributeList(group.features()),
                        mapAttributeList(group.unFeatures())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String resolveIcecatLanguage(DomainLanguage domainLanguage) {
        return domainLanguage == null ? DomainLanguage.en.languageTag() : domainLanguage.languageTag();
    }


    private List<ProductAttributeDto> mapAttributeList(List<ProductAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Collections.emptyList();
        }
        return attributes.stream()
                .map(this::mapProductAttribute)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Map images, videos and documents attached to the product.
     */
    private ProductResourcesDto mapResources(Product product) {
        List<ProductImageDto> images = product.images() == null
                ? Collections.emptyList()
                : product.images().stream()
                        .map(this::mapImage)
                        .filter(Objects::nonNull)
                        .toList();
        List<ProductVideoDto> videos = product.videos() == null
                ? Collections.emptyList()
                : product.videos().stream()
                        .map(this::mapVideo)
                        .filter(Objects::nonNull)
                        .toList();
        List<ProductPdfDto> pdfs = product.pdfs() == null
                ? Collections.emptyList()
                : product.pdfs().stream()
                        .map(this::mapPdf)
                        .filter(Objects::nonNull)
                        .toList();

        return new ProductResourcesDto(images, videos, pdfs, resolveCoverImageUrl(product.getCoverImagePath()),
                product.externalCover());
    }

    /**
     * Map datasource statistics associated with the product.
     */
    private ProductDatasourcesDto mapDatasources(Product product) {
        Map<String, Long> datasourceCodes = product.getDatasourceCodes() == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(product.getDatasourceCodes());

        return new ProductDatasourcesDto(datasourceCodes);
    }

    /**
     * Map eco score and other computed scores into DTO representations.
     */
    private ProductScoresDto mapScores(Product product, DomainLanguage domainLanguage, VerticalConfig vConf) {
        Map<String, ProductScoreDto> scores = product.getScores() == null
                ? Collections.emptyMap()
                : product.getScores().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey,
                                entry -> mapScore(entry.getValue(), domainLanguage, vConf),
                                (left, right) -> right,
                                LinkedHashMap::new));
        List<ProductScoreDto> realScores = product.realScores() == null
                ? Collections.emptyList()
                : product.realScores().stream().map(e -> mapScore(e,domainLanguage, vConf)).toList();
        List<ProductScoreDto> virtualScores = product.virtualScores() == null
                ? Collections.emptyList()
                : product.virtualScores().stream().map(e -> mapScore(e,domainLanguage, vConf)).toList();
        ProductScoreDto ecoscore = product.ecoscore() == null ? null : mapScore(product.ecoscore(),domainLanguage, vConf);
        Set<String> worstScores = product.getWorsesScores() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(product.getWorsesScores());
        Set<String> bestScores = product.getBestsScores() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(product.getBestsScores());
        ProductRankingDto ranking = mapRanking(product);

        return new ProductScoresDto(scores, realScores, virtualScores, ecoscore, worstScores, bestScores, ranking);
    }

    /**
     * Map ranking information provided by the product repository.
     */
    private ProductRankingDto mapRanking(Product product) {
        EcoScoreRanking ranking = product.getRanking();
        if (ranking == null) {
            return null;
        }
        return new ProductRankingDto(
                ranking.getGlobalPosition(),
                ranking.getGlobalCount(),
                ranking.getGlobalBest(),
                ranking.getGlobalBetter(),
                ranking.getSpecializedPosition(),
                ranking.getSpecializedCount(),
                ranking.getSpecializedBest(),
                ranking.getSpecializedBetter());
    }

    /**
     * Map AI generated descriptive texts.
     */
    private ProductAiTextsDto mapAiTexts(Product product, DomainLanguage domainLanguage, Locale locale) {
        ResolvedLocalisedValue<AiDescriptions> resolved = resolveLocalised(product.getGenaiTexts(), domainLanguage, locale);
        if (resolved.value() == null) {
            return null;
        }
        Map<String, ProductAiDescriptionDto> descriptions = Optional.ofNullable(resolved.value().getDescriptions())
                .orElse(Collections.emptyMap())
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Entry::getKey,
                        entry -> new ProductAiDescriptionDto(entry.getValue().getTs(), entry.getValue().getContent()),
                        (left, right) -> right,
                        LinkedHashMap::new));
        return new ProductAiTextsDto(resolved.key(), descriptions);
    }

    /**
     * Map offers including best prices, histories and trends.
     */
    private ProductOffersDto mapOffers(Product product) {
        AggregatedPrices aggregatedPrices = product.getPrice();
        ProductAggregatedPriceDto bestPrice = mapAggregatedPrice(product.bestPrice());
        ProductAggregatedPriceDto bestNew = aggregatedPrices == null ? null : mapAggregatedPrice(aggregatedPrices.bestNewOffer());
        ProductAggregatedPriceDto bestOccasion = aggregatedPrices == null ? null : mapAggregatedPrice(aggregatedPrices.bestOccasionOffer());

        Map<ProductCondition, List<ProductAggregatedPriceDto>> offersByCondition = Collections.emptyMap();
        ProductPriceHistoryDto newHistory = null;
        ProductPriceHistoryDto occasionHistory = null;
        Map<ProductCondition, Integer> trends = Collections.emptyMap();
        Double historyPriceGap = null;

        if (aggregatedPrices != null) {
            offersByCondition = aggregatedPrices.getConditions() == null
                    ? Collections.emptyMap()
                    : aggregatedPrices.getConditions().stream()
                            .collect(Collectors.toMap(
                                    condition -> condition,
                                    condition -> aggregatedPrices.sortedOffers(condition).stream()
                                            .map(this::mapAggregatedPrice)
                                            .collect(Collectors.toList()),
                                    (left, right) -> right,
                                    () -> new EnumMap<>(ProductCondition.class)));
            newHistory = mapPriceHistory(aggregatedPrices.getNewPricehistory());
            occasionHistory = mapPriceHistory(aggregatedPrices.getOccasionPricehistory());
            trends = aggregatedPrices.getTrends() == null
                    ? Collections.emptyMap()
                    : new EnumMap<>(aggregatedPrices.getTrends());
            historyPriceGap = aggregatedPrices.historyPriceGap();
        }

        return new ProductOffersDto(
                product.getOffersCount(),
                product.hasOccasions(),
                bestPrice,
                bestNew,
                bestOccasion,
                offersByCondition,
                newHistory,
                occasionHistory,
                trends,
                historyPriceGap);
    }

    /**
     * Retrieve a paginated list of products and map them to DTOs.
     *
     * @param pageable        pagination configuration
     * @param locale          locale requested by the caller
     * @param includes        optional set of components to include
     * @param aggregation     aggregation configuration (currently ignored but reserved for future use)
     * @param domainLanguage  domain language requested by the caller
     * @return page of mapped products
     */
    /**
     * Execute a paginated search on products and convert the results to DTOs alongside aggregation metadata.
     *
     * @param pageable         pagination information requested by the caller
     * @param locale           locale resolved for the request
     * @param includes         requested DTO components
     * @param aggregation      optional aggregation definition
     * @param domainLanguage   requested domain language
     * @param verticalId       optional vertical identifier
     * @param query            optional free text query
     * @param filters         optional search filters applied on the Elasticsearch query
     * @return response payload containing paginated products and aggregations
     */
    public ProductSearchResponseDto searchProducts(Pageable pageable, Locale locale, Set<String> includes,
            AggregationRequestDto aggregation, DomainLanguage domainLanguage, String verticalId, String query,
            FilterRequestDto filters) {

        SearchService.SearchResult result = searchService.search(pageable, verticalId, query, aggregation, filters);
        SearchHits<Product> hits = result.hits();

        List<ProductDto> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(product -> mapProduct(product, locale, includes, domainLanguage))
                .toList();

        Page<ProductDto> page = new PageImpl<>(items, pageable, hits.getTotalHits());
        PageMetaDto meta = new PageMetaDto(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        PageDto<ProductDto> pageDto = new PageDto<>(meta, items);

        return new ProductSearchResponseDto(pageDto, result.aggregations());
    }

    /**
     * Retrieve reviews for a given product (placeholder implementation).
     */
    public Page<ProductReviewDto> getReviews(long gtin, Pageable pageable) throws ResourceNotFoundException {
        List<ProductReviewDto> reviews = new ArrayList<>();
        return new PageImpl<>(reviews, pageable, 0);
    }

    /**
     * Request AI review generation for the product (currently delegated to the repository).
     */
    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
        repository.getById(gtin);
        logger.info("AI review generation requested for product {}", gtin);
    }

    /**
     * Map external identifier references (ASIN, SKU, ...).
     */
    private ProductExternalIdsDto mapExternalIds(ExternalIds externalIds) {
        if (externalIds == null) {
            return null;
        }
        return new ProductExternalIdsDto(
                externalIds.getAsin(),
                externalIds.getIcecat(),
                externalIds.getMpn() == null ? Collections.emptySet() : new LinkedHashSet<>(externalIds.getMpn()),
                externalIds.getSku() == null ? Collections.emptySet() : new LinkedHashSet<>(externalIds.getSku()));
    }

    /**
     * Map GTIN related information including localisation of country name.
     */
    private ProductGtinInfoDto mapGtinInfo(GtinInfo gtinInfo, DomainLanguage domainLanguage, Locale locale) {
        if (gtinInfo == null) {
            return null;
        }
        String countryCode = gtinInfo.getCountry();
        if (!StringUtils.hasText(countryCode)) {
            return new ProductGtinInfoDto(gtinInfo.getUpcType(), null, null, null);
        }

        ULocale displayLocale = resolveDisplayLocale(domainLanguage, locale);
        String countryName = new ULocale("", countryCode).getDisplayCountry(displayLocale);
        String countryFlagUrl = "/images/flags/" + countryCode.toLowerCase(Locale.ROOT) + ".webp";

        return new ProductGtinInfoDto(gtinInfo.getUpcType(), countryCode, countryName, countryFlagUrl);
    }

    /**
     * Resolve the ICU locale used to display country names.
     */
    private ULocale resolveDisplayLocale(DomainLanguage domainLanguage, Locale locale) {
        if (domainLanguage != null && StringUtils.hasText(domainLanguage.languageTag())) {
            return ULocale.forLanguageTag(domainLanguage.languageTag());
        }
        if (locale != null) {
            return ULocale.forLocale(locale);
        }
        return ULocale.getDefault();
    }

    /**
     * Resolve the vertical home URL using localisation fallback rules.
     */
    private String resolveVerticalHomeUrl(String verticalId, DomainLanguage domainLanguage, VerticalConfig config) {
        if (!StringUtils.hasText(verticalId)) {
            return null;
        }
        if (config == null) {
            return null;
        }
        VerticalConfigDto dto = categoryMappingService.toVerticalConfigDto(config, domainLanguage);
        if (dto == null || !StringUtils.hasText(dto.verticalHomeUrl())) {
            return null;
        }
        return dto.verticalHomeUrl();
    }

    /**
     * Build the public URL of the cover image based on the configured resource root.
     */
    private String resolveCoverImageUrl(String coverImagePath) {
        if (!StringUtils.hasText(coverImagePath)) {
            return null;
        }
        String resourceRoot = apiProperties.getResourceRootPath();
        if (!StringUtils.hasText(resourceRoot)) {
            return coverImagePath;
        }
        return resourceRoot + coverImagePath;
    }

    /**
     * Build the public URL of a resource using the configured root path and cache key information.
     */
    private String buildResourceUrl(Resource resource, String pathSegment) {
        if (resource == null) {
            return null;
        }
        String resourceRoot = apiProperties.getResourceRootPath();
        if (!StringUtils.hasText(resourceRoot)
                || !StringUtils.hasText(pathSegment)
                || !StringUtils.hasText(resource.getFileName())
                || !StringUtils.hasText(resource.getCacheKey())
                || !StringUtils.hasText(resource.getExtension())) {
            return resource.getUrl();
        }
        return resourceRoot + pathSegment + resource.getFileName() + "_" + resource.getCacheKey() + "." + resource.getExtension();
    }

    /**
     * Convert a resource URL to its optimised WebP variant by replacing the extension.
     */
    private String toWebpUrl(String originalUrl) {
        if (!StringUtils.hasText(originalUrl)) {
            return originalUrl;
        }
        int querySeparatorIndex = originalUrl.indexOf('?');
        String basePath = querySeparatorIndex >= 0 ? originalUrl.substring(0, querySeparatorIndex) : originalUrl;
        String query = querySeparatorIndex >= 0 ? originalUrl.substring(querySeparatorIndex) : "";

        int lastSlashIndex = basePath.lastIndexOf('/');
        int extensionIndex = basePath.lastIndexOf('.');
        if (extensionIndex > lastSlashIndex) {
            basePath = basePath.substring(0, extensionIndex + 1) + "webp";
        }
        else {
            basePath = basePath + ".webp";
        }
        return basePath + query;
    }

    /**
     * Map indexed attributes which include localisation hints and scoring flags.
     */
    private ProductIndexedAttributeDto mapIndexedAttribute(IndexedAttribute attribute, VerticalConfig vConfig, DomainLanguage domainLanguage) {
        if (attribute == null) {
            return null;
        }
        String displayName = attribute.getName();
        if (vConfig != null && vConfig.getAttributesConfig() != null
                && vConfig.getAttributesConfig().getAttributeConfigByKey(attribute.getName()) != null) {
            displayName = vConfig.getAttributesConfig().getAttributeConfigByKey(attribute.getName()).getName()
                    .get(domainLanguage.languageTag());
        }
        return new ProductIndexedAttributeDto(
                displayName,
                attribute.getValue(),
                attribute.getNumericValue(),
                attribute.getBoolValue());
    }

    /**
     * Map a generic attribute entry including provenance metadata.
     */
    private ProductAttributeDto mapProductAttribute(ProductAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        Set<ProductSourcedAttributeDto> sources = attribute.getSource() == null
                ? Collections.emptySet()
                : attribute.getSource().stream()
                        .map(this::mapSourcedAttribute)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ProductAttributeDto(
                attribute.getName(),
                attribute.getValue(),
                attribute.getIcecatTaxonomyIds() == null ? Collections.emptySet() : new LinkedHashSet<>(attribute.getIcecatTaxonomyIds()),
                sources);
    }

    /**
     * Map a sourced attribute containing provenance metadata.
     */
    private ProductSourcedAttributeDto mapSourcedAttribute(SourcedAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        return new ProductSourcedAttributeDto(
                attribute.getDataSourcename(),
                attribute.getValue(),
                null,
                attribute.getIcecatTaxonomyId(),
                attribute.getName());
    }

    /**
     * Map an image resource to the dedicated DTO.
     */
    private ProductImageDto mapImage(Resource resource) {
        if (resource == null) {
            return null;
        }
        ImageInfo imageInfo = resource.getImageInfo();
        String originalUrl = buildResourceUrl(resource, IMAGES_PATH);
        return new ProductImageDto(
                toWebpUrl(originalUrl),
                IMAGE_WEBP_MEDIATYPE,
                originalUrl,
                resource.getMimeType(),
                resource.getTimeStamp(),
                resource.getCacheKey(),
                resource.isEvicted(),
                resource.isProcessed(),
                resource.getStatus(),
                resource.getFileSize(),
                resource.getFileName(),
                resource.getExtension(),
                resource.getMd5(),
                resource.getResourceType(),
                imageInfo == null ? null : imageInfo.getHeight(),
                imageInfo == null ? null : imageInfo.getWidth(),
                resource.getGroup(),
                resource.getDatasourceName(),
                resource.getTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getTags()),
                resource.getHardTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getHardTags()));
    }

    /**
     * Map a video resource to the dedicated DTO.
     */
    private ProductVideoDto mapVideo(Resource resource) {
        if (resource == null) {
            return null;
        }
        return new ProductVideoDto(
                buildResourceUrl(resource, VIDEOS_PATH),
                resource.getMimeType(),
                resource.getTimeStamp(),
                resource.getCacheKey(),
                resource.isEvicted(),
                resource.isProcessed(),
                resource.getStatus(),
                resource.getFileSize(),
                resource.getFileName(),
                resource.getExtension(),
                resource.getMd5(),
                resource.getResourceType(),
                resource.getGroup(),
                resource.getDatasourceName(),
                resource.getTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getTags()),
                resource.getHardTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getHardTags()));
    }

    /**
     * Map a PDF resource to the dedicated DTO.
     */
    private ProductPdfDto mapPdf(Resource resource) {
        if (resource == null) {
            return null;
        }
        PdfInfo pdfInfo = resource.getPdfInfo();
        return new ProductPdfDto(
                buildResourceUrl(resource, PDFS_PATH),
                resource.getMimeType(),
                resource.getTimeStamp(),
                resource.getCacheKey(),
                resource.isEvicted(),
                resource.isProcessed(),
                resource.getStatus(),
                resource.getFileSize(),
                resource.getFileName(),
                resource.getExtension(),
                resource.getMd5(),
                resource.getResourceType(),
                resource.getGroup(),
                resource.getDatasourceName(),
                resource.getTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getTags()),
                resource.getHardTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getHardTags()),
                pdfInfo == null ? null : pdfInfo.getMetadataTitle(),
                pdfInfo == null ? null : pdfInfo.getExtractedTitle(),
                pdfInfo == null ? null : pdfInfo.getNumberOfPages(),
                pdfInfo == null ? null : pdfInfo.getAuthor(),
                pdfInfo == null ? null : pdfInfo.getSubject(),
                pdfInfo == null ? null : pdfInfo.getKeywords(),
                pdfInfo == null ? null : pdfInfo.getCreationDate(),
                pdfInfo == null ? null : pdfInfo.getModificationDate(),
                pdfInfo == null ? null : pdfInfo.getProducer(),
                pdfInfo == null ? null : pdfInfo.getLanguage(),
                pdfInfo == null ? null : pdfInfo.getLanguageConfidence());
    }

    /**
     * Map aggregated price data and rebuild the affiliation link when needed.
     */
    private ProductAggregatedPriceDto mapAggregatedPrice(AggregatedPrice price) {
        if (price == null) {
            return null;
        }
        String url = price.getUrl();
        if (StringUtils.hasText(price.getDatasourceName()) && StringUtils.hasText(price.getUrl())) {
            url = AffiliationPartnerService.CONTRIB_ENDPOINT
                    + affiliationService.encryptAffiliationLink(price.getDatasourceName(), price.getUrl());
        }
        return new ProductAggregatedPriceDto(
                price.getDatasourceName(),
                price.getOfferName(),
                url,
                price.getCompensation(),
                price.getProductState(),
                price.getAffiliationToken(),
                price.getPrice(),
                price.getCurrency(),
                price.getTimeStamp(),
                safeCall(price::shortPrice));
    }

    /**
     * Map the price history entries to DTOs and compute derived statistics.
     */
    private ProductPriceHistoryDto mapPriceHistory(List<PriceHistory> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        List<ProductPriceHistoryEntryDto> entries = history.stream()
                .map(this::mapPriceHistoryEntry)
                .collect(Collectors.toList());
        PriceHistory lowest = history.stream().min((left, right) -> Double.compare(left.price(), right.price())).orElse(null);
        PriceHistory highest = history.stream().max((left, right) -> Double.compare(left.price(), right.price())).orElse(null);
        Double average = history.stream().mapToDouble(PriceHistory::price).average().orElse(Double.NaN);
        if (Double.isNaN(average)) {
            average = null;
        }
        return new ProductPriceHistoryDto(
                entries,
                mapPriceHistoryEntry(lowest),
                mapPriceHistoryEntry(highest),
                average);
    }

    /**
     * Map a single price history entry.
     */
    private ProductPriceHistoryEntryDto mapPriceHistoryEntry(PriceHistory entry) {
        if (entry == null) {
            return null;
        }
        return new ProductPriceHistoryEntryDto(entry.timestamp(), entry.price());
    }

    /**
     * Map a score into its DTO, including localisation of impact criteria when possible.
     */
    private ProductScoreDto mapScore(Score score, DomainLanguage domainLanguage, VerticalConfig vConf) {
        if (score == null) {
            return null;
        }

        ImpactScoreCriteria criteria = null;
        if (vConf != null && vConf.getAvailableImpactScoreCriterias() != null) {
            criteria = vConf.getAvailableImpactScoreCriterias().get(score.getName());
        }

        String description = null;
        String title = null;
        if (criteria == null) {
            logger.debug("No ImpactScoreCriteria found for {}", score.getName());
            title = score.getName();
        }
        else {
            title = criteria.getTitle().i18n(domainLanguage.languageTag());
            description = criteria.getDescription().i18n(domainLanguage.languageTag());
        }


        return new ProductScoreDto(
        		score.getName(),
                title,
                description,
                score.getVirtual(),
                score.getValue(),
                mapCardinality(score.getAbsolute()),
                mapCardinality(score.getRelativ()),
                score.getMetadatas(),
                score.getRanking(),
                score.getLowestScoreId(),
                score.getHighestScoreId(),
                safeCall(score::percent),
                safeCall(score::on20),
                safeCall(score::absValue),
                safeCall(score::relValue),
                safeCall(score::letter));
    }

    /**
     * Map cardinality statistics for score computation.
     */
    private ProductCardinalityDto mapCardinality(Cardinality cardinality) {
        if (cardinality == null) {
            return null;
        }
        return new ProductCardinalityDto(
                cardinality.getMin(),
                cardinality.getMax(),
                cardinality.getAvg(),
                cardinality.getCount(),
                cardinality.getSum(),
                cardinality.getValue());
    }

    /**
     * Resolve a string localisable by delegating to {@link #resolveLocalised(Localisable, DomainLanguage, Locale)}.
     */
    private String resolveLocalisedString(Localisable<String, String> localisable, DomainLanguage domainLanguage, Locale locale) {
        ResolvedLocalisedValue<String> resolved = resolveLocalised(localisable, domainLanguage, locale);
        return resolved.value();
    }

    /**
     * Resolve the best matching value from a {@link Localisable} structure.
     */
    private <T> ResolvedLocalisedValue<T> resolveLocalised(Localisable<String, T> localisable, DomainLanguage domainLanguage, Locale locale) {
        if (localisable == null || localisable.isEmpty()) {
            return new ResolvedLocalisedValue<>(null, null);
        }
        for (String key : candidateLanguageKeys(domainLanguage, locale)) {
            if (localisable.containsKey(key)) {
                T value = localisable.get(key);
                if (value != null) {
                    return new ResolvedLocalisedValue<>(key, value);
                }
            }
        }
        Optional<Entry<String, T>> fallback = localisable.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .findFirst();
        return fallback.map(entry -> new ResolvedLocalisedValue<>(entry.getKey(), entry.getValue()))
                .orElseGet(() -> new ResolvedLocalisedValue<>(null, null));
    }

    /**
     * Build a deterministic list of language keys used to look up localised values.
     */
    private Set<String> candidateLanguageKeys(DomainLanguage domainLanguage, Locale locale) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (domainLanguage != null) {
            String iso = domainLanguage.name();
            String tag = domainLanguage.languageTag();
            String normalizedTag = tag.replace('_', '-');
            keys.add(iso);
            keys.add(iso.toLowerCase(Locale.ROOT));
            keys.add(iso.toUpperCase(Locale.ROOT));
            keys.add(tag);
            keys.add(tag.toLowerCase(Locale.ROOT));
            keys.add(normalizedTag);
            keys.add(normalizedTag.toLowerCase(Locale.ROOT));
            if (normalizedTag.contains("-")) {
                String base = normalizedTag.substring(0, normalizedTag.indexOf('-'));
                keys.add(base);
                keys.add(base.toLowerCase(Locale.ROOT));
            }
        }
        if (locale != null) {
            String language = locale.getLanguage();
            if (language != null && !language.isBlank()) {
                keys.add(language);
                keys.add(language.toLowerCase(Locale.ROOT));
            }
            String localeTag = locale.toLanguageTag();
            if (localeTag != null && !localeTag.isBlank()) {
                keys.add(localeTag);
                keys.add(localeTag.toLowerCase(Locale.ROOT));
            }
        }
        keys.add(DEFAULT_LANGUAGE_KEY);
        return keys;
    }

    /**
     * Execute a supplier and swallow unexpected exceptions, returning {@code null} on failure.
     */
    private <T> T safeCall(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            logger.debug("Computed helper failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Small value object capturing the resolved key and value for localisation lookups.
     */
    private record ResolvedLocalisedValue<T>(String key, T value) {
    }
}
