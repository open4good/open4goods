package org.open4goods.xwiki.authentication;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.xwiki.services.XWikiAuthenticationService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;




/**
 * A spring authentication provider that relies on Xwiki
 * @author Goulven.Furet
 *
 */

//TODO:  gérer le profile par les props ?? 

//@Profile("!dev")
//@ConditionalOnProperty(name = "xwiki.spring.profile") 
public class XwikiAuthenticationProvider implements AuthenticationProvider {

	XWikiAuthenticationService xwikiAuthenticationService;
	
	public XwikiAuthenticationProvider(XWikiAuthenticationService xwikiAuthenticationService) {
		this.xwikiAuthenticationService = xwikiAuthenticationService;
	}
	
	@Override
	public boolean supports(Class<?> authentication) {
		return true;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		String password = authentication.getCredentials().toString();
		String user = authentication.getName();

		List<String> groups  = new ArrayList<String>();
		try {
			groups = xwikiAuthenticationService.login(user, password);
		} catch (Exception e) {
			// error messages have been managed in login method
			// TODO : Specific exception 
			throw new XwikiAuthenticationException(e.getMessage());
		} 
		
		List<GrantedAuthority> grantedAuths = new ArrayList<>();
		if( groups != null ) {
			groups.stream().forEach(e ->  { 
				grantedAuths.add(new SimpleGrantedAuthority( e.replace("xwiki:XWiki.", "").trim().toUpperCase() ));
				grantedAuths.add(new SimpleGrantedAuthority( "ROLE_" + e.replace("xwiki:XWiki.", "").trim().toUpperCase() ));
			});
		}
		
		return new UsernamePasswordAuthenticationToken(authentication.getName(), authentication.getCredentials(), grantedAuths);
	}
}