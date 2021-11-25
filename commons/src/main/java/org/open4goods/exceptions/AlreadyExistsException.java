package org.open4goods.exceptions;

public class AlreadyExistsException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -3413108170927741046L;

	public AlreadyExistsException() {
		super();

	}

	public AlreadyExistsException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public AlreadyExistsException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public AlreadyExistsException(final String message) {
		super(message);

	}

	public AlreadyExistsException(final Throwable cause) {
		super(cause);

	}

}