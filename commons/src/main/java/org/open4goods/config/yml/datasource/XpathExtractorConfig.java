
package org.open4goods.config.yml.datasource;

import java.util.Map;

import javax.validation.constraints.NotBlank;

public class XpathExtractorConfig {

	@NotBlank
	private String xpathName;

	@NotBlank
	private String xpathPrice;

	@NotBlank
	private String xpathCurrency;

	private String xpathDescription;

	private Map<String, String> xpathOther;

	public String getXpathName() {
		return xpathName;
	}

	public void setXpathName(final String xpathName) {
		this.xpathName = xpathName;
	}

	public String getXpathPrice() {
		return xpathPrice;
	}

	public void setXpathPrice(final String xpathPrice) {
		this.xpathPrice = xpathPrice;
	}

	public String getXpathCurrency() {
		return xpathCurrency;
	}

	public void setXpathCurrency(final String xpathCurrency) {
		this.xpathCurrency = xpathCurrency;
	}

	public String getXpathDescription() {
		return xpathDescription;
	}

	public void setXpathDescription(final String xpathDescription) {
		this.xpathDescription = xpathDescription;
	}

	public Map<String, String> getXpathOther() {
		return xpathOther;
	}

	public void setXpathOther(final Map<String, String> xpathOther) {
		this.xpathOther = xpathOther;
	}

}
