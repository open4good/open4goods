package org.open4goods.b2b.dto.product;

import java.util.HashMap;
import java.util.Map;

import model.AvailableFacets;

public class RequestMetadata {

	private Integer requestCost;
	private Map<AvailableFacets, Short> facetsCosts = new HashMap<>();
	private Long timestampEpoch;





	public RequestMetadata() {
		super();
		timestampEpoch = System.currentTimeMillis();
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




}
