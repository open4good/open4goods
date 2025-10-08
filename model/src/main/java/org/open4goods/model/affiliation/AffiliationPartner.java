package org.open4goods.model.affiliation;

import java.util.Objects;
import java.util.Set;

public class AffiliationPartner {
	private String id;
	private String name;
	private String logoUrl;
	private String affiliationLink;
	private String portalUrl;
	private Set<String> countryCodes;

	  @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof AffiliationPartner)) return false;
	        AffiliationPartner that = (AffiliationPartner) o;
	        return Objects.equals(id, that.id)
	                && Objects.equals(name, that.name)
	                && Objects.equals(logoUrl, that.logoUrl)
	                && Objects.equals(affiliationLink, that.affiliationLink)
	                && Objects.equals(countryCodes, that.countryCodes);
	    }

	    @Override
	    public int hashCode() {
	        return Objects.hash(id, name, logoUrl, affiliationLink, countryCodes);
	    }


	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLogoUrl() {
		return logoUrl;
	}
	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}
	public String getAffiliationLink() {
		return affiliationLink;
	}
	public void setAffiliationLink(String affiliationLink) {
		this.affiliationLink = affiliationLink;
	}
	public Set<String> getCountryCodes() {
		return countryCodes;
	}
	public void setCountryCodes(Set<String> countryCodes) {
		this.countryCodes = countryCodes;
	}

	public String getPortalUrl() {
		return portalUrl;
	}

	public void setPortalUrl(String portalUrl) {
		this.portalUrl = portalUrl;
	}




}
