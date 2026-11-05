package org.open4goods.model.product;

import org.open4goods.model.Localisable;

/**
 * Localised naming elements of a product: public URL slug, titles and meta.
 *
 * <p>The slug ({@link #getUrl()}) is frozen once generated: it is only regenerated when it is
 * missing for a language or when the product vertical differs from {@link #getUrlVertical()}
 * (GOU-1708).</p>
 */
public class ProductTexts {

	/** Value of {@link #urlVertical} for a slug generated while the product had no vertical. */
	public static final String URL_WITHOUT_VERTICAL = "_none";

	private Localisable<String,String> url = new Localisable<>();

	/**
	 * Vertical of the product when its slug was generated, {@link #URL_WITHOUT_VERTICAL} when it had
	 * none, {@code null} when unknown (slug generated before GOU-1708, kept as is).
	 */
	private String urlVertical;

	private Localisable<String,String> displayName = new Localisable<>();

	private Localisable<String,String> cardName = new Localisable<>();

	private Localisable<String,String> pageTitle = new Localisable<>();

	private Localisable<String,String> seoName = new Localisable<>();
	
	private Localisable<String,String> metaDescription = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphTitle = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphDescription = new Localisable<>();
	

	public Localisable<String, String> getUrl() {
		return url;
	}

	public void setUrl(Localisable<String, String> url) {
		this.url = url;
	}

	/**
	 * @return vertical of the product when its slug was generated, {@link #URL_WITHOUT_VERTICAL}
	 *         when it had none, {@code null} when unknown
	 */
	public String getUrlVertical() {
		return urlVertical;
	}

	public void setUrlVertical(String urlVertical) {
		this.urlVertical = urlVertical;
	}

	public Localisable<String, String> getDisplayName() {
		return displayName;
	}

	public void setDisplayName(Localisable<String, String> displayName) {
		this.displayName = displayName;
	}

	public Localisable<String, String> getCardName() {
		return cardName;
	}

	public void setCardName(Localisable<String, String> cardName) {
		this.cardName = cardName;
	}

	public Localisable<String, String> getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(Localisable<String, String> pageTitle) {
		this.pageTitle = pageTitle;
	}

	public Localisable<String, String> getSeoName() {
		return seoName;
	}

	public void setSeoName(Localisable<String, String> seoName) {
		this.seoName = seoName;
	}

	public Localisable<String, String> getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(Localisable<String, String> metaDescription) {
		this.metaDescription = metaDescription;
	}

	public Localisable<String, String> getProductMetaOpenGraphTitle() {
		return productMetaOpenGraphTitle;
	}

	public void setProductMetaOpenGraphTitle(Localisable<String, String> productMetaOpenGraphTitle) {
		this.productMetaOpenGraphTitle = productMetaOpenGraphTitle;
	}

	public Localisable<String, String> getProductMetaOpenGraphDescription() {
		return productMetaOpenGraphDescription;
	}

	public void setProductMetaOpenGraphDescription(Localisable<String, String> productMetaOpenGraphDescription) {
		this.productMetaOpenGraphDescription = productMetaOpenGraphDescription;
	}

	
}
