package org.open4goods.eprelservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.open4goods.services.eprelservice.model.EprelProduct;

/**
 * Unit tests for {@link EprelProduct}.
 */
class EprelProductTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Dates described as arrays should be converted to epoch seconds")
    void shouldConvertDatesFromArrays() throws Exception
    {
        String json = "{" +
                "\"eprelRegistrationNumber\":\"123\"," +
                "\"productGroup\":\"televisions\"," +
                "\"modelIdentifier\":\"MODEL-123\"," +
                "\"onMarketStartDate\":[2024,1,31]," +
                "\"category\":\"MONITOR\"" +
                "}";
        EprelProduct product = objectMapper.readValue(json, EprelProduct.class);
        long expectedEpoch = LocalDate.of(2024, 1, 31).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        assertThat(product.getOnMarketStartDate()).isEqualTo(expectedEpoch);
        assertThat(product.getEprelCategory()).isEqualTo("MONITOR");
    }

    @Test
    @DisplayName("GTIN should be inherited from additional details when absent at root level")
    void shouldPopulateGtinFromAdditionalDetails() throws Exception
    {
        String json = objectMapper.writeValueAsString(Map.of(
                "eprelRegistrationNumber", "456",
                "productGroup", "televisions",
                "modelIdentifier", "MODEL-456",
                "additionalDetails", Map.of(
                        "gtinIdentifier", "5600413206256",
                        "somethingElse", "value"),
                "customField", "custom",
                "category", "DISPLAY"));
        EprelProduct product = objectMapper.readValue(json, EprelProduct.class);
        assertThat(product.getGtinIdentifier()).isEqualTo("5600413206256");
        assertThat(product.getNumericGtin()).isEqualTo(5_600_413_206_256L);
        assertThat(product.getAdditionalDetails().getAttributes()).containsEntry("somethingElse", "value");
        assertThat(product.getCategorySpecificAttributes()).containsEntry("customField", "custom");
    }
}
