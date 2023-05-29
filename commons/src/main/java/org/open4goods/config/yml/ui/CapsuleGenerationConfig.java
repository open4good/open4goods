package org.open4goods.config.yml.ui;

import java.util.HashSet;
import java.util.Set;

import org.springframework.validation.annotation.Validated;

@Validated
public class CapsuleGenerationConfig {



	/**
	 * If false, we will also generate the products for datafragments that have no affiliated links.
	 */
	private boolean skipProductsWithoutAffiliatedOffers = false;

	/**
	 * If false, we will also generate the products for items that have no commercial
	 * offer
	 */
	private boolean skipProductsWithoutMerchantOffers = false;

	/**
	 * If defined, datafragments without this REFERENTIEL attributes keys will not be aggregated
	 */

	private Set<String> skipIfAttributesNotPresent = new HashSet<>();




	/**
	 * Frequency in ms for new update lockup agains masterendpoint
	 */
	private Long masterQueryUpdateDelay = 10000L;


	/**
	 * If true, the index will be cleaned before aggregating datas
	 */
	private Boolean cleanIndex = false;

	/**
	 * The size of elastic bulk for products indexation
	 */
	private Integer productsBulkIndexSize = 100;


	/**
	 * The max numbers of  aggregated datas to proceed, can be usefull to limit processing in dev mode
	 */
	private Long maxProducts = Long.MAX_VALUE;




	public boolean isSkipProductsWithoutAffiliatedOffers() {
		return skipProductsWithoutAffiliatedOffers;
	}

	public void setSkipProductsWithoutAffiliatedOffers(final boolean skipProductsWithoutAffiliatedOffers) {
		this.skipProductsWithoutAffiliatedOffers = skipProductsWithoutAffiliatedOffers;
	}

	public Set<String> getSkipIfAttributesNotPresent() {
		return skipIfAttributesNotPresent;
	}

	public void setSkipIfAttributesNotPresent(final Set<String> skipIfAttributesNotPresent) {
		this.skipIfAttributesNotPresent = skipIfAttributesNotPresent;
	}

	public boolean isSkipProductsWithoutMerchantOffers() {
		return skipProductsWithoutMerchantOffers;
	}

	public void setSkipProductsWithoutMerchantOffers(final boolean skipProductsWithoutMerchantOffers) {
		this.skipProductsWithoutMerchantOffers = skipProductsWithoutMerchantOffers;
	}


	public Long getMasterQueryUpdateDelay() {
		return masterQueryUpdateDelay;
	}

	public void setMasterQueryUpdateDelay(Long masterQueryUpdateDelay) {
		this.masterQueryUpdateDelay = masterQueryUpdateDelay;
	}

	public Boolean getCleanIndex() {
		return cleanIndex;
	}

	public void setCleanIndex(Boolean cleanIndex) {
		this.cleanIndex = cleanIndex;
	}

	public Integer getProductsBulkIndexSize() {
		return productsBulkIndexSize;
	}

	public void setProductsBulkIndexSize(Integer productsBulkIndexSize) {
		this.productsBulkIndexSize = productsBulkIndexSize;
	}

	public Long getMaxProducts() {
		return maxProducts;
	}

	public void setMaxProducts(Long maxProducts) {
		this.maxProducts = maxProducts;
	}





}
