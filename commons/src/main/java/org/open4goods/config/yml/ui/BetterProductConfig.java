package org.open4goods.config.yml.ui;

import java.util.ArrayList;
import java.util.List;

public class BetterProductConfig {

	private Integer numberOfBetterProducts = 5;

	private List<BetterProductFilter> attributesFilters = new ArrayList<>();

	private List<BetterProductFilter> ratingsFilters = new ArrayList<>();

	/**
	 *
	 * @return the number of mandatory elements
	 */
	public Long numberOfMandatories() {
		return attributesFilters.stream().filter(e -> e.getRequired()).count() + ratingsFilters.stream().filter(e -> e.getRequired()).count();
	}


	public List<BetterProductFilter> getAttributesFilters() {
		return attributesFilters;
	}

	public void setAttributesFilters(final List<BetterProductFilter> attributesFilters) {
		this.attributesFilters = attributesFilters;
	}

	public List<BetterProductFilter> getRatingsFilters() {
		return ratingsFilters;
	}

	public void setRatingsFilters(final List<BetterProductFilter> ratingsFilters) {
		this.ratingsFilters = ratingsFilters;
	}

	public Integer getNumberOfBetterProducts() {
		return numberOfBetterProducts;
	}

	public void setNumberOfBetterProducts(final Integer numberOfBetterProducts) {
		this.numberOfBetterProducts = numberOfBetterProducts;
	}






}
