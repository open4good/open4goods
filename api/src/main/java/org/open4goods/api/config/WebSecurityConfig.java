package org.open4goods.api.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.constants.UrlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
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

	private @Autowired ApiProperties apiProperties;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
		.csrf().disable()
		.addFilterBefore(new TokenAuthenticationFilter(), BasicAuthenticationFilter.class)
		.sessionManagement()
		.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		.and()

		.authorizeRequests()
		.anyRequest().permitAll()
		.and().headers().frameOptions().disable();

		http.headers().frameOptions().disable();

		return http.build();
	}

	public class TokenAuthenticationFilter extends GenericFilterBean {





		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)	throws IOException, ServletException {

			final HttpServletRequest httpRequest = (HttpServletRequest) request;

			// Extracting token prom parameter
			String accessToken = httpRequest.getParameter(UrlConstants.APIKEY_PARAMETER);
			if (null == accessToken) {
				accessToken = httpRequest.getHeader (UrlConstants.APIKEY_PARAMETER);
			}



			// By default, we provide a USER role
			final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
			authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_USER));

			if (null != accessToken) {


				// Assignation of the ROLE_ADMIN
				if (accessToken.equals(apiProperties.getAdminKey())) {
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_ADMIN));
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_CRAWLER));
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_UI_CAPSULE));
				}

				// Assignation of the ROLE_CRAWLER
				if (apiProperties.getCrawlerKeys().contains(accessToken) ) {
					authorities.add(new SimpleGrantedAuthority(RolesConstants.ROLE_CRAWLER));
				}


			}

			// Populate SecurityContextHolder
			final User user = new User("username", "password", true, true, true, true, authorities);

			final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);

			// Continue request handling
			chain.doFilter(request, response);
		}
	}

}