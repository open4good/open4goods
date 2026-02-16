package org.open4goods.nudgerfrontapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationPartnersProperties;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the URL-encoding behaviour in {@link AffiliationPartnerService}.
 *
 * <p>The private helper {@code buildAssetUrl} is exercised via reflection to
 * verify that partner names containing spaces or special characters are
 * properly URL-encoded.</p>
 */
@ExtendWith(MockitoExtension.class)
class AffiliationPartnerServiceTest {

    @Mock
    private AffiliationPartnersProperties affiliationPartnersProperties;

    @Mock
    private AffiliationService affiliationService;

    private ApiProperties apiProperties;
    private AffiliationPartnerService service;
    private Method buildAssetUrl;

    @BeforeEach
    void setUp() throws Exception {
        apiProperties = new ApiProperties();
        apiProperties.setResourceRootPath("https://static.nudger.fr");

        // Stub the properties used during construction
        when(affiliationPartnersProperties.getApiBaseUrl()).thenReturn("https://api.open4goods.org");
        when(affiliationPartnersProperties.getApiKey()).thenReturn("dummy-key");

        service = new AffiliationPartnerService(
                RestClient.builder(), affiliationPartnersProperties, apiProperties, affiliationService);

        // Obtain access to the private buildAssetUrl method
        buildAssetUrl = AffiliationPartnerService.class.getDeclaredMethod(
                "buildAssetUrl", String.class, String.class);
        buildAssetUrl.setAccessible(true);
    }

    private String invoke(String pathSuffix, String partnerName) throws Exception {
        return (String) buildAssetUrl.invoke(service, pathSuffix, partnerName);
    }

    @Test
    void shouldEncodePartnerNameWithSpaces() throws Exception {
        String result = invoke("/logo/", "Boulanger Retail");
        assertNotNull(result);
        assertEquals("https://static.nudger.fr/logo/Boulanger+Retail", result);
    }

    @Test
    void shouldEncodePartnerNameWithAmpersand() throws Exception {
        String result = invoke("/logo/", "Moyenne & Co");
        assertNotNull(result);
        assertTrue(result.contains("%26"), "Ampersand should be encoded as %26");
        assertEquals("https://static.nudger.fr/logo/Moyenne+%26+Co", result);
    }

    @Test
    void shouldHandleSimplePartnerNameWithoutSpecialChars() throws Exception {
        String result = invoke("/logo/", "amazon.fr");
        assertNotNull(result);
        assertEquals("https://static.nudger.fr/logo/amazon.fr", result);
    }

    @Test
    void shouldReturnNullWhenPartnerNameIsBlank() throws Exception {
        String result = invoke("/logo/", "   ");
        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenResourceRootPathIsBlank() throws Exception {
        apiProperties.setResourceRootPath("");
        String result = invoke("/logo/", "partner");
        assertNull(result);
    }

    @Test
    void shouldStripTrailingSlashFromResourceRoot() throws Exception {
        apiProperties.setResourceRootPath("https://static.nudger.fr/");
        String result = invoke("/logo/", "amazon.fr");
        assertNotNull(result);
        assertEquals("https://static.nudger.fr/logo/amazon.fr", result);
    }

    @Test
    void shouldEncodePartnerNameInFaviconPath() throws Exception {
        String result = invoke("/favicon?url=", "Moyenne & Co");
        assertNotNull(result);
        assertEquals("https://static.nudger.fr/favicon?url=Moyenne+%26+Co", result);
    }
}
