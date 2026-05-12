package org.open4goods.icecat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/** Brand customer-service contact information for a specific country. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatCustomerService {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    @JacksonXmlProperty(isAttribute = true, localName = "Country_ID")
    private Integer countryId;

    @JacksonXmlProperty(isAttribute = true, localName = "Country_Code")
    private String countryCode;

    @JacksonXmlProperty(isAttribute = true, localName = "Country")
    private String country;

    @JacksonXmlProperty(isAttribute = true, localName = "Phone")
    private String phone;

    @JacksonXmlProperty(isAttribute = true, localName = "AddressDetails")
    private String addressDetails;

    @JacksonXmlProperty(isAttribute = true, localName = "Website")
    private String website;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCountryId() { return countryId; }
    public void setCountryId(Integer countryId) { this.countryId = countryId; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddressDetails() { return addressDetails; }
    public void setAddressDetails(String addressDetails) { this.addressDetails = addressDetails; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
