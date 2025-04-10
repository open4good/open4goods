package org.open4goods.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.ai.AiReview;

public class AiReviewHolder {
	
	/**
	 * Date the AIReview was created
	 */

	 
	private AiReview review;
	
	// Map of url<>estimated tokens count of the markdown content
	private Map<String, Integer> sources = new HashMap<>();
	
	// If false, means generation could not be launched, not enough data to proceed
	private boolean enoughData;
	
	private Integer totalTokens;
	
	/**
	 * Date the AIReview was created
	 */
	private Long createdMs;

	public AiReview getReview() {
		return review;
	}

	public void setReview(AiReview review) {
		this.review = review;
	}

	public Map<String, Integer> getSources() {
		return sources;
	}

	public void setSources(Map<String, Integer> sources) {
		this.sources = sources;
	}

	public Long getCreatedMs() {
		return createdMs;
	}

	public void setCreatedMs(Long createdMs) {
		this.createdMs = createdMs;
	}

	public boolean isEnoughData() {
		return enoughData;
	}

	public void setEnoughData(boolean enoughData) {
		this.enoughData = enoughData;
	}

	public Integer getTotalTokens() {
		return totalTokens;
	}

	public void setTotalTokens(Integer totalTokens) {
		this.totalTokens = totalTokens;
	}
	
	
	

}
