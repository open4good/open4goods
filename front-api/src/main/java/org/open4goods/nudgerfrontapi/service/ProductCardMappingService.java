package org.open4goods.nudgerfrontapi.service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAggregatedPriceDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAttributesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductBaseDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardAttributesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardBaseDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardIdentityDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardImageDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardNamesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardOfferDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardResourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardScoreDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductCardScoresDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductIdentityDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductImageDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductIndexedAttributeDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductNamesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoreDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductScoresDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductCardSearchResponseDto;
import org.open4goods.nudgerfrontapi.dto.search.ProductSearchResponseDto;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Project the full {@link ProductDto} of a listing to the lightweight {@link ProductCardDto} (GOU-2710).
 *
 * <p>The projection starts from the DTO produced by {@link ProductMappingService}, so card values (fresh offers,
 * localised names, ImpactScore exposure rules) stay identical to the full payload; it only drops what a card
 * never reads: all attributes and sourcing, price histories, other scores, videos, PDFs and extra images.</p>
 */
@Service
public class ProductCardMappingService {

    /** DTO components a card is projected from. */
    public static final Set<String> CARD_COMPONENTS = Set.of(
            ProductDtoComponent.base.name(),
            ProductDtoComponent.identity.name(),
            ProductDtoComponent.names.name(),
            ProductDtoComponent.attributes.name(),
            ProductDtoComponent.resources.name(),
            ProductDtoComponent.scores.name(),
            ProductDtoComponent.offers.name());

    /** Score kept on cards. */
    static final String ECOSCORE = "ECOSCORE";

    /**
     * Attribute key embedded in a sort or filter mapping, mirroring {@code extractAttributeKeyFromMapping} of the
     * frontend ({@code app/utils/_sort-attribute-display.ts}).
     */
    private static final Pattern ATTRIBUTE_MAPPING = Pattern.compile(
            "attributes\\.(?:indexed(?:Attributes)?|referential(?:Attributes)?|referentielAttributes|indexedAttributes)\\.([^.]+)",
            Pattern.CASE_INSENSITIVE);

    private final VerticalsConfigService verticalsConfigService;

    /**
     * Create the card mapping service.
     *
     * @param verticalsConfigService vertical configurations, source of the attribute keys shown on cards
     */
    public ProductCardMappingService(VerticalsConfigService verticalsConfigService) {
        this.verticalsConfigService = verticalsConfigService;
    }

    /**
     * Project a product search response to its card variant, keeping pagination and aggregations.
     *
     * @param response full search response mapped with {@link #CARD_COMPONENTS}
     * @param sort     effective sort of the search; its attribute keys are kept on the cards
     * @return the card response, with an empty page when {@code response} has no products
     */
    public ProductCardSearchResponseDto toCardResponse(ProductSearchResponseDto response, Sort sort) {
        if (response == null) {
            return new ProductCardSearchResponseDto(null, List.of());
        }
        PageDto<ProductCardDto> page = null;
        if (response.products() != null) {
            Set<String> sortKeys = attributeKeysOf(sort);
            Map<String, Set<String>> keysByVertical = new HashMap<>();
            List<ProductCardDto> cards = response.products().data() == null ? List.of()
                    : response.products().data().stream()
                            .map(product -> toCard(product, keysByVertical.computeIfAbsent(verticalOf(product),
                                    vertical -> cardAttributeKeys(vertical, sortKeys))))
                            .toList();
            page = new PageDto<>(response.products().page(), cards);
        }
        return new ProductCardSearchResponseDto(page, response.aggregations());
    }

    /**
     * Project one product to its card.
     *
     * @param product       full product DTO, may be {@code null}
     * @param attributeKeys indexed attribute keys kept on the card
     * @return the card, or {@code null} when {@code product} is {@code null}
     */
    public ProductCardDto toCard(ProductDto product, Set<String> attributeKeys) {
        if (product == null) {
            return null;
        }
        return new ProductCardDto(
                product.gtin(),
                product.slug(),
                product.fullSlug(),
                toCardBase(product.base()),
                toCardIdentity(product.identity()),
                toCardNames(product.names()),
                toCardAttributes(product.attributes(), attributeKeys),
                toCardResources(product.resources()),
                toCardScores(product.scores()),
                toCardOffers(product.offers()));
    }

    /**
     * Indexed attribute keys displayed by the listings of a vertical: popular attributes (card chips), technical and
     * eco filters (table columns), plus the attributes of the active sort (sorted value shown on cards).
     *
     * @param verticalId vertical identifier, may be {@code null}
     * @param sortKeys   attribute keys extracted from the active sort
     * @return the keys to keep, never {@code null}
     */
    Set<String> cardAttributeKeys(String verticalId, Set<String> sortKeys) {
        Set<String> keys = new LinkedHashSet<>(sortKeys);
        VerticalConfig config = StringUtils.hasText(verticalId) ? verticalsConfigService.getConfigById(verticalId) : null;
        if (config != null) {
            addAll(keys, config.getPopularAttributes());
            addAll(keys, config.getTechnicalFilters());
            addAll(keys, config.getEcoFilters());
        }
        return keys;
    }

    /**
     * Extract the attribute keys referenced by a sort (for instance {@code attributes.indexed.WEIGHT.numericValue}).
     *
     * @param sort sort to inspect, may be {@code null}
     * @return attribute keys, empty when the sort uses no attribute
     */
    static Set<String> attributeKeysOf(Sort sort) {
        Set<String> keys = new LinkedHashSet<>();
        if (sort == null) {
            return keys;
        }
        for (Sort.Order order : sort) {
            Matcher matcher = ATTRIBUTE_MAPPING.matcher(order.getProperty());
            if (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    private static void addAll(Set<String> keys, Collection<String> values) {
        if (values != null) {
            values.stream().filter(StringUtils::hasText).forEach(keys::add);
        }
    }

    private static String verticalOf(ProductDto product) {
        return product == null || product.base() == null || product.base().vertical() == null ? ""
                : product.base().vertical();
    }

    private static ProductCardBaseDto toCardBase(ProductBaseDto base) {
        if (base == null) {
            return null;
        }
        return new ProductCardBaseDto(base.gtin(), base.vertical(), base.bestName(), base.coverImagePath());
    }

    private static ProductCardIdentityDto toCardIdentity(ProductIdentityDto identity) {
        if (identity == null) {
            return null;
        }
        return new ProductCardIdentityDto(identity.brand(), identity.model(), identity.bestName());
    }

    private static ProductCardNamesDto toCardNames(ProductNamesDto names) {
        if (names == null) {
            return null;
        }
        return new ProductCardNamesDto(names.displayName(), names.cardName());
    }

    private static ProductCardAttributesDto toCardAttributes(ProductAttributesDto attributes, Set<String> keys) {
        if (attributes == null) {
            return null;
        }
        Map<String, ProductCardAttributeDto> indexed = new LinkedHashMap<>();
        Map<String, ProductIndexedAttributeDto> source = attributes.indexedAttributes();
        if (source != null && keys != null) {
            for (String key : keys) {
                ProductIndexedAttributeDto attribute = source.get(key);
                if (attribute != null) {
                    indexed.put(key, new ProductCardAttributeDto(attribute.name(), attribute.value(),
                            attribute.numericValue()));
                }
            }
        }
        return new ProductCardAttributesDto(attributes.referentialAttributes(), indexed);
    }

    private static ProductCardResourcesDto toCardResources(ProductResourcesDto resources) {
        if (resources == null) {
            return null;
        }
        List<ProductImageDto> images = resources.images();
        List<ProductCardImageDto> firstImage = images == null || images.isEmpty() || images.get(0) == null
                ? List.of()
                : List.of(new ProductCardImageDto(images.get(0).url(), images.get(0).originalUrl()));
        return new ProductCardResourcesDto(resources.coverImagePath(), resources.externalCover(), firstImage);
    }

    private static ProductCardScoresDto toCardScores(ProductScoresDto scores) {
        if (scores == null) {
            return null;
        }
        Map<String, ProductCardScoreDto> cardScores = null;
        ProductScoreDto ecoscore = scores.scores() == null ? null : scores.scores().get(ECOSCORE);
        if (ecoscore != null) {
            cardScores = Map.of(ECOSCORE, new ProductCardScoreDto(ecoscore.id(), ecoscore.value()));
        }
        return new ProductCardScoresDto(cardScores, scores.impactScore());
    }

    private static ProductCardOffersDto toCardOffers(ProductOffersDto offers) {
        if (offers == null) {
            return null;
        }
        Map<ProductCondition, List<ProductCardOfferDto>> byCondition = null;
        if (offers.offersByCondition() != null) {
            byCondition = new EnumMap<>(ProductCondition.class);
            for (Map.Entry<ProductCondition, List<ProductAggregatedPriceDto>> entry : offers.offersByCondition().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    byCondition.put(entry.getKey(), entry.getValue().stream()
                            .map(ProductCardMappingService::toCardOffer)
                            .toList());
                }
            }
        }
        return new ProductCardOffersDto(
                offers.offersCount(),
                toCardOffer(offers.bestPrice()),
                toCardOffer(offers.bestNewOffer()),
                toCardOffer(offers.bestOccasionOffer()),
                byCondition,
                offers.newTrend(),
                offers.occasionTrend());
    }

    private static ProductCardOfferDto toCardOffer(ProductAggregatedPriceDto offer) {
        if (offer == null) {
            return null;
        }
        return new ProductCardOfferDto(
                offer.datasourceName(),
                offer.favicon(),
                offer.url(),
                offer.condition(),
                offer.affiliationToken(),
                offer.price(),
                offer.currency(),
                offer.shortPrice(),
                offer.quantityInStock());
    }
}
