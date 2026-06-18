package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
}
