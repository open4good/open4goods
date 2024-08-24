package org.open4goods.commons.model.icecat;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatSupplier {

    @JacksonXmlProperty(localName = "CustomerService")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<IcecatCustomerService> customerServices;

    @JacksonXmlProperty(localName = "Names")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<IcecatSupplierNames> names;

    @JacksonXmlProperty(isAttribute = true)
    private Integer ID;

    @JacksonXmlProperty(isAttribute = true)
    private String Name;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String LogoPic;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoPicHeight;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoPicWidth;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoPicSize;

    @JacksonXmlProperty(isAttribute = true)
    private String LogoLowPic;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoLowPicHeight;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoLowPicWidth;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoLowPicSize;

    @JacksonXmlProperty(isAttribute = true)
    private String LogoMediumPic;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoMediumPicHeight;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoMediumPicWidth;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoMediumPicSize;

    @JacksonXmlProperty(isAttribute = true)
    private String LogoHighPic;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoHighPicHeight;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoHighPicWidth;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoHighPicSize;

    @JacksonXmlProperty(isAttribute = true)
    private String LogoOriginal;

    @JacksonXmlProperty(isAttribute = true)
    private Integer LogoOriginalSize;

    @JacksonXmlProperty(isAttribute = true)
    private String Sponsor;

    @JacksonXmlProperty(isAttribute = true)
    private String Updated;

    @JacksonXmlProperty(isAttribute = true)
    private String updated;

    // Getters et setters

    public List<IcecatCustomerService> getCustomerServices() {
        return customerServices;
    }

    public void setCustomerServices(List<IcecatCustomerService> customerServices) {
        this.customerServices = customerServices;
    }

    public List<IcecatSupplierNames> getNames() {
        return names;
    }

    public void setNames(List<IcecatSupplierNames> names) {
        this.names = names;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getLogoPic() {
        return LogoPic;
    }

    public void setLogoPic(String logoPic) {
        LogoPic = logoPic;
    }

    public Integer getLogoPicHeight() {
        return LogoPicHeight;
    }

    public void setLogoPicHeight(Integer logoPicHeight) {
        LogoPicHeight = logoPicHeight;
    }

    public Integer getLogoPicWidth() {
        return LogoPicWidth;
    }

    public void setLogoPicWidth(Integer logoPicWidth) {
        LogoPicWidth = logoPicWidth;
    }

    public Integer getLogoPicSize() {
        return LogoPicSize;
    }

    public void setLogoPicSize(Integer logoPicSize) {
        LogoPicSize = logoPicSize;
    }

    public String getLogoLowPic() {
        return LogoLowPic;
    }

    public void setLogoLowPic(String logoLowPic) {
        LogoLowPic = logoLowPic;
    }

    public Integer getLogoLowPicHeight() {
        return LogoLowPicHeight;
    }

    public void setLogoLowPicHeight(Integer logoLowPicHeight) {
        LogoLowPicHeight = logoLowPicHeight;
    }

    public Integer getLogoLowPicWidth() {
        return LogoLowPicWidth;
    }

    public void setLogoLowPicWidth(Integer logoLowPicWidth) {
        LogoLowPicWidth = logoLowPicWidth;
    }

    public Integer getLogoLowPicSize() {
        return LogoLowPicSize;
    }

    public void setLogoLowPicSize(Integer logoLowPicSize) {
        LogoLowPicSize = logoLowPicSize;
    }

    public String getLogoMediumPic() {
        return LogoMediumPic;
    }

    public void setLogoMediumPic(String logoMediumPic) {
        LogoMediumPic = logoMediumPic;
    }

    public Integer getLogoMediumPicHeight() {
        return LogoMediumPicHeight;
    }

    public void setLogoMediumPicHeight(Integer logoMediumPicHeight) {
        LogoMediumPicHeight = logoMediumPicHeight;
    }

    public Integer getLogoMediumPicWidth() {
        return LogoMediumPicWidth;
    }

    public void setLogoMediumPicWidth(Integer logoMediumPicWidth) {
        LogoMediumPicWidth = logoMediumPicWidth;
    }

    public Integer getLogoMediumPicSize() {
        return LogoMediumPicSize;
    }

    public void setLogoMediumPicSize(Integer logoMediumPicSize) {
        LogoMediumPicSize = logoMediumPicSize;
    }

    public String getLogoHighPic() {
        return LogoHighPic;
    }

    public void setLogoHighPic(String logoHighPic) {
        LogoHighPic = logoHighPic;
    }

    public Integer getLogoHighPicHeight() {
        return LogoHighPicHeight;
    }

    public void setLogoHighPicHeight(Integer logoHighPicHeight) {
        LogoHighPicHeight = logoHighPicHeight;
    }

    public Integer getLogoHighPicWidth() {
        return LogoHighPicWidth;
    }

    public void setLogoHighPicWidth(Integer logoHighPicWidth) {
        LogoHighPicWidth = logoHighPicWidth;
    }

    public Integer getLogoHighPicSize() {
        return LogoHighPicSize;
    }

    public void setLogoHighPicSize(Integer logoHighPicSize) {
        LogoHighPicSize = logoHighPicSize;
    }

    public String getLogoOriginal() {
        return LogoOriginal;
    }

    public void setLogoOriginal(String logoOriginal) {
        LogoOriginal = logoOriginal;
    }

    public Integer getLogoOriginalSize() {
        return LogoOriginalSize;
    }

    public void setLogoOriginalSize(Integer logoOriginalSize) {
        LogoOriginalSize = logoOriginalSize;
    }

    public String getSponsor() {
        return Sponsor;
    }

    public void setSponsor(String sponsor) {
        Sponsor = sponsor;
    }

    public String getUpdated() {
        return Updated;
    }

    public void setUpdated(String updated) {
        Updated = updated;
    }

    public String getUpdatedAttribute() {
        return updated;
    }

    public void setUpdatedAttribute(String updated) {
        this.updated = updated;
    }
}