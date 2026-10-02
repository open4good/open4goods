package org.open4goods.sbadmin.config;

import org.open4goods.sbadmin.config.properties.AdminGoogleSsoProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import de.codecentric.boot.admin.server.config.AdminServerProperties;

/**
 * HTTP Security configuration.
 * <p>
 * Interactive login is Google SSO (Authorization Code via Spring Security's {@code oauth2Login},
 * deny-by-default email allowlist), replacing the former XWiki password login per ADR-0009.
 *
 * @author Goulven.Furet
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({ SbaRegistrationCredentials.class, AdminGoogleSsoProperties.class })
public class WebSecurityConfig {

	// We bind on a role carried by the Google SSO allowlist (mirrors the former XWiki admin group).
	private static final String REQUIRED_ROLE = "XWIKIADMINGROUP";

	private final AdminServerProperties adminServer;

	private final SbaRegistrationCredentials registrationCredentials;

	private final AdminGoogleSsoProperties googleSsoProperties;

	private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

	public WebSecurityConfig(AdminServerProperties adminServer, SbaRegistrationCredentials registrationCredentials,
			AdminGoogleSsoProperties googleSsoProperties,
			ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
		this.adminServer = adminServer;
		this.registrationCredentials = registrationCredentials;
		this.googleSsoProperties = googleSsoProperties;
		this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
	}

	//////////////////////////////////////////////
	// The WebSecurity configuration
	//////////////////////////////////////////////

	@Bean
	protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
		successHandler.setTargetUrlParameter("redirectTo");
		successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");

		http
				.authenticationProvider(new SbaRegistrationAuthenticationProvider(registrationCredentials))
				.authorizeHttpRequests(
						req -> req.requestMatchers(this.adminServer.getContextPath() + "/assets/**").permitAll()
								.requestMatchers(this.adminServer.getContextPath() + "/login").permitAll()
								.requestMatchers(this.adminServer.getContextPath() + "/login/**").permitAll()
								.requestMatchers(this.adminServer.getContextPath() + "/oauth2/**").permitAll()
								.anyRequest().hasRole(REQUIRED_ROLE))
				.logout((logout) -> logout.logoutUrl(this.adminServer.getContextPath() + "/logout"))
				.httpBasic(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable());

		// Only wire Google SSO when a client registration is actually configured (beta/prod).
		// Without credentials (e.g. local/test), the admin console simply has no interactive
		// login configured yet - it never falls back to XWiki, and it never fails to boot.
		if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
			http.oauth2Login(oauth2 -> oauth2
					.loginPage(this.adminServer.getContextPath() + "/login")
					.userInfoEndpoint(userInfo -> userInfo.oidcUserService(adminOidcUserService()))
					.successHandler(successHandler));
		}

		return http.build();

	}

	OAuth2UserService<OidcUserRequest, OidcUser> adminOidcUserService() {
		return new AdminOidcUserService(googleSsoProperties);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
