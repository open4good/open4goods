package org.open4goods.ui.config;

import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
/**
 * HTTP Security configuration
 * @author Goulven.Furet
 *
 */
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private @Autowired UiConfig config;
		
    private @Autowired AuthenticationProvider  authProvider;
	
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("attributes-datasources-report.html");
		//TODO(security,P1, 0.5) : Add actuator security
		web.ignoring().antMatchers("/actuator/**");
		web.ignoring().antMatchers("/wikiupdate/**");
		web.ignoring().antMatchers("/widgets");
		web.ignoring().antMatchers("/widget/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		//TODO : gof : see restrictions
		//		if (config.getDeploymentConfig().getRestricted()) {
//		
//		http.authorizeRequests()			
//			.anyRequest().authenticated()
//			.and().formLogin().permitAll()
//			.and().logout().permitAll();
//		} else 
		
		{
			http.authorizeRequests()			
			.anyRequest().permitAll()
			.and().formLogin().permitAll()
			.and().logout().permitAll();
		}
		
		http.csrf().disable();
		http.headers().frameOptions().disable();
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {	
			auth.authenticationProvider(authProvider);
	}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}