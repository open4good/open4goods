package org.open4goods.ui.config;

import org.open4goods.services.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
/**
 * HTTP Security configuration
 * @author Goulven.Furet
 *
 */
public class WebConfig {

    private @Autowired AuthenticationProvider  authProvider;
    private @Autowired VerticalsConfigService verticalConfig;

    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.authorizeRequests()			
//		.anyRequest().permitAll()
		.anyRequest().authenticated()
		
		
		.and().formLogin().permitAll()
		.and().logout().permitAll();
		
		http.csrf().disable();
		http.headers().frameOptions().disable();
		
		return http.build();
	}

    
    
    @Bean
    AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}