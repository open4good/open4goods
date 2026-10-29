package org.open4goods.api.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.open4goods.api.config.yml.ActuatorMonitorCredentials;
import org.open4goods.api.config.yml.ApiGoogleSsoProperties;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP security of api (GOU-1392, GOU-1393).
 * <ul>
 * <li>Machines authenticate with an API key ({@code Authorization} header or parameter holding the raw
 * key): {@code admin-key} grants {@code ROLE_USER} and {@code XWIKIADMINGROUP}, a test key
 * {@code ROLE_USER} and {@code ROLE_TESTER}.</li>
 * <li>People log in interactively with Google SSO only ({@code oauth2Login}, wired when a {@code google}
 * client registration exists): verified email, deny-by-default allowlist
 * {@code api.security.google-sso.role-allowlist} ({@link ApiOidcUserService}). There is no login page,
 * no password form and no generated {@code user}: {@code /login} is protected like any page.</li>
 * <li>HTTP Basic exists only on {@code /actuator/**}, for the Spring Boot Admin monitor identity
 * ({@code o4g.actuator.monitor.*}, {@link ActuatorMonitorAuthenticationProvider}).</li>
 * <li>An anonymous browser navigation is redirected to Google; any other anonymous call gets a bare 401,
 * without a {@code WWW-Authenticate: Basic} challenge that would open a browser popup.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({ ApiGoogleSsoProperties.class, ActuatorMonitorCredentials.class })
public class WebSecurityConfig {

	/** Spring Security's authorization request of the {@code google} client registration. */
	static final String GOOGLE_REGISTRATION_ID = "google";
	static final String GOOGLE_AUTHORIZATION_PATH = "/oauth2/authorization/" + GOOGLE_REGISTRATION_ID;

	/** Authorities of an API key or an allowlisted Google identity: one of them is required outside Actuator. */
	private static final String[] API_AUTHORITIES = { RolesConstants.ROLE_USER, RolesConstants.ROLE_ADMIN,
			RolesConstants.ROLE_TESTER, RolesConstants.ROLE_XWIKI_ALL };

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfig.class);

	private final ApiProperties apiProperties;

	private final ApiGoogleSsoProperties googleSsoProperties;

	private final ActuatorMonitorCredentials actuatorMonitorCredentials;

	private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

	public WebSecurityConfig(ApiProperties apiProperties, ApiGoogleSsoProperties googleSsoProperties,
			ActuatorMonitorCredentials actuatorMonitorCredentials,
			ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider) {
		this.apiProperties = apiProperties;
		this.googleSsoProperties = googleSsoProperties;
		this.actuatorMonitorCredentials = actuatorMonitorCredentials;
		this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
	}

	/**
	 * Actuator: the admin API key, an allowlisted Google session holding {@code XWIKIADMINGROUP}, or the
	 * Spring Boot Admin monitor through HTTP Basic. No other Basic identity exists.
	 */
	@Bean
	@Order(1)
	SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/actuator", "/actuator/**")
				.cors(withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.authenticationProvider(new ActuatorMonitorAuthenticationProvider(actuatorMonitorCredentials))
				.addFilterBefore(new TokenAuthenticationFilter(apiProperties), BasicAuthenticationFilter.class)
				// A refused Basic login gets the same bare 401 as an anonymous call: no challenge, no popup.
				.httpBasic(basic -> basic.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(requests -> requests
						.anyRequest().hasAnyAuthority(RolesConstants.ACTUATOR_ADMIN_ROLE,
								ActuatorMonitorAuthenticationProvider.REQUIRED_ROLE));
		configureAnonymousEntry(http);
		return http.build();
	}

	/** Everything else: an API key or an allowlisted Google session. */
	@Bean
	@Order(2)
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// For Cors config, see the below corsConfigurationSource()
				.cors(withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.addFilterBefore(new TokenAuthenticationFilter(apiProperties), BasicAuthenticationFilter.class)
				.authorizeHttpRequests(requests -> requests
						// The error page of a refused request must not trigger a new login (redirect loop).
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.anyRequest().hasAnyAuthority(API_AUTHORITIES))
				.logout(logout -> logout.permitAll());

		if (isGoogleSsoConfigured()) {
			checkAllowlist();
			http.oauth2Login(oauth2 -> oauth2
					.userInfoEndpoint(userInfo -> userInfo.oidcUserService(new ApiOidcUserService(googleSsoProperties)))
					.failureHandler(loginFailureHandler()));
		}
		configureAnonymousEntry(http);
		return http.build();
	}

	/**
	 * An anonymous browser navigation goes to Google (when configured), and only it is remembered to come
	 * back after login; any other anonymous call gets a bare 401 and creates no session.
	 */
	private void configureAnonymousEntry(HttpSecurity http) throws Exception {
		RequestMatcher browserNavigation = browserNavigationMatcher();
		HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
		requestCache.setRequestMatcher(browserNavigation);
		http
				.requestCache(cache -> cache.requestCache(requestCache))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(anonymousEntryPoint(browserNavigation)));
	}

	private AuthenticationEntryPoint anonymousEntryPoint(RequestMatcher browserNavigation) {
		AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
		if (!isGoogleSsoConfigured()) {
			return unauthorized;
		}
		return DelegatingAuthenticationEntryPoint.builder()
				.addEntryPointFor(new LoginUrlAuthenticationEntryPoint(GOOGLE_AUTHORIZATION_PATH), browserNavigation)
				.defaultEntryPoint(unauthorized)
				.build();
	}

	/** A top-level GET of a browser: accepts {@code text/html} explicitly ({@code *}{@code /*} alone does not count), not XHR. */
	static RequestMatcher browserNavigationMatcher() {
		MediaTypeRequestMatcher html = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
		html.setIgnoredMediaTypes(Set.of(MediaType.ALL));
		return new AndRequestMatcher(
				PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/**"),
				html,
				new NegatedRequestMatcher(new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")));
	}

	/**
	 * A Google identity refused by {@link ApiOidcUserService} gets a 403, any other login error a 401. Never a
	 * redirect: there is no login page, and a redirect to Google could loop on a persistent error.
	 */
	static AuthenticationFailureHandler loginFailureHandler() {
		return (request, response, exception) -> {
			if (exception instanceof OAuth2AuthenticationException oauth2Exception
					&& ApiOidcUserService.REFUSAL_CODES.contains(oauth2Exception.getError().getErrorCode())) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			LOGGER.warn("Google SSO login failed (401): {}", exception instanceof OAuth2AuthenticationException oauth2Exception
					? oauth2Exception.getError().getErrorCode() : exception.getClass().getSimpleName());
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		};
	}

	private boolean isGoogleSsoConfigured() {
		ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();
		return repository != null && repository.findByRegistrationId(GOOGLE_REGISTRATION_ID) != null;
	}

	/** Reports, without failing the startup, an allowlist that cannot match any login. */
	private void checkAllowlist() {
		if (googleSsoProperties.getRoleAllowlist().isEmpty()) {
			LOGGER.warn("Google SSO is configured but api.security.google-sso.role-allowlist is empty: "
					+ "every Google login will be refused.");
			return;
		}
		List<String> malformed = googleSsoProperties.malformedAllowlistKeys();
		if (!malformed.isEmpty()) {
			LOGGER.error("Google SSO role allowlist has {} key(s) that are not e-mail addresses ({}): these entries "
					+ "never match a login. Spring Boot drops the '@' of an unbracketed map key; write each key of "
					+ "api.security.google-sso.role-allowlist with brackets and quotes, e.g. \"[adresse@domaine]\": "
					+ "[ROLE_USER].", malformed.size(),
					malformed.stream().map(key -> key.isEmpty() ? "" : key.charAt(0) + "***").sorted().toList());
		}
	}

	/**
	 * This filter allows token-based API authentication.
	 */
	static final class TokenAuthenticationFilter extends GenericFilterBean {

		private final ApiProperties apiProperties;

		TokenAuthenticationFilter(ApiProperties apiProperties) {
			this.apiProperties = apiProperties;
		}

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)	throws IOException, ServletException {

			final HttpServletRequest httpRequest = (HttpServletRequest) request;

			final String accessToken = resolveAccessToken(httpRequest);


			if (null != accessToken) {

				final List<SimpleGrantedAuthority> authorities = resolveAuthorities(accessToken);

				if (!authorities.isEmpty()) {
					// Populate SecurityContextHolder with a new context, never the one of a Google session.
					final User user = new User("api-key", "", true, true, true, true, authorities);
					final UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
					final SecurityContext context = SecurityContextHolder.createEmptyContext();
					context.setAuthentication(authentication);
					SecurityContextHolder.setContext(context);
				}
			}


			// Continue request handling
			chain.doFilter(request, response);
		}

		private String resolveAccessToken(HttpServletRequest httpRequest) {
			// Extract token from request parameter first for backward compatibility, then from header.
			String accessToken = httpRequest.getParameter(UrlConstants.APIKEY_PARAMETER);
			if (null == accessToken) {
				accessToken = httpRequest.getHeader(UrlConstants.APIKEY_PARAMETER);
			}
			if (null == accessToken || accessToken.isBlank()) {
				return null;
			}
			return accessToken.trim();
		}

		private List<SimpleGrantedAuthority> resolveAuthorities(String accessToken) {
			final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
			final boolean adminKey = Objects.equals(accessToken, apiProperties.getAdminKey());
			final boolean testKey = apiProperties.getTestKeys() != null && apiProperties.getTestKeys().contains(accessToken);

			if (adminKey || testKey) {
				authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_USER));
			}
			if (adminKey) {
				authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_ADMIN));
			}
			if (testKey) {
				authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_TESTER));
			}

			return authorities;
		}
	}


	/**
	 * Cors configuration
	 * @return
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration configuration = new CorsConfiguration();
	    configuration.setAllowedOrigins(apiProperties.getCorsAllowedHosts());
	    configuration.setAllowedMethods(Arrays.asList("*"));
	    configuration.setAllowedHeaders(Arrays.asList("*"));
	    configuration.setAllowCredentials(true);
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", configuration);
	    return source;
	}

}
