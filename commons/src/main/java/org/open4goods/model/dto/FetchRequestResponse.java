package org.open4goods.model.dto;

public class FetchRequestResponse {
	private boolean crawlAccepted;
	private String message;


	public FetchRequestResponse() {
		super();
	}


	public FetchRequestResponse(final boolean crawlAccepted) {
		super();
		this.crawlAccepted = crawlAccepted;
	}


	public FetchRequestResponse(final boolean crawlAccepted, final String message) {
		super();
		this.crawlAccepted = crawlAccepted;
		this.message = message;
	}

	public boolean isCrawlAccepted() {
		return crawlAccepted;
	}

	public void setCrawlAccepted(final boolean crawlAccepted) {
		this.crawlAccepted = crawlAccepted;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}



}
