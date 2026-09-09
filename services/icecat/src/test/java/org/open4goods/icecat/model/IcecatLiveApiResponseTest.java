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
}
