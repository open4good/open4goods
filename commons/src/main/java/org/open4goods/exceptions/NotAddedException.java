package org.open4goods.exceptions;

public class NotAddedException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 8081955467953179782L;

	public NotAddedException() {
		super();

	}

	public NotAddedException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public NotAddedException(final String message, final Throwable cause) {
		super(message, cause);

	}

	public NotAddedException(final String message) {
		super(message);

	}

	public NotAddedException(final Throwable cause) {
		super(cause);

	}


}
