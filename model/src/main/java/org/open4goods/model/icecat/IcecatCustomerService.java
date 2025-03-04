package org.open4goods.model.icecat;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatCustomerService {

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlProperty(isAttribute = true)
    private Integer Country_ID;

    @JacksonXmlProperty(isAttribute = true)
    private String Country_Code;

    @JacksonXmlProperty(isAttribute = true)
    private String Country;

    @JacksonXmlProperty(isAttribute = true)
    private String Phone;

    @JacksonXmlProperty(isAttribute = true)
    private String AddressDetails;

    @JacksonXmlProperty(isAttribute = true)
    private String Website;

    // Getters et setters

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public Integer getCountry_ID() {
        return Country_ID;
    }

    public void setCountry_ID(Integer country_ID) {
        Country_ID = country_ID;
    }

    public String getCountry_Code() {
        return Country_Code;
    }

    public void setCountry_Code(String country_Code) {
        Country_Code = country_Code;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String country) {
        Country = country;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getAddressDetails() {
        return AddressDetails;
    }

    public void setAddressDetails(String addressDetails) {
        AddressDetails = addressDetails;
    }

    public String getWebsite() {
        return Website;
    }

    public void setWebsite(String website) {
        Website = website;
    }
}
