package org.open4goods.b2bapi.dto.product;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.open4goods.model.product.ProductCondition;

/**
 * Verifies the Product Data API DTO contract is documented and instantiable.
 */
class B2bProductDtoTest {

    private static final List<Class<? extends Record>> PRODUCT_DTO_RECORDS = List.of(
            B2bCoverageMeta.class,
            B2bFacetMeta.class,
            B2bMeta.class,
            B2bOfferDto.class,
            B2bPriceDto.class,
            B2bPriceHistorySummaryDto.class,
            B2bPriceTrendDto.class,
            B2bResponse.class);

    @Test
    void productDtosExposeSchemaMetadataOnEveryRecordComponent() {
        PRODUCT_DTO_RECORDS.forEach(type -> {
            assertThat(type.getAnnotation(Schema.class))
                    .as("%s record schema", type.getSimpleName())
                    .isNotNull();

            Stream.of(type.getRecordComponents()).forEach(component -> assertThat(hasOpenApiSchema(component))
                    .as("%s.%s schema", type.getSimpleName(), component.getName())
                    .isTrue());
        });
    }

    @Test
    void priceEnvelopeCanBeBuiltWithoutPrivateOfferFields() {
        Currency currency = Currency.getInstance("EUR");
        B2bOfferDto bestOffer = new B2bOfferDto(
                "Example Store",
                "Samsung Galaxy S25 256GB - Black",
                "https://merchant.example/products/samsung-galaxy-s25",
                ProductCondition.NEW,
                799.99,
                currency,
                Instant.parse("2026-06-15T09:30:00Z"),
                1,
                "https://merchant.example/favicon.ico");
        B2bPriceTrendDto trend = new B2bPriceTrendDto(
                ProductCondition.NEW,
                -1,
                799.99,
                849.99,
                -50.0,
                -5.88,
                749.99,
                50.0,
                604800000L);
        B2bPriceHistorySummaryDto summary = new B2bPriceHistorySummaryDto(
                ProductCondition.NEW,
                749.99,
                Instant.parse("2026-05-20T08:00:00Z"),
                899.99,
                Instant.parse("2026-04-12T08:00:00Z"),
                812.49,
                currency);
        B2bPriceDto data = new B2bPriceDto(
                "0887276812345",
                "Samsung Galaxy S25",
                "Samsung",
                "SM-S931B",
                1,
                1,
                bestOffer,
                bestOffer,
                null,
                Map.of(ProductCondition.NEW, List.of(bestOffer)),
                trend,
                null,
                summary,
                null);
        B2bMeta meta = new B2bMeta(
                "pdreq_01JZ7V8N9P4K6T2QW3E5R7Y8U9",
                Instant.parse("2026-06-15T10:15:30Z"),
                "en",
                1,
                249,
                true,
                30,
                42,
                List.of(new B2bFacetMeta("price", 1, true, true)),
                List.of(new B2bCoverageMeta("price", true)));

        B2bResponse<B2bPriceDto> response = new B2bResponse<>(data, meta);

        assertThat(response.data().bestPrice().merchant()).isEqualTo("Example Store");
        assertThat(response.data().bestPrice().condition()).isEqualTo(ProductCondition.NEW);
        assertThat(response.data().offersByCondition()).containsKey(ProductCondition.NEW);
        assertThat(response.meta().requestId()).startsWith("pdreq_");
    }

    private static boolean hasOpenApiSchema(RecordComponent component) {
        try {
            Field field = component.getDeclaringRecord().getDeclaredField(component.getName());
            return component.getAnnotation(Schema.class) != null
                    || component.getAnnotation(ArraySchema.class) != null
                    || component.getAccessor().getAnnotation(Schema.class) != null
                    || component.getAccessor().getAnnotation(ArraySchema.class) != null
                    || field.getAnnotation(Schema.class) != null
                    || field.getAnnotation(ArraySchema.class) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
