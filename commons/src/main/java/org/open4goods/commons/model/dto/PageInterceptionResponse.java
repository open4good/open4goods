package org.open4goods.commons.model.dto;

public class PageInterceptionResponse {

	private Long date;
	private Long duration;

	public PageInterceptionResponse() {

	}

	public PageInterceptionResponse(final Long attribute) {
		date = attribute;
		duration = System.currentTimeMillis() - date;

	}

	public Long getDate() {
		return date;
	}

	public void setDate(final Long date) {
		this.date = date;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(final Long duration) {
		this.duration = duration;
	}

}
