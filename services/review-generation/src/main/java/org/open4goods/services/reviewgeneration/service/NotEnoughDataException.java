package org.open4goods.services.reviewgeneration.service;

import java.util.Map;

import org.open4goods.services.reviewgeneration.dto.ReviewGenerationFailureDetails;

/**
 * Raised when review generation cannot proceed because source retrieval did not
 * produce enough usable product facts.
 */
public class NotEnoughDataException extends Exception {

	private final ReviewGenerationFailureDetails details;
	private Map<String, String> enrichmentStatus = Map.of();

	public NotEnoughDataException() {
		super();
		this.details = null;
	}

	public NotEnoughDataException(String message) {
		super(message);
		this.details = null;
	}

	public NotEnoughDataException(String message, ReviewGenerationFailureDetails details) {
		super(message);
		this.details = details;
	}

	public ReviewGenerationFailureDetails getDetails() {
		return details;
	}

	public Map<String, String> getEnrichmentStatus() {
		return enrichmentStatus;
	}

	public void setEnrichmentStatus(Map<String, String> enrichmentStatus) {
		this.enrichmentStatus = enrichmentStatus == null ? Map.of() : Map.copyOf(enrichmentStatus);
	}
}
