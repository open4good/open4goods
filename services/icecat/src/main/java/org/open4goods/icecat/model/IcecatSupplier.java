package org.open4goods.icecat.model;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * A brand/manufacturer entry from the Icecat suppliers catalog.
 * Logo URLs are available in multiple resolutions for use in brand enrichment.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatSupplier {

    @JacksonXmlProperty(isAttribute = true, localName = "ID")
    private Integer id;

    /**
     * Brand name as an XML attribute. Icecat inconsistently uses both uppercase {@code Name}
     * and lowercase {@code name}; use {@link #getEffectiveName()} to resolve this.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "Name")
    private String nameUppercase;

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    private String nameLowercase;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoPic")
    private String logoPic;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoPicHeight")
    private Integer logoPicHeight;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoPicWidth")
    private Integer logoPicWidth;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoPicSize")
    private Integer logoPicSize;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoLowPic")
    private String logoLowPic;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoLowPicHeight")
    private Integer logoLowPicHeight;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoLowPicWidth")
    private Integer logoLowPicWidth;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoLowPicSize")
    private Integer logoLowPicSize;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoMediumPic")
    private String logoMediumPic;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoMediumPicHeight")
    private Integer logoMediumPicHeight;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoMediumPicWidth")
    private Integer logoMediumPicWidth;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoMediumPicSize")
    private Integer logoMediumPicSize;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoHighPic")
    private String logoHighPic;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoHighPicHeight")
    private Integer logoHighPicHeight;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoHighPicWidth")
    private Integer logoHighPicWidth;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoHighPicSize")
    private Integer logoHighPicSize;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoOriginal")
    private String logoOriginal;

    @JacksonXmlProperty(isAttribute = true, localName = "LogoOriginalSize")
    private Integer logoOriginalSize;

    @JacksonXmlProperty(isAttribute = true, localName = "Sponsor")
    private String sponsor;

    @JacksonXmlProperty(isAttribute = true, localName = "Updated")
    private String updated;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "CustomerService")
    private List<IcecatCustomerService> customerServices;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Names")
    private List<IcecatSupplierNames> names;

    /**
     * Returns the brand name, resolving Icecat's inconsistent casing
     * of the {@code Name} / {@code name} attribute.
     */
    public String getEffectiveName() {
        return nameUppercase != null ? nameUppercase : nameLowercase;
    }

    /** Returns the best available logo URL (prefers high-res, falls back to medium, low, standard). */
    public String getBestLogoUrl() {
        if (logoHighPic != null) return logoHighPic;
        if (logoMediumPic != null) return logoMediumPic;
        if (logoLowPic != null) return logoLowPic;
        return logoPic;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNameUppercase() { return nameUppercase; }
    public void setNameUppercase(String nameUppercase) { this.nameUppercase = nameUppercase; }

    public String getNameLowercase() { return nameLowercase; }
    public void setNameLowercase(String nameLowercase) { this.nameLowercase = nameLowercase; }

    public String getLogoPic() { return logoPic; }
    public void setLogoPic(String logoPic) { this.logoPic = logoPic; }

    public Integer getLogoPicHeight() { return logoPicHeight; }
    public void setLogoPicHeight(Integer logoPicHeight) { this.logoPicHeight = logoPicHeight; }

    public Integer getLogoPicWidth() { return logoPicWidth; }
    public void setLogoPicWidth(Integer logoPicWidth) { this.logoPicWidth = logoPicWidth; }

    public Integer getLogoPicSize() { return logoPicSize; }
    public void setLogoPicSize(Integer logoPicSize) { this.logoPicSize = logoPicSize; }

    public String getLogoLowPic() { return logoLowPic; }
    public void setLogoLowPic(String logoLowPic) { this.logoLowPic = logoLowPic; }

    public Integer getLogoLowPicHeight() { return logoLowPicHeight; }
    public void setLogoLowPicHeight(Integer logoLowPicHeight) { this.logoLowPicHeight = logoLowPicHeight; }

    public Integer getLogoLowPicWidth() { return logoLowPicWidth; }
    public void setLogoLowPicWidth(Integer logoLowPicWidth) { this.logoLowPicWidth = logoLowPicWidth; }

    public Integer getLogoLowPicSize() { return logoLowPicSize; }
    public void setLogoLowPicSize(Integer logoLowPicSize) { this.logoLowPicSize = logoLowPicSize; }

    public String getLogoMediumPic() { return logoMediumPic; }
    public void setLogoMediumPic(String logoMediumPic) { this.logoMediumPic = logoMediumPic; }

    public Integer getLogoMediumPicHeight() { return logoMediumPicHeight; }
    public void setLogoMediumPicHeight(Integer logoMediumPicHeight) { this.logoMediumPicHeight = logoMediumPicHeight; }

    public Integer getLogoMediumPicWidth() { return logoMediumPicWidth; }
    public void setLogoMediumPicWidth(Integer logoMediumPicWidth) { this.logoMediumPicWidth = logoMediumPicWidth; }

    public Integer getLogoMediumPicSize() { return logoMediumPicSize; }
    public void setLogoMediumPicSize(Integer logoMediumPicSize) { this.logoMediumPicSize = logoMediumPicSize; }

    public String getLogoHighPic() { return logoHighPic; }
    public void setLogoHighPic(String logoHighPic) { this.logoHighPic = logoHighPic; }

    public Integer getLogoHighPicHeight() { return logoHighPicHeight; }
    public void setLogoHighPicHeight(Integer logoHighPicHeight) { this.logoHighPicHeight = logoHighPicHeight; }

    public Integer getLogoHighPicWidth() { return logoHighPicWidth; }
    public void setLogoHighPicWidth(Integer logoHighPicWidth) { this.logoHighPicWidth = logoHighPicWidth; }

    public Integer getLogoHighPicSize() { return logoHighPicSize; }
    public void setLogoHighPicSize(Integer logoHighPicSize) { this.logoHighPicSize = logoHighPicSize; }

    public String getLogoOriginal() { return logoOriginal; }
    public void setLogoOriginal(String logoOriginal) { this.logoOriginal = logoOriginal; }

    public Integer getLogoOriginalSize() { return logoOriginalSize; }
    public void setLogoOriginalSize(Integer logoOriginalSize) { this.logoOriginalSize = logoOriginalSize; }

    public String getSponsor() { return sponsor; }
    public void setSponsor(String sponsor) { this.sponsor = sponsor; }

    public String getUpdated() { return updated; }
    public void setUpdated(String updated) { this.updated = updated; }

    public List<IcecatCustomerService> getCustomerServices() {
        return customerServices != null ? customerServices : Collections.emptyList();
    }
    public void setCustomerServices(List<IcecatCustomerService> customerServices) {
        this.customerServices = customerServices;
    }

    public List<IcecatSupplierNames> getNames() {
        return names != null ? names : Collections.emptyList();
    }
    public void setNames(List<IcecatSupplierNames> names) { this.names = names; }
}
