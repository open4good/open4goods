package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.model.RolesConstants;
import org.open4goods.services.eprelservice.repository.EprelProductRepository;
import org.open4goods.services.productrepository.repository.ElasticProductRepository;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.prompt.service.provider.GeminiProvider;
import org.open4goods.services.prompt.service.provider.ProviderRegistry;
import org.open4goods.services.wikidataservice.repository.WikidataEntityRepository;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ObservationAuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shows, on the whole api context served by Tomcat, that api is "pure Google SSO" for people (GOU-1392, GOU-1393):
 * no password form and no generated user, an anonymous browser goes to Google, an anonymous call gets a 401 without
 * Basic challenge, only allowlisted Google identities get in, while the machine accesses keep working (API keys,
 * Spring Boot Admin monitor through HTTP Basic on Actuator only). Real HTTP requests go through Tomcat, so the
 * {@code NATIVE} forwarded headers strategy is the production one.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration," +
                "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration," +
                "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration," +
                "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration," +
                "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration," +
                "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration," +
                "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration",
        "server.address=127.0.0.1",
        "spring.security.oauth2.client.registration.google.client-id=api-sso-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=api-sso-client-secret",
        "api.security.google-sso.role-allowlist[owner@example.com][0]=" + RolesConstants.ROLE_USER,
        "api.security.google-sso.role-allowlist[owner@example.com][1]=" + RolesConstants.ROLE_ADMIN,
        "admin-key=" + ApiGoogleSsoAccessTest.ADMIN_KEY,
        "test-keys[0]=" + ApiGoogleSsoAccessTest.TEST_KEY,
        "o4g.actuator.monitor.username=" + ApiGoogleSsoAccessTest.MONITOR_USER,
        "o4g.actuator.monitor.password=" + ApiGoogleSsoAccessTest.MONITOR_PASSWORD,
        // Access is under test, not health: a contributor DOWN without Elasticsearch must not turn it into a 503.
        "management.endpoint.health.status.http-mapping.down=200",
        "management.endpoint.health.status.http-mapping.out-of-service=200" })
class ApiGoogleSsoAccessTest {

    static final String ADMIN_KEY = "admin-key-for-tests";
    static final String TEST_KEY = "test-key-for-tests";
    static final String MONITOR_USER = "o4g";
    static final String MONITOR_PASSWORD = "monitor-password-for-tests";

    private static final String BROWSER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ADMIN_ENDPOINT = "/batch/clean-resources";
    private static final String SWAGGER_UI = "/swagger-ui/index.html";

    @MockitoBean private BrandService brandService;
    @MockitoBean private ElasticProductRepository elasticProductRepository;
    @MockitoBean private EprelProductRepository eprelProductRepository;
    @MockitoBean private IcecatFeatureRepository icecatFeatureRepository;
    @MockitoBean private IcecatFeatureGroupRepository icecatFeatureGroupRepository;
    @MockitoBean private IcecatCategoryRepository icecatCategoryRepository;
    @MockitoBean private IcecatSupplierRepository icecatSupplierRepository;
    @MockitoBean private WikidataEntityRepository wikidataEntityRepository;
    @MockitoBean private ElasticsearchOperations elasticsearchOperations;
    @MockitoBean private GeminiProvider geminiProvider;
    @MockitoBean private PromptService promptService;
    @MockitoBean private ProviderRegistry providerRegistry;
    @MockitoBean private OpenAiChatModel openAiChatModel;
    @MockitoBean private GoogleGenAiChatModel googleGenAiChatModel;

    @LocalServerPort
    private int port;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClientRegistrationRepository clientRegistrations;

    @Autowired
    private FilterChainProxy filterChainProxy;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // People: Google SSO only

    @Test
    void thereIsNoPasswordLoginAndNoGeneratedUser() throws Exception {
        // /login is no form: a browser goes to Google, a password posted to it is not processed.
        HttpResponse<String> login = send(request("/login").header("Accept", BROWSER_ACCEPT));
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).endsWith(WebSecurityConfig.GOOGLE_AUTHORIZATION_PATH));
        assertThat(login.body()).doesNotContain("password");

        HttpResponse<String> postedPassword = send(request("/login")
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("username=user&password=anything")));
        assertThat(postedPassword.statusCode()).isEqualTo(401);

        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }

    @Test
    void anAnonymousBrowserIsSentToGoogle() throws Exception {
        HttpResponse<String> response = send(request(SWAGGER_UI).header("Accept", BROWSER_ACCEPT));

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location"))
                .hasValue("http://127.0.0.1:" + port + WebSecurityConfig.GOOGLE_AUTHORIZATION_PATH);
    }

    @Test
    void anAnonymousCallGetsA401WithoutBasicChallenge() throws Exception {
        for (String path : List.of("/", SWAGGER_UI, "/v3/api-docs", ADMIN_ENDPOINT, "/actuator/health")) {
            for (String accept : List.of("*/*", MediaType.APPLICATION_JSON_VALUE)) {
                HttpResponse<String> response = send(request(path).header("Accept", accept));

                assertThat(response.statusCode()).as("%s, Accept %s", path, accept).isEqualTo(401);
                assertThat(response.headers().firstValue("WWW-Authenticate")).as("%s, Accept %s", path, accept)
                        .isEmpty();
                assertThat(response.headers().firstValue("Set-Cookie")).as("no session for %s", path).isEmpty();
            }
        }
    }

    @Test
    void googleLoginGoesThroughTheAllowlist() {
        // openid in the scopes: Spring Security takes the OIDC path, whose user service is the allowlist.
        assertThat(clientRegistrations.findByRegistrationId("google").getScopes()).contains("openid");
        assertThat(oidcUserServices()).singleElement().isInstanceOf(ApiOidcUserService.class);
        // No mapper can add an authority to a Google identity behind the allowlist's back.
        assertThat(context.getBeanNamesForType(GrantedAuthoritiesMapper.class)).isEmpty();
    }

    @Test
    void aGoogleAccountOutsideTheAllowlistIsForbidden() throws Exception {
        // What a verified Google user holds without the allowlist: Google's own authorities only.
        mockMvc.perform(get(SWAGGER_UI).accept(MediaType.TEXT_HTML)
                        .with(oidcLogin().idToken(token -> token.claim("email", "stranger@example.com")
                                .claim("email_verified", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAllowlistedAccountOpensSwaggerUi() throws Exception {
        mockMvc.perform(get(SWAGGER_UI).accept(MediaType.TEXT_HTML)
                        .with(oidcLogin().idToken(token -> token.claim("email", "owner@example.com")
                                .claim("email_verified", true))
                                .authorities(new SimpleGrantedAuthority(RolesConstants.ROLE_USER),
                                        new SimpleGrantedAuthority(RolesConstants.ROLE_ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void theRedirectUriBehindTheProxyIsTheHttpsPublicOne() throws Exception {
        HttpResponse<String> entry = send(request(SWAGGER_UI).header("Accept", BROWSER_ACCEPT)
                .header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "api.example.org"));
        assertThat(entry.headers().firstValue("Location"))
                .hasValue("https://api.example.org" + WebSecurityConfig.GOOGLE_AUTHORIZATION_PATH);

        HttpResponse<String> authorization = send(request(WebSecurityConfig.GOOGLE_AUTHORIZATION_PATH)
                .header("X-Forwarded-Proto", "https").header("X-Forwarded-Host", "api.example.org"));

        assertThat(authorization.statusCode()).isEqualTo(302);
        String location = authorization.headers().firstValue("Location").orElseThrow();
        assertThat(location).startsWith("https://accounts.google.com/");
        assertThat(URLDecoder.decode(location, StandardCharsets.UTF_8))
                .contains("redirect_uri=https://api.example.org/login/oauth2/code/google")
                .contains("client_id=api-sso-client-id");
    }

    // Machines: API keys and the Spring Boot Admin monitor

    @Test
    void theAdminKeyOpensAProtectedEndpointAndActuator() throws Exception {
        assertThat(send(request(ADMIN_ENDPOINT).header("Authorization", ADMIN_KEY)).statusCode()).isEqualTo(200);
        assertThat(send(request("/actuator/health").header("Authorization", ADMIN_KEY)).statusCode()).isEqualTo(200);
    }

    @Test
    void aTestKeyKeepsItsAccessesWithoutTheAdminOnes() throws Exception {
        assertThat(send(request("/v3/api-docs").header("Authorization", TEST_KEY)).statusCode()).isEqualTo(200);
        assertThat(send(request(ADMIN_ENDPOINT).header("Authorization", TEST_KEY)).statusCode()).isEqualTo(403);
    }

    @Test
    void theMonitorReadsActuatorThroughBasic() throws Exception {
        HttpResponse<String> response = send(request("/actuator/health")
                .header("Authorization", basic(MONITOR_USER, MONITOR_PASSWORD)));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\"");
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void noOtherBasicIdentityExists() throws Exception {
        for (String authorization : List.of(basic("user", "anything"), basic(MONITOR_USER, "wrong-password"))) {
            HttpResponse<String> response = send(request("/actuator/health").header("Authorization", authorization));
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
        }
    }

    @Test
    void theMonitorIsLimitedToActuator() throws Exception {
        HttpResponse<String> response = send(request("/v3/api-docs")
                .header("Authorization", basic(MONITOR_USER, MONITOR_PASSWORD)));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String basic(String username, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /** The user services of the OIDC providers that the oauth2Login filters authenticate with. */
    private List<Object> oidcUserServices() {
        List<Object> services = new ArrayList<>();
        filterChainProxy.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(filter -> (AuthenticationManager) ReflectionTestUtils.getField(filter, "authenticationManager"))
                .forEach(manager -> collectOidcUserServices(manager, services));
        return services;
    }

    private static void collectOidcUserServices(AuthenticationManager manager, List<Object> services) {
        if (manager instanceof ObservationAuthenticationManager) {
            collectOidcUserServices((AuthenticationManager) ReflectionTestUtils.getField(manager, "delegate"), services);
            return;
        }
        if (!(manager instanceof ProviderManager providerManager)) {
            return;
        }
        for (AuthenticationProvider provider : providerManager.getProviders()) {
            if (provider instanceof OidcAuthorizationCodeAuthenticationProvider) {
                services.add(ReflectionTestUtils.getField(provider, "userService"));
            }
        }
        collectOidcUserServices((AuthenticationManager) ReflectionTestUtils.getField(providerManager, "parent"),
                services);
    }
}
