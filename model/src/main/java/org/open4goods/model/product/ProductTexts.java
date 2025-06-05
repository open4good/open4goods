package org.open4goods.model.product;

import org.open4goods.model.Localisable;

public record ProductTexts(
                Localisable<String, String> url,
                Localisable<String, String> h1Title,
                Localisable<String, String> metaDescription,
                Localisable<String, String> productMetaOpenGraphTitle,
                Localisable<String, String> productMetaOpenGraphDescription,
                Localisable<String, String> productMetaTwitterTitle,
                Localisable<String, String> productMetaTwitterDescription) {

        public ProductTexts() {
                this(new Localisable<>(), new Localisable<>(), new Localisable<>(),
                                new Localisable<>(), new Localisable<>(), new Localisable<>(),
                                new Localisable<>());
        }
	

	public Localisable<String, String> getUrl() {
		return url;
	}


	public Localisable<String, String> getH1Title() {
		return h1Title;
	}


	public Localisable<String, String> getMetaDescription() {
		return metaDescription;
	}


	public Localisable<String, String> getProductMetaOpenGraphTitle() {
		return productMetaOpenGraphTitle;
	}


	public Localisable<String, String> getProductMetaOpenGraphDescription() {
		return productMetaOpenGraphDescription;
	}


	public Localisable<String, String> getProductMetaTwitterTitle() {
		return productMetaTwitterTitle;
	}


	public Localisable<String, String> getProductMetaTwitterDescription() {
		return productMetaTwitterDescription;
	}


	
}
