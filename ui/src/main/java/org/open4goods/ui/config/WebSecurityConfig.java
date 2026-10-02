package org.open4goods.ui.config;

import java.util.Arrays;

import org.open4goods.model.RolesConstants;
import org.open4goods.ui.config.properties.UiGoogleSsoProperties;
import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({ ActuatorMonitorCredentials.class, UiGoogleSsoProperties.class })
public class WebSecurityConfig {

	private final ActuatorMonitorCredentials actuatorMonitorCredentials;

	private final UiGoogleSsoProperties googleSsoProperties;

	private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

	private @Autowired UiConfig config;

	public WebSecurityConfig(ActuatorMonitorCredentials actuatorMonitorCredentials,
			UiGoogleSsoProperties googleSsoProperties,
			ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
		this.actuatorMonitorCredentials = actuatorMonitorCredentials;
		this.googleSsoProperties = googleSsoProperties;
		this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.httpBasic(Customizer.withDefaults())
			.logout(Customizer.withDefaults())
			.authenticationProvider(new ActuatorMonitorAuthenticationProvider(actuatorMonitorCredentials))
			.csrf(AbstractHttpConfigurer::disable);

		if (config.getWebConfig().getWebAuthentication()) {
			http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/actuator/*").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/login/**", "/oauth2/**").permitAll()
				.requestMatchers("/").denyAll()
				.anyRequest().authenticated());
		} else {
			http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/actuator/*").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/").denyAll()
				.anyRequest().permitAll());
		}

		// Only wire Google SSO when a client registration is actually configured (beta/prod).
		// Without credentials (e.g. local/test), ui simply has no interactive login configured
		// yet - it never falls back to XWiki, and it never fails to boot.
		if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
			http.oauth2Login(oauth2 -> oauth2
					.userInfoEndpoint(userInfo -> userInfo.oidcUserService(uiOidcUserService())));
		}

		return http.build();
	}

	OAuth2UserService<OidcUserRequest, OidcUser> uiOidcUserService() {
		return new UiOidcUserService(googleSsoProperties);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		CorsConfiguration globalConfiguration = new CorsConfiguration();
		globalConfiguration.setAllowedOrigins(config.getWebConfig().getCorsAllowedHosts());
		globalConfiguration.setAllowedMethods(Arrays.asList("*"));
		globalConfiguration.setAllowedHeaders(Arrays.asList("*"));
		globalConfiguration.setAllowCredentials(true);
		source.registerCorsConfiguration("/**", globalConfiguration);

		return source;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
