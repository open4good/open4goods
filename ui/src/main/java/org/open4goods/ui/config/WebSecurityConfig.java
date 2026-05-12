package org.open4goods.ui.config;

import java.util.Arrays;

import org.open4goods.model.RolesConstants;
import org.open4goods.ui.config.yml.UiConfig;
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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

	private final AuthenticationProvider authProvider;

	private @Autowired UiConfig config;

	public WebSecurityConfig(AuthenticationProvider authProvider) {
		this.authProvider = authProvider;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.httpBasic(Customizer.withDefaults())
			.formLogin(Customizer.withDefaults())
			.logout(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable);

		if (config.getWebConfig().getWebAuthentication()) {
			http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/actuator/*").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/").denyAll()
				.anyRequest().authenticated());
		} else {
			http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/actuator/*").hasRole(RolesConstants.ACTUATOR_ADMIN_ROLE)
				.requestMatchers("/").denyAll()
				.anyRequest().permitAll());
		}

		return http.build();
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
	AuthenticationManager authManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.authenticationProvider(authProvider);
		return authenticationManagerBuilder.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
