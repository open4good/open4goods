package org.open4goods.commons.exceptions;

//@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 3321207092043790032L;

	public ResourceNotFoundException() {
		super();

	}

	public ResourceNotFoundException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public ResourceNotFoundException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public ResourceNotFoundException(final String message) {
		super(message);

	}

	public ResourceNotFoundException(final Throwable cause) {
		super(cause);

	}

}