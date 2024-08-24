package org.open4goods.commons.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 3321207092043790032L;

	public ForbiddenException() {
		super();

	}

	public ForbiddenException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public ForbiddenException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public ForbiddenException(final String message) {
		super(message);

	}

	public ForbiddenException(final Throwable cause) {
		super(cause);

	}

}