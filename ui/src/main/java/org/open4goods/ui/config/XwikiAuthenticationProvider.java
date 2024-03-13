package org.open4goods.ui.config;
// TODO: Temp until o4g-xwiki-springboot-starter is ready
import java.util.ArrayList;
import java.util.List;

import org.open4goods.services.XwikiService;
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
//@Component
//@DependsOn("XWikiService")
//@ConditionalOnProperty(name = "xwiki.spring.profile") 
public class XwikiAuthenticationProvider implements AuthenticationProvider {

	//@Autowired 
	XwikiService xwikiService;
	
	public XwikiAuthenticationProvider(XwikiService xwikiService) {
		this.xwikiService = xwikiService;
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
			groups = xwikiService.loginAndGetGroups(user, password);
		} catch (Exception e) {
			// error messages have been managed in login method
			throw new XwikiAuthenticationException(e.getMessage());
		} 
		
		List<GrantedAuthority> grantedAuths = new ArrayList<>();
		if( groups != null ) {
			groups.stream().forEach(e ->  { 
				grantedAuths.add(new SimpleGrantedAuthority( e.replace("xwiki:XWiki.", "").trim().toUpperCase() ));
				//grantedAuths.add(new SimpleGrantedAuthority( "ROLE_" + e.replace("xwiki:XWiki.", "").trim().toUpperCase() ));
			});
		}
		
		return new UsernamePasswordAuthenticationToken(authentication.getName(), authentication.getCredentials(), grantedAuths);
	}
}