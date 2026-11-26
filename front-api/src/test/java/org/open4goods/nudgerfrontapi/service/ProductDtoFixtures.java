package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test fixtures for listing payloads.
 *
 * <p>{@code fixtures/product-dto-tv-listing.json} is a real {@link ProductDto} of the {@code /televiseurs} listing
 * (production, 26/11/2026, mapped with {@code base,identity,names,attributes,resources,scores,offers}), about 50 KB:
 * it measures the card projection against what listings carried before GOU-2710.</p>
 */
public final class ProductDtoFixtures {

    /** JSON mapper tolerant to fields added to the DTO after the fixture was captured. */
    public static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private ProductDtoFixtures() {
    }

    /**
     * Load the full listing DTO of a television.
     *
     * @return the deserialised product
     */
    public static ProductDto tvListingProduct() {
        try (InputStream input = ProductDtoFixtures.class.getResourceAsStream("/fixtures/product-dto-tv-listing.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing fixture product-dto-tv-listing.json");
            }
            return MAPPER.readValue(input, ProductDto.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Vertical configuration of the television fixture, with the listing attribute keys of {@code tv.yml}.
     *
     * @return the configuration
     */
    public static VerticalConfig tvVerticalConfig() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setPopularAttributes(List.of("DIAGONALE_POUCES", "DISPLAY_TECHNOLOGY"));
        config.setTechnicalFilters(List.of("DIAGONALE_POUCES", "DISPLAY_TECHNOLOGY", "HDMI_PORTS_QUANTITY"));
        config.setEcoFilters(List.of("REPAIRABILITY_INDEX", "CLASSE_ENERGY"));
        return config;
    }
}
