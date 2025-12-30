package org.open4goods.services.exposeddocs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the exposed docs microservice.
 * Allows disabling authentication entirely via configuration.
 */
@Configuration
@EnableWebSecurity
public class ExposedDocsSecurityConfig
{

    private final ExposedDocsSecurityProperties securityProperties;

    public ExposedDocsSecurityConfig(ExposedDocsSecurityProperties securityProperties)
    {
        this.securityProperties = securityProperties;
    }

    /**
     * Configures HTTP security rules for the exposed docs endpoints.
     *
     * @param http http security builder
     * @return configured security filter chain
     * @throws Exception when security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http.csrf(AbstractHttpConfigurer::disable);

        if (securityProperties.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults());
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }
}
