package org.open4goods.nudgerfrontapi.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.LocaleResolver;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import io.jsonwebtoken.security.Keys;

/**
 * Web security configuration for the frontend API.
 */

@Configuration
@EnableWebSecurity
// Security configuration adapted for the Nuxt frontend application.

public class WebSecurityConfig {

    private final SecurityProperties securityProperties;
    private final AuthenticationProvider authenticationProvider;

    public WebSecurityConfig(SecurityProperties securityProperties,
                             AuthenticationProvider authenticationProvider) {
        this.securityProperties = securityProperties;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey key = new SecretKeySpec(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(key);
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    /**
     * JWT decoder using the same shared secret.
     */
    JwtDecoder jwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, LocaleResolver localeResolver,
                                                   SharedTokenFilter sharedTokenFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .setSharedObject(LocaleResolver.class, localeResolver);

        if (securityProperties.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(SharedTokenFilter.PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated())
                .addFilterBefore(sharedTokenFilter, BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.authenticationProvider(authenticationProvider);
        return builder.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(securityProperties.getCorsAllowedHosts());
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
