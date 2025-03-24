package org.open4goods.model.exceptions;

public class TechnicalException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 4503388211286582339L;

	public TechnicalException() {
		super();

	}

	public TechnicalException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);

	}

	public TechnicalException(final String message, final Throwable cause) {
		super(message, cause);

	}



	public TechnicalException(final String message) {
		super(message);

	}

	public TechnicalException(final Throwable cause) {
		super(cause);

	}

}