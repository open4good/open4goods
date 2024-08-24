package org.open4goods.commons.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidParameterException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -8211890012230278518L;

	public InvalidParameterException() {
		super();

	}

	public InvalidParameterException(final String message) {
		super(message);

	}

	public InvalidParameterException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public InvalidParameterException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public InvalidParameterException(final Throwable cause) {
		super(cause);

	}

}