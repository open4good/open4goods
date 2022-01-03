package org.open4goods.exceptions;

/**
 * Indicates than an error in aggragation pipeline is not revovable and must prevent the data indexation
 * @author Goulven.Furet
 *
 */
public class AggregationSkipException extends Exception {

	public AggregationSkipException() {
		super();
	}

	public AggregationSkipException(String message, Throwable cause) {
		super(message, cause);
	}

	public AggregationSkipException(String message) {
		super(message);
	}

}
