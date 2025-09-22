package org.open4goods.b2b.dto.product;

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import model.AvailableFacets;

public class RequestMetadata {

	@Schema(description = "Total credits cost of this request")
	private Integer requestCost;

	@Schema(description = "Detailled credits cost for this request, per facets")
	private Map<AvailableFacets, Short> facetsCosts = new HashMap<>();

	@Schema(description = "Timestamp (epoch milliseconds) where this request was initiated")
	private Long timestampEpoch;

	@Schema(description = "Response generation durationMs (in milliseconds)")
	private Long durationMs;

	public RequestMetadata() {
		super();
	}
	public Integer getRequestCost() {
		return requestCost;
	}
	public void setRequestCost(Integer requestCost) {
		this.requestCost = requestCost;
	}
	public Map<AvailableFacets, Short> getFacetsCosts() {
		return facetsCosts;
	}
	public void setFacetsCosts(Map<AvailableFacets, Short> facetsCosts) {
		this.facetsCosts = facetsCosts;
	}
	public Long getTimestampEpoch() {
		return timestampEpoch;
	}
	public void setTimestampEpoch(Long timestampEpoch) {
		this.timestampEpoch = timestampEpoch;
	}
	public Long getDurationMs() {
		return durationMs;
	}
	public void setDurationMs(Long duration) {
		this.durationMs = duration;
	}
}
