package org.open4goods.api.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.UrlConstants;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {


	private final ApiProperties apiProperties;

	private final AuthenticationProvider  authProvider;
	
	public WebSecurityConfig(ApiProperties apiProperties, AuthenticationProvider  authProvider) {
		this.apiProperties = apiProperties;
		this.authProvider = authProvider;
	}
	
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                // For Cors config, see the below corsConfigurationSource()
                .cors(withDefaults())
                .authorizeHttpRequests(requests -> requests
                        // Actuator endpoints are protected
                        .requestMatchers("/actuator").hasAuthority(RolesConstants.ACTUATOR_ADMIN_ROLE)
                        .requestMatchers("/actuator/**").hasAuthority(RolesConstants.ACTUATOR_ADMIN_ROLE))
				.formLogin(login -> login.permitAll().loginProcessingUrl("/login").defaultSuccessUrl("/",true))
				.logout(logout -> logout.permitAll())
                .csrf(c -> c.ignoringRequestMatchers("/actuator/**").disable())
                .addFilterBefore(new TokenAuthenticationFilter(apiProperties), BasicAuthenticationFilter.class)
                // Allowing directauth via http creds
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().authenticated());

		return http.build();
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
					// Populate SecurityContextHolder
					final User user = new User("api-key", "", true, true, true, true, authorities);
					final UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
					SecurityContextHolder.getContext().setAuthentication(authentication);
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


	/**
	 * Authentication manager
	 * @param http
	 * @return
	 * @throws Exception
	 */
	@Bean
	AuthenticationManager authManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder =	http.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.authenticationProvider(authProvider);
		return authenticationManagerBuilder.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
