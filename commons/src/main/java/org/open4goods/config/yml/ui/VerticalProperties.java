
package org.open4goods.config.yml.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonMerge;

import jakarta.validation.constraints.NotBlank;
@Validated
/**
 * Configuration properties for a capsule definition at the API level
 * @author goulven
 *
 */
public class VerticalProperties {


	/**
	 * The apiKey that the capsule must presents in order to be allowed to connect to the API
	 */
	@NotBlank
	private String apiKey;

	/** The validity duration in days of a price d**/
	private Integer priceValidity =  3;



	/**
	 * The minimum price a datafragment must have to be aggregated
	 */
	private Double minimumEvictionPrice;


	/**
	 * The elastic query, based on category filtering. Please, use kibana and the "verticals" boards to elaborate
	 */
	private String query;


	/**
	 * Limit the gtin extraction to this limit
	 */
	private Long gtinLimit;


	/**
	 * The list of strings that will be removed from model names.
	 */
	@JsonMerge
	private List<String> modelTokensRemovals = new ArrayList<>();



	/**
	 * The attributes name/values replacement that occurs at the segment level.
	 * They are appended to the attributes replacement defined at the capsule level
	 */
	@JsonMerge
	private Map<String,Map<String,String>> attributesReplacement = new HashMap<>();




	///////////////////////////////////////////////////////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////////////////////////////////////////////////////////


	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}



	public Map<String, Map<String, String>> getAttributesReplacement() {
		return attributesReplacement;
	}

	public void setAttributesReplacement(final Map<String, Map<String, String>> attributesReplacement) {
		this.attributesReplacement = attributesReplacement;
	}


	public Integer getPriceValidity() {
		return priceValidity;
	}

	public void setPriceValidity(Integer csvPriceValidity) {
		priceValidity = csvPriceValidity;
	}



	public Double getMinimumEvictionPrice() {
		return minimumEvictionPrice;
	}

	public void setMinimumEvictionPrice(final Double minimumEvictionPrice) {
		this.minimumEvictionPrice = minimumEvictionPrice;
	}



	public String getQuery() {
		return query;
	}

	public void setQuery(String elasticQuery) {
		query = elasticQuery;
	}


	public List<String> getModelTokensRemovals() {
		return modelTokensRemovals;
	}

	public void setModelTokensRemovals(final List<String> brandUidRemovals) {
		modelTokensRemovals = brandUidRemovals;
	}

	public Long getGtinLimit() {
		return gtinLimit;
	}

	public void setGtinLimit(Long gtinLimit) {
		this.gtinLimit = gtinLimit;
	}


}
