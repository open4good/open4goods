package org.open4goods.icecat.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatProduct {

    @JacksonXmlProperty(isAttribute = true)
    private String ID;

    @JacksonXmlProperty(isAttribute = true)
    private String Name;

    @JacksonXmlProperty(isAttribute = true)
    private String Title;

    @JacksonXmlProperty(isAttribute = true)
    private String ReleaseDate;

    @JacksonXmlProperty(isAttribute = true)
    private String EndOfLifeDate;

    @JacksonXmlProperty(isAttribute = true)
    private String Code;

    @JacksonXmlProperty(isAttribute = true)
    private String Prod_id;

    @JacksonXmlProperty(isAttribute = true)
    private String HighPic;

    @JacksonXmlProperty(isAttribute = true)
    private String LowPic;

    @JacksonXmlProperty(isAttribute = true)
    private String ThumbPic;

    @JacksonXmlProperty(localName = "Category")
    private IcecatCategory category;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "EANCode")
    private List<IcecatEANCode> eanCodes;

    @JacksonXmlProperty(localName = "Supplier")
    private IcecatSupplier supplier;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ProductGallery")
    private List<IcecatProductGallery> gallery;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ProductMultimediaObject")
    private List<IcecatMultimediaObject> multimedia;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "CategoryFeatureGroup")
    private List<IcecatCategoryFeatureGroup> categoryFeatureGroups;

    @JacksonXmlProperty(localName = "SummaryDescription")
    private IcecatSummaryDescription summaryDescription;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getReleaseDate() {
        return ReleaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        ReleaseDate = releaseDate;
    }

    public String getEndOfLifeDate() {
        return EndOfLifeDate;
    }

    public void setEndOfLifeDate(String endOfLifeDate) {
        EndOfLifeDate = endOfLifeDate;
    }

    public String getHighPic() {
        return HighPic;
    }

    public void setHighPic(String highPic) {
        HighPic = highPic;
    }

    public String getLowPic() {
        return LowPic;
    }

    public void setLowPic(String lowPic) {
        LowPic = lowPic;
    }

    public IcecatCategory getCategory() {
        return category;
    }

    public void setCategory(IcecatCategory category) {
        this.category = category;
    }

    public List<IcecatEANCode> getEanCodes() {
        return eanCodes;
    }

    public void setEanCodes(List<IcecatEANCode> eanCodes) {
        this.eanCodes = eanCodes;
    }

    public IcecatSupplier getSupplier() {
        return supplier;
    }

    public void setSupplier(IcecatSupplier supplier) {
        this.supplier = supplier;
    }

    public String getCode() {
        return Code;
    }

    public void setCode(String code) {
        Code = code;
    }

    public String getProd_id() {
        return Prod_id;
    }

    public void setProd_id(String prod_id) {
        Prod_id = prod_id;
    }

    public String getThumbPic() {
        return ThumbPic;
    }

    public void setThumbPic(String thumbPic) {
        ThumbPic = thumbPic;
    }

    public List<IcecatProductGallery> getGallery() {
        return gallery;
    }

    public void setGallery(List<IcecatProductGallery> gallery) {
        this.gallery = gallery;
    }

    public List<IcecatMultimediaObject> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<IcecatMultimediaObject> multimedia) {
        this.multimedia = multimedia;
    }

    public List<IcecatCategoryFeatureGroup> getCategoryFeatureGroups() {
        return categoryFeatureGroups;
    }

    public void setCategoryFeatureGroups(List<IcecatCategoryFeatureGroup> categoryFeatureGroups) {
        this.categoryFeatureGroups = categoryFeatureGroups;
    }

    public IcecatSummaryDescription getSummaryDescription() {
        return summaryDescription;
    }

    public void setSummaryDescription(IcecatSummaryDescription summaryDescription) {
        this.summaryDescription = summaryDescription;
    }
}
