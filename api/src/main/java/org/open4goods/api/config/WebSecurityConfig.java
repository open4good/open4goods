package org.open4goods.api.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.config.http.SessionCreationPolicy;
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
		.cors()
		// Allowing directauth via http creds
		.and().httpBasic(Customizer.withDefaults())
		.authorizeRequests()
		// Actuator endpoints are protected
		.requestMatchers("/actuator").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
		.requestMatchers("/actuator/*").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
		//  login and logout are allowed
		.and().formLogin().permitAll()
		.and().logout().permitAll()
		// CSRF is disabled for actuator endpoints
		.and().csrf(c -> c.ignoringRequestMatchers("/actuator/**").disable())
		.addFilterBefore(new TokenAuthenticationFilter(), BasicAuthenticationFilter.class)

		.authorizeRequests()
		.anyRequest().authenticated();

		return http.build();
	}

	/**
	 * This filter is used to allow external crawlers authentication, through a specific set of keys 
	 */
	public class TokenAuthenticationFilter extends GenericFilterBean {

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)	throws IOException, ServletException {

			final HttpServletRequest httpRequest = (HttpServletRequest) request;

			// Extracting token prom parameter
			String accessToken = httpRequest.getParameter(UrlConstants.APIKEY_PARAMETER);
			if (null == accessToken) {
				accessToken = httpRequest.getHeader (UrlConstants.APIKEY_PARAMETER);
			}


			if (null != accessToken) {

				// By default, we provide a USER role
				final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
				authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_USER));

				// Assignation of the ROLE_ADMIN
				if (accessToken.equals(apiProperties.getAdminKey())) {
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_ADMIN));
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_CRAWLER));
				}

				// Assignation of the ROLE_CRAWLER
				if (apiProperties.getCrawlerKeys().contains(accessToken) ) {
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_CRAWLER));
				}

				// Assignation of the ROLE_CRAWLER
				if (apiProperties.getTestKeys().contains(accessToken) ) {
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_TESTER));
				}

				// Populate SecurityContextHolder
				final User user = new User("username", "password", true, true, true, true, authorities);
				
				final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}


			// Continue request handling
			chain.doFilter(request, response);
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