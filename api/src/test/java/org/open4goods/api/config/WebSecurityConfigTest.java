package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests API-key authentication behavior configured by {@link WebSecurityConfig}.
 */
class WebSecurityConfigTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void tokenFilterIgnoresUnknownTokens() throws Exception {
		ApiProperties properties = new ApiProperties();
		properties.setAdminKey("admin-key");
		properties.setTestKeys(List.of("test-key"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(UrlConstants.APIKEY_PARAMETER, "unknown-key");

		new WebSecurityConfig.TokenAuthenticationFilter(properties)
				.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void tokenFilterGrantsAdminAuthorityForAdminKey() throws Exception {
		ApiProperties properties = new ApiProperties();
		properties.setAdminKey("admin-key");
		properties.setTestKeys(List.of("test-key"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(UrlConstants.APIKEY_PARAMETER, " admin-key ");

		new WebSecurityConfig.TokenAuthenticationFilter(properties)
				.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.containsExactlyInAnyOrder(RolesConstants.ROLE_USER, RolesConstants.ROLE_ADMIN);
	}

	@Test
	void tokenFilterGrantsTesterAuthorityForConfiguredTestKey() throws Exception {
		ApiProperties properties = new ApiProperties();
		properties.setAdminKey("admin-key");
		properties.setTestKeys(List.of("test-key"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(UrlConstants.APIKEY_PARAMETER, "test-key");

		new WebSecurityConfig.TokenAuthenticationFilter(properties)
				.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.containsExactlyInAnyOrder(RolesConstants.ROLE_USER, RolesConstants.ROLE_TESTER);
	}

	// The dedicated Metriks key (GOU-1412)

	@Test
	void tokenFilterGrantsOnlyTheMetriksAuthorityForTheMetriksKeyOnAMetriksPath() throws Exception {
		Authentication authentication = authenticate(metriksProperties(), "/api/metriks/system", "", "metriks-key");

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.containsExactly(RolesConstants.ROLE_METRIKS);
	}

	@Test
	void tokenFilterChecksTheMetriksPathWithoutTheContextPath() throws Exception {
		assertThat(authenticate(metriksProperties(), "/ctx/api/metriks/seo", "/ctx", "metriks-key")).isNotNull();
		assertThat(authenticate(metriksProperties(), "/api/metriks/seo", "/ctx", "metriks-key")).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/", "/batch/clean-resources", "/actuator/health", "/v3/api-docs", "/api/metriks",
			"/api/metriks/", "/api/metriksX", "/api/metriksX/system", "/api/metriks/../batch/clean-resources",
			"/api/metriks/./system", "/api/metriks/%2e%2e/batch/clean-resources", "/api/metriks//system",
			"/api/metriks/system;jsessionid=x", "/api/metriks;x/system", "/x/api/metriks/system" })
	void tokenFilterIgnoresTheMetriksKeyOutsideTheMetriksPaths(String path) throws Exception {
		assertThat(authenticate(metriksProperties(), path, "", "metriks-key")).as(path).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  " })
	void tokenFilterAcceptsNoMetriksKeyWhenNoneIsConfigured(String configured) throws Exception {
		ApiProperties properties = metriksProperties();
		properties.setMetriksKey(configured.isEmpty() ? null : configured);

		assertThat(authenticate(properties, "/api/metriks/system", "", "metriks-key")).isNull();
		assertThat(authenticate(properties, "/api/metriks/system", "", configured)).isNull();
	}

	@Test
	void tokenFilterKeepsTheAdminAuthoritiesOfTheAdminKeyOnAMetriksPath() throws Exception {
		Authentication authentication = authenticate(metriksProperties(), "/api/metriks/system", "", "admin-key");

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.containsExactlyInAnyOrder(RolesConstants.ROLE_USER, RolesConstants.ROLE_ADMIN);
	}

	@Test
	void tokenFilterIgnoresAPrefixOfTheMetriksKey() throws Exception {
		assertThat(authenticate(metriksProperties(), "/api/metriks/system", "", "metriks-ke")).isNull();
		assertThat(authenticate(metriksProperties(), "/api/metriks/system", "", "metriks-key-x")).isNull();
	}

	private static ApiProperties metriksProperties() {
		ApiProperties properties = new ApiProperties();
		properties.setAdminKey("admin-key");
		properties.setTestKeys(List.of("test-key"));
		properties.setMetriksKey("metriks-key");
		return properties;
	}

	/** Runs the token filter on a request presenting the key in the header, and returns what it authenticated. */
	private static Authentication authenticate(ApiProperties properties, String requestUri, String contextPath,
			String key) throws Exception {
		SecurityContextHolder.clearContext();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
		request.setContextPath(contextPath);
		request.addHeader(UrlConstants.APIKEY_PARAMETER, key);

		new WebSecurityConfig.TokenAuthenticationFilter(properties)
				.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		return SecurityContextHolder.getContext().getAuthentication();
	}
}
