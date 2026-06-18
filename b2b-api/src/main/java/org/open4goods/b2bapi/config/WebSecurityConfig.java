package org.open4goods.b2bapi.config;

import org.open4goods.b2bapi.service.RbacAuthority;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Initial security posture for infrastructure endpoints.
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
            final ObjectProvider<DashboardJwtAuthenticationFilter> dashboardJwtAuthenticationFilter,
            final ObjectProvider<ApiKeyAuthFilter> apiKeyAuthFilter) throws Exception {
        apiKeyAuthFilter.ifAvailable(filter -> {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        });
        dashboardJwtAuthenticationFilter.ifAvailable(filter -> {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        });

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/redoc",
                                "/scalar",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/auth/**",
                                "/api/v1/customer/billing/catalog",
                                "/api/v1/billing/stripe/webhook",
                                "/api/v1/barcodes/assets/**",
                                "/api/v1/barcodes/check",
                                "/api/v1/catalog/stats")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAuthority(RbacAuthority.PLATFORM_ADMIN)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
