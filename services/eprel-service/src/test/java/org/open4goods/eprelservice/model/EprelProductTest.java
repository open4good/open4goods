package org.open4goods.eprelservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.open4goods.model.eprel.EprelProduct;

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
        assertThat(product.getEprelCategories()).containsExactly("MONITOR");
        assertThat(product.getEprelCategory()).isEqualTo("MONITOR");
    }

    @Test
    @DisplayName("Dates described as integer years should be converted to epoch seconds (1st Jan)")
    void shouldConvertDatesFromIntegerYear() throws Exception
    {
        String json = "{" +
                "\"onMarketStartDate\":2024" +
                "}";
        EprelProduct product = objectMapper.readValue(json, EprelProduct.class);
        long expectedEpoch = LocalDate.of(2024, 1, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        assertThat(product.getOnMarketStartDate()).isEqualTo(expectedEpoch);
    }

    @Test
    @DisplayName("Dates described as large long/integers should be treated as epoch seconds")
    void shouldConvertDatesFromTimestamp() throws Exception
    {
        long timestamp = 1706659200L; // 2024-01-31
        String json = "{" +
                "\"onMarketStartDate\":" + timestamp + "" +
                "}";
        EprelProduct product = objectMapper.readValue(json, EprelProduct.class);
        assertThat(product.getOnMarketStartDate()).isEqualTo(timestamp);
    }


}
