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
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.open4goods.model.Localisable;
import org.open4goods.model.ai.AiDescriptions;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.ExternalIds;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.Score;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.model.resource.PdfInfo;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiDescriptionDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiTextsDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAggregatedPriceDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAttributesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductBaseDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardinalityDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDatasourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductExternalIdsDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductGtinInfoDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductIdentityDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductIndexedAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductNamesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductPriceHistoryDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductPriceHistoryEntryDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductRankingDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourceDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourceImageInfoDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourcePdfInfoDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoreDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoresDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductSourcedAttributeDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps {@link Product} domain entities to DTOs consumed by the frontend API.
 * Handles localisation and filtering of the returned fields.
 */

@Service
public class ProductMappingService {

    private static final Logger logger = LoggerFactory.getLogger(ProductMappingService.class);
    private static final String DEFAULT_LANGUAGE_KEY = "default";

    private final ProductRepository repository;

    public ProductMappingService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieve and map a product.
     */
    public ProductDto getProduct(long gtin, Locale locale, Set<String> includes, DomainLanguage domainLanguage)
            throws ResourceNotFoundException {
        Product product = repository.getById(gtin);
        return mapProduct(product, locale, includes, domainLanguage);
    }

    private ProductDto mapProduct(Product product, Locale locale, Set<String> includes, DomainLanguage domainLanguage) {
        EnumSet<ProductDtoComponent> components = resolveComponents(includes);

        ProductBaseDto base = components.contains(ProductDtoComponent.base) ? mapBase(product) : null;
        ProductIdentityDto identity = components.contains(ProductDtoComponent.identity) ? mapIdentity(product) : null;
        ProductNamesDto names = components.contains(ProductDtoComponent.names) ? mapNames(product, domainLanguage, locale) : null;
        ProductAttributesDto attributes = components.contains(ProductDtoComponent.attributes) ? mapAttributes(product) : null;
        ProductResourcesDto resources = components.contains(ProductDtoComponent.resources) ? mapResources(product) : null;
        ProductDatasourcesDto datasources = components.contains(ProductDtoComponent.datasources) ? mapDatasources(product) : null;
        ProductScoresDto scores = components.contains(ProductDtoComponent.scores) ? mapScores(product) : null;
        ProductRankingDto ranking = components.contains(ProductDtoComponent.ranking) ? mapRanking(product) : null;
        ProductAiTextsDto aiTexts = components.contains(ProductDtoComponent.aiTexts) ? mapAiTexts(product, domainLanguage, locale) : null;
        ProductAiReviewDto aiReview = components.contains(ProductDtoComponent.aiReview) ? mapAiReview(product, domainLanguage, locale) : null;
        ProductOffersDto offers = components.contains(ProductDtoComponent.offers) ? mapOffers(product) : null;

        String slug = null;
        if (product.getNames() != null) {
            slug = resolveLocalisedString(product.getNames().getUrl(), domainLanguage, locale);
        }

        return new ProductDto(
                product.getId(),
                slug,
                base,
                identity,
                names,
                attributes,
                resources,
                datasources,
                scores,
                ranking,
                aiTexts,
                aiReview,
                offers);
    }

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

    private ProductBaseDto mapBase(Product product) {
        return new ProductBaseDto(
                product.getId(),
                product.getCreationDate(),
                product.getLastChange(),
                product.getVertical(),
                mapExternalIds(product.getExternalIds()),
                product.getGoogleTaxonomyId(),
                product.isExcluded(),
                product.getExcludedCauses() == null ? Collections.emptySet() : new LinkedHashSet<>(product.getExcludedCauses()),
                mapGtinInfo(product.getGtinInfos()),
                product.getCoverImagePath());
    }

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

    private ProductAttributesDto mapAttributes(Product product) {
        ProductAttributes attributes = product.getAttributes();
        if (attributes == null) {
            return new ProductAttributesDto(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), product.caracteristics());
        }
        Map<String, ProductIndexedAttributeDto> indexed;
        if (attributes.getIndexed() == null) {
            indexed = Collections.emptyMap();
        } else {
            indexed = new LinkedHashMap<>();
            attributes.getIndexed().forEach((key, value) -> indexed.put(key, mapIndexedAttribute(value)));
        }
        Map<String, ProductAttributeDto> all;
        if (attributes.getAll() == null) {
            all = Collections.emptyMap();
        } else {
            all = new LinkedHashMap<>();
            attributes.getAll().forEach((key, value) -> all.put(key, mapProductAttribute(value)));
        }
        Map<String, String> referential;
        if (attributes.getReferentielAttributes() == null) {
            referential = Collections.emptyMap();
        } else {
            referential = new LinkedHashMap<>();
            attributes.getReferentielAttributes().forEach((key, value) -> referential.put(key.toString(), value));
        }

        return new ProductAttributesDto(referential, indexed, all, product.caracteristics());
    }

    private ProductResourcesDto mapResources(Product product) {
        List<ProductResourceDto> images = product.images() == null
                ? Collections.emptyList()
                : product.images().stream().map(this::mapResource).toList();
        List<ProductResourceDto> videos = product.videos() == null
                ? Collections.emptyList()
                : product.videos().stream().map(this::mapResource).toList();
        List<ProductResourceDto> pdfs = product.pdfs() == null
                ? Collections.emptyList()
                : product.pdfs().stream().map(this::mapResource).toList();

        return new ProductResourcesDto(images, videos, pdfs, product.externalCover());
    }

    private ProductDatasourcesDto mapDatasources(Product product) {
        Map<String, Long> datasourceCodes = product.getDatasourceCodes() == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(product.getDatasourceCodes());

        return new ProductDatasourcesDto(datasourceCodes);
    }

    private ProductScoresDto mapScores(Product product) {
        Map<String, ProductScoreDto> scores = product.getScores() == null
                ? Collections.emptyMap()
                : product.getScores().entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey,
                                entry -> mapScore(entry.getValue()),
                                (left, right) -> right,
                                LinkedHashMap::new));
        List<ProductScoreDto> realScores = product.realScores() == null
                ? Collections.emptyList()
                : product.realScores().stream().map(this::mapScore).toList();
        List<ProductScoreDto> virtualScores = product.virtualScores() == null
                ? Collections.emptyList()
                : product.virtualScores().stream().map(this::mapScore).toList();
        ProductScoreDto ecoscore = product.ecoscore() == null ? null : mapScore(product.ecoscore());
        Set<String> worstScores = product.getWorsesScores() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(product.getWorsesScores());
        Set<String> bestScores = product.getBestsScores() == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(product.getBestsScores());

        return new ProductScoresDto(scores, realScores, virtualScores, ecoscore, worstScores, bestScores);
    }

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

    private ProductAiReviewDto mapAiReview(Product product, DomainLanguage domainLanguage, Locale locale) {
        ResolvedLocalisedValue<AiReviewHolder> resolved = resolveLocalised(product.getReviews(), domainLanguage, locale);
        if (resolved.value() == null) {
            return null;
        }
        AiReviewHolder holder = resolved.value();
        return new ProductAiReviewDto(
                resolved.key(),
                holder.getReview(),
                holder.getSources(),
                holder.isEnoughData(),
                holder.getTotalTokens(),
                holder.getCreatedMs());
    }

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

    public Page<ProductDto> getProducts(Pageable pageable, Locale locale, Set<String> includes,
                                        AggregationRequestDto aggregation, DomainLanguage domainLanguage) {

        SearchHits<Product> response = repository.get(pageable);
        List<ProductDto> items = response
                .map(hit -> hit.getContent())
                .map(product -> mapProduct(product, locale, includes, domainLanguage))
                .toList();

        return new PageImpl<>(items, pageable, response.getTotalHits());
    }

    public Page<ProductReviewDto> getReviews(long gtin, Pageable pageable) throws ResourceNotFoundException {
        List<ProductReviewDto> reviews = new ArrayList<>();
        return new PageImpl<>(reviews, pageable, 0);
    }

    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
        repository.getById(gtin);
        logger.info("AI review generation requested for product {}", gtin);
    }

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

    private ProductGtinInfoDto mapGtinInfo(GtinInfo gtinInfo) {
        if (gtinInfo == null) {
            return null;
        }
        return new ProductGtinInfoDto(gtinInfo.getUpcType(), gtinInfo.getCountry());
    }

    private ProductIndexedAttributeDto mapIndexedAttribute(IndexedAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        return new ProductIndexedAttributeDto(
                attribute.getName(),
                attribute.getValue(),
                attribute.getNumericValue(),
                attribute.getBoolValue());
    }

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
                attribute.getNumericValue(),
                attribute.getIcecatTaxonomyIds() == null ? Collections.emptySet() : new LinkedHashSet<>(attribute.getIcecatTaxonomyIds()),
                sources);
    }

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

    private ProductResourceDto mapResource(Resource resource) {
        if (resource == null) {
            return null;
        }
        return new ProductResourceDto(
                resource.getUrl(),
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
                mapImageInfo(resource.getImageInfo()),
                mapPdfInfo(resource.getPdfInfo()),
                resource.getGroup(),
                resource.getDatasourceName(),
                resource.getTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getTags()),
                resource.getHardTags() == null ? Collections.emptySet() : new LinkedHashSet<>(resource.getHardTags()));
    }

    private ProductResourceImageInfoDto mapImageInfo(ImageInfo imageInfo) {
        if (imageInfo == null) {
            return null;
        }
        return new ProductResourceImageInfoDto(
                imageInfo.getHeight(),
                imageInfo.getWidth(),
                imageInfo.getpHashValue(),
                imageInfo.getpHashLength());
    }

    private ProductResourcePdfInfoDto mapPdfInfo(PdfInfo pdfInfo) {
        if (pdfInfo == null) {
            return null;
        }
        return new ProductResourcePdfInfoDto(
                pdfInfo.getMetadataTitle(),
                pdfInfo.getExtractedTitle(),
                pdfInfo.getNumberOfPages(),
                pdfInfo.getAuthor(),
                pdfInfo.getSubject(),
                pdfInfo.getKeywords(),
                pdfInfo.getCreationDate(),
                pdfInfo.getModificationDate(),
                pdfInfo.getProducer(),
                pdfInfo.getLanguage(),
                pdfInfo.getLanguageConfidence());
    }

    private ProductAggregatedPriceDto mapAggregatedPrice(AggregatedPrice price) {
        if (price == null) {
            return null;
        }
        return new ProductAggregatedPriceDto(
                price.getDatasourceName(),
                price.getOfferName(),
                price.getUrl(),
                price.getCompensation(),
                price.getProductState(),
                price.getAffiliationToken(),
                price.getPrice(),
                price.getCurrency(),
                price.getTimeStamp(),
                safeCall(price::shortPrice));
    }

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

    private ProductPriceHistoryEntryDto mapPriceHistoryEntry(PriceHistory entry) {
        if (entry == null) {
            return null;
        }
        return new ProductPriceHistoryEntryDto(entry.timestamp(), entry.price());
    }

    private ProductScoreDto mapScore(Score score) {
        if (score == null) {
            return null;
        }
        return new ProductScoreDto(
                score.getName(),
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

    private String resolveLocalisedString(Localisable<String, String> localisable, DomainLanguage domainLanguage, Locale locale) {
        ResolvedLocalisedValue<String> resolved = resolveLocalised(localisable, domainLanguage, locale);
        return resolved.value();
    }

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

    private <T> T safeCall(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            logger.debug("Computed helper failed: {}", ex.getMessage());
            return null;
        }
    }

    private record ResolvedLocalisedValue<T>(String key, T value) {
    }
}
