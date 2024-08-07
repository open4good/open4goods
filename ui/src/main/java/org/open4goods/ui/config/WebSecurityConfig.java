package org.open4goods.ui.config;

import java.util.Arrays;

import org.open4goods.model.constants.RolesConstants;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.webextention.WebExtensionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
/**
 * HTTP Security configuration for the UI component
 * @author Goulven.Furet
 *
 */
public class WebSecurityConfig {


	private final AuthenticationProvider  authProvider;
	
	private @Autowired UiConfig config;

	public WebSecurityConfig(AuthenticationProvider authProvider) {
		this.authProvider = authProvider;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		///////////////////////////
		// Shared configuration
		//////////////////////////
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
				.and().csrf(c -> c.ignoringRequestMatchers("/actuator/**").disable());

		
		if (config.getWebConfig().getWebAuthentication().booleanValue()) {
			///////////////////////////
			// If a full protected  webapp configuration
			//////////////////////////
			http.authorizeRequests().anyRequest().authenticated();
		} else {
			///////////////////////////
			// If a open protected  webapp configuration
			//////////////////////////
			http.authorizeRequests().anyRequest().permitAll();
		}
		return http.build();
	}
	
	
	
	/**
	 * Cors configuration
	 * @return
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		

		// Specific CORS configuration for webextension endpoints
		CorsConfiguration webExtConfig = new CorsConfiguration();
		webExtConfig.setAllowedOriginPatterns(Arrays.asList("*")) ;
		webExtConfig.setAllowedMethods(Arrays.asList("*"));
		webExtConfig.setAllowedHeaders(Arrays.asList("*"));
		webExtConfig.setAllowCredentials(true);
		source.registerCorsConfiguration(WebExtensionController.WEBEXTENSION_ENDPOINT, webExtConfig);
		source.registerCorsConfiguration(WebExtensionController.WEBEXTENSION_EXISTS_ENDPOINT, webExtConfig);

		// Global cors configuration, with specific allowed hosts
	    CorsConfiguration globalConfiguration = new CorsConfiguration();
	    globalConfiguration.setAllowedOrigins(config.getWebConfig().getCorsAllowedHosts()) ;
	    globalConfiguration.setAllowedMethods(Arrays.asList("*"));
	    globalConfiguration.setAllowedHeaders(Arrays.asList("*"));
	    globalConfiguration.setAllowCredentials(true);
	    source.registerCorsConfiguration("/**", globalConfiguration);

	    
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