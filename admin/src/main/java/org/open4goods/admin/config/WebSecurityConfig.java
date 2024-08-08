package org.open4goods.admin.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.xwiki.XWikiServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
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
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import de.codecentric.boot.admin.server.config.AdminServerProperties;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
/**
 * HTTP Security configuration
 * 
 * @author Goulven.Furet
 *
 */
public class WebSecurityConfig {

	private final AdminServerProperties adminServer;

	private final AuthenticationProvider authProvider;

	public WebSecurityConfig(AuthenticationProvider authProvider, AdminServerProperties adminServer) {
		this.authProvider = authProvider;
	    this.adminServer = adminServer;
	}

	//////////////////////////////////////////////
	// The WebSecurity configuration
	//////////////////////////////////////////////
	
	 @Bean
     protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");

        http.authorizeHttpRequests(req -> req.requestMatchers(this.adminServer.getContextPath() + "/assets/**")
                .permitAll()
                .requestMatchers(this.adminServer.getContextPath() + "/login")
                .permitAll()
                .anyRequest()
                .authenticated())
            .formLogin(formLogin -> formLogin.loginPage(this.adminServer.getContextPath() + "/login")
                .successHandler(successHandler))
            .logout((logout) -> logout.logoutUrl(this.adminServer.getContextPath() + "/logout"))
            .httpBasic(Customizer.withDefaults())
            .csrf().disable();
        return http.build();

	}

	@Bean
	AuthenticationManager authManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.authenticationProvider(authProvider);
		return authenticationManagerBuilder.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	
	//////////////////////////////////////////////
	// The cache managers, needed for the XWiki services caching
	//////////////////////////////////////////////

	@Bean
	CacheManager cacheManager(@Autowired final Ticker ticker) {
		final CaffeineCache hCache = buildCache(XWikiServiceConfiguration.ONE_HOUR_LOCAL_CACHE_NAME, ticker, 60);
		final SimpleCacheManager manager = new SimpleCacheManager();
		manager.setCaches(Arrays.asList(hCache));
		return manager;
	}

	private CaffeineCache buildCache(final String name, final Ticker ticker, final int minutesToExpire) {
		return new CaffeineCache(name,
				Caffeine.newBuilder().expireAfterWrite(minutesToExpire, TimeUnit.MINUTES).ticker(ticker).build());
	}

	@Bean
	Ticker ticker() {
		return Ticker.systemTicker();
	}

}