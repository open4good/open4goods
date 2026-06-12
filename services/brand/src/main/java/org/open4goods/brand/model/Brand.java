package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple representation of a brand and its associated company.
 */
public class Brand {

    private String brandName;

    private String companyName;

    private Company company;

    private List<String> officialDomains = new ArrayList<>();

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
        if (company != null && company.getName() != null && !company.getName().isBlank()) {
            return company.getName();
        }
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public List<String> getOfficialDomains() {
        return officialDomains;
    }

    public void setOfficialDomains(List<String> officialDomains) {
        this.officialDomains = officialDomains != null ? officialDomains : new ArrayList<>();
    }

    @Override
    public String toString() {
        String compName = getCompanyName();
        return brandName + (compName != null && !compName.isBlank() ? " (" + compName + ")" : "");
    }
}
