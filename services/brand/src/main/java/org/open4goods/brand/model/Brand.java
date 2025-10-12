package org.open4goods.brand.model;

/**
 * Simple representation of a brand and its associated company.
 */
public class Brand {

    private String brandName;

    private String companyName;

    public Brand() {
    }

    public Brand(String name) {
        this.brandName = name;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return brandName + (companyName != null ? " (" + companyName + ")" : "");
    }
}
