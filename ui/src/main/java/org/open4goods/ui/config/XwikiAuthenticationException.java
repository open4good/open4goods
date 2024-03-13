package org.open4goods.ui.config;
//TODO: Temp until o4g-xwiki-springboot-starter is ready
import org.springframework.security.core.AuthenticationException;

public class XwikiAuthenticationException extends AuthenticationException {

	public XwikiAuthenticationException(String msg) {
		super(msg);
	}

}
