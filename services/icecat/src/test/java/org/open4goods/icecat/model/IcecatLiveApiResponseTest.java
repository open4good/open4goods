package org.open4goods.icecat.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Guards the live-API JSON model's independence from the bulk-XML JAXB contract: it must stay a
 * hand-tolerant Jackson model that survives Icecat adding fields, not one regenerated from the XSD.
 */
class IcecatLiveApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void ignoresUnknownFieldsAtTopLevelAndNested() {
        String json = """
                {
                  "msg": "OK",
                  "unexpectedTopLevelField": "should be ignored",
                  "data": {
                    "unexpectedNestedField": 42,
                    "GeneralInfo": {
                      "IcecatId": 1,
                      "Title": "Example",
                      "AnotherUnexpectedField": ["a", "b"]
                    },
                    "DemoAccount": false
                  }
                }
                """;

        assertThatCode(() -> mapper.readValue(json, IcecatLiveApiResponse.class)).doesNotThrowAnyException();
    }

    @Test
    void parsesKnownFieldsWhileIgnoringUnknownOnes() throws Exception {
        String json = """
                {
                  "msg": "OK",
                  "data": {
                    "aFutureIcecatField": true,
                    "GeneralInfo": {
                      "IcecatId": 42,
                      "Title": "Example Product"
                    }
                  }
                }
                """;

        IcecatLiveApiResponse response = mapper.readValue(json, IcecatLiveApiResponse.class);

        assertThat(response.msg).isEqualTo("OK");
        assertThat(response.data.generalInfo.icecatId).isEqualTo(42);
        assertThat(response.data.generalInfo.title).isEqualTo("Example Product");
    }

    /**
     * Shape sourced from Icecat's published JSON API manual, not a captured live response
     * (no by-id/Variants fixture is available offline) : proves the mapper parses this
     * documented shape, not that it matches production exactly.
     */
    @Test
    void parsesVariantsWithIdentifiersAndIgnoresUnmappedInnerArrays() throws Exception {
        String json = """
                {
                  "msg": "OK",
                  "data": {
                    "GeneralInfo": { "IcecatId": 1 },
                    "Variants": [
                      {
                        "VariantID": "v1",
                        "VariantIdentifiers": [
                          { "Identifier Type": "GTIN13", "Value": "1234567890123" }
                        ],
                        "VariantDescriptions": "Red edition",
                        "VariantFeatures": [{ "Unmapped": "ignored" }],
                        "VariantImages": [{ "Unmapped": "ignored" }],
                        "VariantMultimedia": [{ "Unmapped": "ignored" }]
                      }
                    ]
                  }
                }
                """;

        IcecatLiveApiResponse response = mapper.readValue(json, IcecatLiveApiResponse.class);

        assertThat(response.data.variants).hasSize(1);
        assertThat(response.data.variants.get(0).variantID).isEqualTo("v1");
        assertThat(response.data.variants.get(0).variantDescriptions).isEqualTo("Red edition");
        assertThat(response.data.variants.get(0).variantIdentifiers).hasSize(1);
        assertThat(response.data.variants.get(0).variantIdentifiers.get(0).identifierType).isEqualTo("GTIN13");
        assertThat(response.data.variants.get(0).variantIdentifiers.get(0).value).isEqualTo("1234567890123");
    }
}
