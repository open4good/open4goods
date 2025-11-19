package org.open4goods.icecat.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IcecatProductPicture {
    @JacksonXmlProperty(isAttribute = true)
    private String Pic;

    @JacksonXmlProperty(isAttribute = true)
    private String ThumbPic;

    @JacksonXmlProperty(isAttribute = true)
    private String LowPic;

    @JacksonXmlProperty(isAttribute = true)
    private String Size;

    @JacksonXmlProperty(isAttribute = true)
    private String PicHeight;

    @JacksonXmlProperty(isAttribute = true)
    private String PicWidth;

    @JacksonXmlProperty(isAttribute = true)
    private String IsMain;

    @JacksonXmlProperty(isAttribute = true)
    private String Type;

    @JacksonXmlProperty(isAttribute = true)
    private String ProductPicture_ID;

    public String getPic() {
        return Pic;
    }

    public void setPic(String pic) {
        Pic = pic;
    }

    public String getThumbPic() {
        return ThumbPic;
    }

    public void setThumbPic(String thumbPic) {
        ThumbPic = thumbPic;
    }

    public String getLowPic() {
        return LowPic;
    }

    public void setLowPic(String lowPic) {
        LowPic = lowPic;
    }

    public String getSize() {
        return Size;
    }

    public void setSize(String size) {
        Size = size;
    }

    public String getPicHeight() {
        return PicHeight;
    }

    public void setPicHeight(String picHeight) {
        PicHeight = picHeight;
    }

    public String getPicWidth() {
        return PicWidth;
    }

    public void setPicWidth(String picWidth) {
        PicWidth = picWidth;
    }

    public String getIsMain() {
        return IsMain;
    }

    public void setIsMain(String isMain) {
        IsMain = isMain;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public String getProductPicture_ID() {
        return ProductPicture_ID;
    }

    public void setProductPicture_ID(String productPicture_ID) {
        ProductPicture_ID = productPicture_ID;
    }

}
