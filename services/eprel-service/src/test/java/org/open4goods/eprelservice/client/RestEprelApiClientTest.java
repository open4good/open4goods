package org.open4goods.eprelservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.open4goods.eprelservice.config.EprelServiceProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

/**
 * Tests for {@link RestEprelApiClient}.
 */
class RestEprelApiClientTest
{
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private EprelServiceProperties properties;
    private RestEprelApiClient client;

    @BeforeEach
    void setUp()
    {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        properties = new EprelServiceProperties();
        properties.setApiKey("test-key");
        properties.setApiUrl("https://example.com/api");
        client = new RestEprelApiClient(restTemplate, properties);
    }

    @AfterEach
    void tearDown()
    {
        server.verify();
    }

    @Test
    @DisplayName("fetchProductGroups should retrieve and deserialize groups")
    void fetchProductGroupsShouldReturnEntries()
    {
        String response = "[{\"code\":\"A\",\"url_code\":\"group\",\"name\":\"Group\",\"regulation\":\"REG\"}]";
        server.expect(MockRestRequestMatchers.requestTo("https://example.com/api/product-groups"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess(response, MediaType.APPLICATION_JSON));
        List<EprelProductGroup> groups = client.fetchProductGroups();
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).urlCode()).isEqualTo("group");
    }

    @Test
    @DisplayName("downloadCatalogueZip should download the ZIP file and store it locally")
    void downloadCatalogueZipShouldWriteFile() throws IOException
    {
        byte[] archive = createZipArchive();
        server.expect(MockRestRequestMatchers.requestTo("https://example.com/api/exportProducts/group"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header("x-api-key", "test-key"))
                .andRespond(MockRestResponseCreators.withSuccess(archive, MediaType.APPLICATION_OCTET_STREAM));
        Path path = client.downloadCatalogueZip("group");
        try
        {
            assertThat(Files.exists(path)).isTrue();
            assertThat(Files.size(path)).isEqualTo(archive.length);
        }
        finally
        {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @DisplayName("downloadCatalogueZip should raise an exception when status is not successful")
    void downloadCatalogueZipShouldFailOnError()
    {
        server.expect(MockRestRequestMatchers.requestTo("https://example.com/api/exportProducts/group"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> client.downloadCatalogueZip("group")).isInstanceOf(IOException.class);
    }

    private byte[] createZipArchive() throws IOException
    {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(baos))
        {
            zipOutputStream.putNextEntry(new ZipEntry("file.txt"));
            zipOutputStream.write("content".getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.finish();
            return baos.toByteArray();
        }
    }
}
