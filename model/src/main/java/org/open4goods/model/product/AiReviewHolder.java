package org.open4goods.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.ai.AiReview;

@io.swagger.v3.oas.annotations.media.Schema(description = "Holder for AI generated review and metadata")
public class AiReviewHolder {
	
	/**
	 * Date the AIReview was created
	 */

	@io.swagger.v3.oas.annotations.media.Schema(description = "The generated review")
	private AiReview review;
	
	// Map of url<>estimated tokens count of the markdown content
    @io.swagger.v3.oas.annotations.media.Schema(description = "Map of source URLs to estimated token counts")
	private Map<String, Integer> sources = new HashMap<>();
	
	// If false, means generation could not be launched, not enough data to proceed
    @io.swagger.v3.oas.annotations.media.Schema(description = "True if enough data was available to generate review")
	private boolean enoughData;
	
    @io.swagger.v3.oas.annotations.media.Schema(description = "Total tokens used for generation")
	private Integer totalTokens;
	
	/**
	 * Date the AIReview was created
	 */
    @io.swagger.v3.oas.annotations.media.Schema(description = "Creation timestamp in epoch milliseconds")
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
