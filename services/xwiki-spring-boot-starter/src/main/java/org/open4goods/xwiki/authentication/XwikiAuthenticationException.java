package org.open4goods.xwiki.authentication;

import org.springframework.security.core.AuthenticationException;

/**
 * Custom Exception for xwiki authentication
 * @author Thierry.Ledan
 *
 */
public class XwikiAuthenticationException extends AuthenticationException {

	private static final long serialVersionUID = 1L;

	public XwikiAuthenticationException(String msg) {
		super(msg);
	}

}
