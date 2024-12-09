package org.open4goods.commons.model.data;

public class Brand {

	private String brandName;

	private String companyName;
	
	@Override
	public String toString() {
		return brandName + companyName != null ? ("( " + companyName+")") : "";
	}
	
	public Brand(String name) {
		this.brandName = name;
	}
	
	public Brand() {
		super();
	}
	
	public String getBrandName() {
		return brandName;
	}

	public void setBrandName(String name) {
		this.brandName = name;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	
}
