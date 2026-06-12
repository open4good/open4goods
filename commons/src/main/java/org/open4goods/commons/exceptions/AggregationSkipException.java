package org.open4goods.commons.exceptions;

/**
 * Indicates than an error in aggragation pipeline is not revovable and must prevent the data indexation
 * @author Goulven.Furet
 *
 */
public class AggregationSkipException extends Exception {

	private static final long serialVersionUID = 2675494159198565365L;

	public AggregationSkipException() {
		super();
	}

	public AggregationSkipException(String message, Throwable cause) {
		super(message, cause);
	}

	public AggregationSkipException(String message) {
		super(message);
	}

	/**
	 * Overridden to return {@code this} directly, bypassing expensive stack trace generation.
	 * This is an optimization for exceptions thrown frequently in tight loops (e.g. invalid barcodes 
	 * skipped during real-time data aggregation) where stack traces are not used.
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
