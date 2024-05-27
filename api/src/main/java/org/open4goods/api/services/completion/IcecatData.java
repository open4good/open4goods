package org.open4goods.api.services.completion;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class IcecatData {

    @JsonProperty("msg")
    public String msg;

    @JsonProperty("data")
    public IceDataItem data;

    // Getters and Setters

    public static class IceDataItem {
        @JsonProperty("Dictionary")
        public Dictionary dictionary;

        @JsonProperty("GeneralInfo")
        public GeneralInfo generalInfo;

        @JsonProperty("Image")
        public Image image;

        @JsonProperty("Multimedia")
        public List<Multimedia> multimedia;

        @JsonProperty("Gallery")
        public List<Gallery> gallery;

        @JsonProperty("FeaturesGroups")
        public List<FeaturesGroups> featuresGroups;

        @JsonProperty("FeatureLogos")
        public List<FeatureLogos> featureLogos;

        @JsonProperty("ReasonsToBuy")
        public List<ReasonsToBuy> reasonsToBuy;

        @JsonProperty("Reviews")
        public List<Reviews> reviews;

        @JsonProperty("TaxonomyDescriptions")
        public List<TaxonomyDescriptions> taxonomyDescriptions;

        @JsonProperty("ProductRelated")
        public List<ProductRelated> productRelated;

        @JsonProperty("Variants")
        public List<Variants> variants;

        @JsonProperty("ProductStory")
        public List<ProductStory> productStory;

        @JsonProperty("DemoAccount")
        public boolean demoAccount;

        // Getters and Setters
    }

    public static class Dictionary {
        @JsonProperty("link_integrate_desk")
        public String linkIntegrateDesk;
        @JsonProperty("release_date")
        public String releaseDate;
        @JsonProperty("demo_msg_part3")
        public String demoMsgPart3;
        @JsonProperty("demo_msg_part2")
        public String demoMsgPart2;
        @JsonProperty("prod_code")
        public String prodCode;
        @JsonProperty("demo_insert_desc")
        public String demoInsertDesc;
        @JsonProperty("zoom_panel_init")
        public String zoomPanelInit;
        @JsonProperty("model_name")
        public String modelName;
        @JsonProperty("product_series")
        public String productSeries;
        @JsonProperty("zoom_panel_dragg")
        public String zoomPanelDragg;
        @JsonProperty("desc")
        public String desc;
        @JsonProperty("zoom_panel_in")
        public String zoomPanelIn;
        @JsonProperty("ean_code")
        public String eanCode;
        @JsonProperty("specs")
        public String specs;
        @JsonProperty("pdf_url")
        public String pdfUrl;
        @JsonProperty("video")
        public String video;
        @JsonProperty("flash360")
        public String flash360;
        @JsonProperty("options_head_name")
        public String optionsHeadName;
        @JsonProperty("reviews_head_name")
        public String reviewsHeadName;
        @JsonProperty("marketing_text")
        public String marketingText;
        @JsonProperty("html_content")
        public String htmlContent;
        @JsonProperty("cat_name")
        public String catName;
        @JsonProperty("demo_msg_part1")
        public String demoMsgPart1;
        @JsonProperty("pdf_specs")
        public String pdfSpecs;
        @JsonProperty("back_to_top")
        public String backToTop;
        @JsonProperty("supplier_name")
        public String supplierName;
        @JsonProperty("reasons_to_buy")
        public String reasonsToBuy;
        @JsonProperty("eu_product_fiche")
        public String euProductFiche;
        @JsonProperty("eu_energy_label")
        public String euEnergyLabel;
        @JsonProperty("product_family")
        public String productFamily;
        @JsonProperty("zoom_panel_out")
        public String zoomPanelOut;

        // Getters and Setters
    }

    public static class GeneralInfo {
        @JsonProperty("IcecatId")
        public int icecatId;        
        @JsonProperty("ReleaseDate")
        public String releaseDate;
        @JsonProperty("EndOfLifeDate")
        public String endOfLifeDate;
        @JsonProperty("Title")
        public String title;
        @JsonProperty("TitleInfo")
        public TitleInfo titleInfo;
        @JsonProperty("Brand")
        public String brand;
        @JsonProperty("BrandID")
        public String brandID;
        @JsonProperty("BrandLogo")
        public String brandLogo;
        @JsonProperty("BrandInfo")
        public BrandInfo brandInfo;
        @JsonProperty("ProductName")
        public String productName;
        @JsonProperty("ProductNameInfo")
        public ProductNameInfo productNameInfo;
        @JsonProperty("BrandPartCode")
        public String brandPartCode;
        @JsonProperty("GTIN")
        public List<String> gtin;
        @JsonProperty("Category")
        public Category category;
        @JsonProperty("ProductFamily")
        public ProductFamily productFamily;
        @JsonProperty("ProductSeries")
        public ProductSeries productSeries;
        @JsonProperty("Description")
        public Description description;
        @JsonProperty("SummaryDescription")
        public SummaryDescription summaryDescription;
        @JsonProperty("BulletPoints")
        public BulletPoints bulletPoints;
        @JsonProperty("GeneratedBulletPoints")
        public GeneratedBulletPoints generatedBulletPoints;

        
        
        // Getters and Setters
    }

    public static class TitleInfo {
        @JsonProperty("GeneratedIntTitle")
        public String generatedIntTitle;
        @JsonProperty("GeneratedLocalTitle")
        public GeneratedLocalTitle generatedLocalTitle;
        @JsonProperty("BrandLocalTitle")
        public BrandLocalTitle brandLocalTitle;

        // Getters and Setters
    }

    public static class GeneratedLocalTitle {
        @JsonProperty("Value")
        public String value;
        @JsonProperty("Language")
        public String language;

        // Getters and Setters
    }

    public static class BrandLocalTitle {
        @JsonProperty("Value")
        public String value;
        @JsonProperty("Language")
        public String language;

        // Getters and Setters
    }

    public static class BrandInfo {
        @JsonProperty("BrandName")
        public String brandName;
        @JsonProperty("BrandLocalName")
        public String brandLocalName;
        @JsonProperty("BrandLogo")
        public String brandLogo;

        // Getters and Setters
    }

    public static class ProductNameInfo {
        @JsonProperty("ProductIntName")
        public String productIntName;
        @JsonProperty("ProductLocalName")
        public ProductLocalName productLocalName;

        // Getters and Setters
    }

    public static class ProductLocalName {
        @JsonProperty("Value")
        public String value;
        @JsonProperty("Language")
        public String language;

        // Getters and Setters
    }

    public static class Category {
        @JsonProperty("CategoryID")
        public String categoryID;
        @JsonProperty("Name")
        public Name name;

        // Getters and Setters
    }

    public static class Name {
        @JsonProperty("Value")
        public String value;
        @JsonProperty("Language")
        public String language;

        // Getters and Setters
    }

    public static class ProductFamily {
        @JsonProperty("ProductFamilyID")
        public String productFamilyID;

        @JsonProperty("Value")
        public String value;

        @JsonProperty("Language")
        public String language;
   }

    public static class ProductSeries {
        @JsonProperty("SeriesID")
        public String seriesID;

        @JsonProperty("Value")
        public String value;
        
        
        @JsonProperty("Language")
        public String language;
        
        
        
        
    }

    public static class Description {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("LongDesc")
        public String longDesc;
        @JsonProperty("LongProductName")
        public String longProductName;
        @JsonProperty("MiddleDesc")
        public String middleDesc;
        @JsonProperty("Disclaimer")
        public String disclaimer;
        @JsonProperty("ManualPDFURL")
        public String manualPDFURL;
        @JsonProperty("ManualPDFSize")
        public String manualPDFSize;
        @JsonProperty("LeafletPDFURL")
        public String leafletPDFURL;
        @JsonProperty("PDFSize")
        public String pdfSize;
        @JsonProperty("URL")
        public String url;
        @JsonProperty("WarrantyInfo")
        public String warrantyInfo;
        @JsonProperty("Updated")
        public String updated;
        @JsonProperty("Language")
        public String language;
        @JsonProperty("Value")
        public String value;
        // Getters and Setters
    }

    public static class SummaryDescription {
        @JsonProperty("ShortSummaryDescription")
        public String shortSummaryDescription;
        @JsonProperty("LongSummaryDescription")
        public String longSummaryDescription;

        // Getters and Setters
    }

    public static class BulletPoints {
        @JsonProperty("BulletPointsId")
        public String bulletPointsId;

        @JsonProperty("Language")
        public String language;

        @JsonProperty("Values")
        public List<String> values;

        @JsonProperty("Updated")
        public String updated;
    }

    public static class GeneratedBulletPoints {
        @JsonProperty("Language")
        public String language;
        @JsonProperty("Values")
        public List<String> values;

        // Getters and Setters
    }

    public static class Image {
        @JsonProperty("HighPic")
        public String highPic;
        @JsonProperty("HighPicSize")
        public String highPicSize;
        @JsonProperty("HighPicHeight")
        public String highPicHeight;
        @JsonProperty("HighPicWidth")
        public String highPicWidth;
        @JsonProperty("LowPic")
        public String lowPic;
        @JsonProperty("LowPicSize")
        public String lowPicSize;
        @JsonProperty("LowPicHeight")
        public String lowPicHeight;
        @JsonProperty("LowPicWidth")
        public String lowPicWidth;
        @JsonProperty("Pic500x500")
        public String pic500x500;
        @JsonProperty("Pic500x500Size")
        public String pic500x500Size;
        @JsonProperty("Pic500x500Height")
        public String pic500x500Height;
        @JsonProperty("Pic500x500Width")
        public String pic500x500Width;
        @JsonProperty("ThumbPic")
        public String thumbPic;
        @JsonProperty("ThumbPicSize")
        public String thumbPicSize;

        // Getters and Setters
    }

    public static class Multimedia {
    	  @JsonProperty("ID")
    	    public String id;

    	    @JsonProperty("URL")
    	    public String url;

    	    @JsonProperty("ConvertedURL")
    	    public String convertedURL;

    	    @JsonProperty("ThumbUrl")
    	    public String thumbUrl;
    	    
    	    @JsonProperty("PreviewUrl")
    	    public String previewUrl;
    	    
    	    
    	    
    	    
    	    @JsonProperty("ConvertedContentType")
    	    public String convertedContentType;
    	    
    	    @JsonProperty("ConvertedSize")
    	    public String convertedSize;
    	 
    	    @JsonProperty("Type")
    	    public String type;

    	    @JsonProperty("LabelType")
    	    public String labelType;

    	    @JsonProperty("ContentType")
    	    public String contentType;

    	    @JsonProperty("KeepAsUrl")
    	    public String keepAsUrl;

    	    @JsonProperty("Description")
    	    public String description;

    	    @JsonProperty("Size")
    	    public String size;

    	    @JsonProperty("IsPrivate")
    	    public String isPrivate;

    	    @JsonProperty("Updated")
    	    public String updated;

    	    @JsonProperty("Language")
    	    public String language;

    	    @JsonProperty("IsVideo")
    	    public int isVideo;
    	    
    	    @JsonProperty("EprelLink")
    	    public String eprelLink;
    }

    public static class Gallery {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("LowPic")
        public String lowPic;
        @JsonProperty("LowSize")
        public String lowSize;
        @JsonProperty("LowHeight")
        public String lowHeight;
        @JsonProperty("LowWidth")
        public String lowWidth;
        @JsonProperty("ThumbPic")
        public String thumbPic;
        @JsonProperty("ThumbPicSize")
        public String thumbPicSize;
        @JsonProperty("Pic")
        public String pic;
        @JsonProperty("Size")
        public String size;
        @JsonProperty("PicHeight")
        public String picHeight;
        @JsonProperty("PicWidth")
        public String picWidth;
        @JsonProperty("Pic500x500")
        public String pic500x500;
        @JsonProperty("Pic500x500Size")
        public String pic500x500Size;
        @JsonProperty("Pic500x500Height")
        public String pic500x500Height;
        @JsonProperty("Pic500x500Width")
        public String pic500x500Width;
        @JsonProperty("No")
        public String no;
        @JsonProperty("IsMain")
        public String isMain;
        @JsonProperty("Updated")
        public String updated;
	    @JsonProperty("IsPrivate")
	    public String isPrivate;
        @JsonProperty("Type")
        public String type;
        @JsonProperty("Attributes")
        public Attributes attributes;

        // Getters and Setters
    }

    public static class Attributes {
        @JsonProperty("OriginalFileName")
        public String originalFileName;

        // Getters and Setters
    }

    public static class FeaturesGroups {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("SortNo")
        public String sortNo;
        @JsonProperty("FeatureGroup")
        public FeatureGroup featureGroup;
        @JsonProperty("Features")
        public List<Feature> features;

        // Getters and Setters
    }

    public static class FeatureGroup {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("Name")
        public Name name;

        // Getters and Setters
    }

    public static class Feature {
        @JsonProperty("Localized")
        public String localized;
        @JsonProperty("ID")
        public String id;
        @JsonProperty("LocalID")
        public String localID;
        @JsonProperty("Type")
        public String type;
        @JsonProperty("Value")
        public String value;
        @JsonProperty("CategoryFeatureId")
        public String categoryFeatureId;
        @JsonProperty("CategoryFeatureGroupID")
        public String categoryFeatureGroupID;
        @JsonProperty("SortNo")
        public String sortNo;
        @JsonProperty("PresentationValue")
        public String presentationValue;
        @JsonProperty("RawValue")
        public String rawValue;
        @JsonProperty("LocalValue")
        public String localValue;
        @JsonProperty("Description")
        public String description;
        @JsonProperty("Mandatory")
        public String mandatory;
        @JsonProperty("Searchable")
        public String searchable;
        @JsonProperty("Optional")
        public String optional;
        @JsonProperty("Feature")
        public FeatureDetail featureDetail;

        // Getters and Setters
    }

    public static class FeatureDetail {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("Sign")
        public String sign;
        @JsonProperty("Measure")
        public Measure measure;
        @JsonProperty("Name")
        public Name name;

        // Getters and Setters
    }

    public static class Measure {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("Sign")
        public String sign;
        @JsonProperty("Signs")
        public Signs signs;

        // Getters and Setters
    }

    public static class Signs {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("_")
        public String underscore;
        @JsonProperty("Language")
        public String language;

        // Getters and Setters
    }

    public static class FeatureLogos {
    	  @JsonProperty("LogoPic")
    	    public String logoPic;

    	    @JsonProperty("Width")
    	    public String width;

    	    @JsonProperty("Height")
    	    public String height;

    	    @JsonProperty("Size")
    	    public String size;

    	    @JsonProperty("ThumbPic")
    	    public String thumbPic;

    	    @JsonProperty("FeatureID")
    	    public String featureID;

    	    @JsonProperty("Value")
    	    public String value;

    	    @JsonProperty("KeyLogo")
    	    public int keyLogo;

    	    @JsonProperty("Description")
    	    public Description description;

    	    // Getters and Setters
    }

    public static class ReasonsToBuy {
    	 @JsonProperty("ReasonToBuyID")
    	    public String reasonToBuyID;

    	    @JsonProperty("Value")
    	    public String value;

    	    @JsonProperty("HighPic")
    	    public String highPic;

    	    @JsonProperty("HighPicSize")
    	    public String highPicSize;

    	    @JsonProperty("No")
    	    public String no;

    	    @JsonProperty("Title")
    	    public String title;

    	    @JsonProperty("Language")
    	    public String language;

    	    @JsonProperty("Updated")
    	    public String updated;

    	    @JsonProperty("Origin")
    	    public String origin;

    	    @JsonProperty("IsPrivate")
    	    public String isPrivate;

    	    // Getters and Setters
    }

    public static class Reviews {
        // Empty class based on provided JSON structure

        // Getters and Setters
    }

    public static class TaxonomyDescriptions {
        // Empty class based on provided JSON structure

        // Getters and Setters
    }

    public static class ProductRelated {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("CategoryID")
        public String categoryID;
        @JsonProperty("Preferred")
        public String preferred;
        @JsonProperty("IcecatID")
        public String icecatID;
        @JsonProperty("ProductCode")
        public String productCode;
        @JsonProperty("ThumbPic")
        public String thumbPic;
        @JsonProperty("ProductName")
        public String productName;
        @JsonProperty("Brand")
        public String brand;
        @JsonProperty("BrandID")
        public String brandID;
        @JsonProperty("ProductRelatedLocales")
        public List<ProductRelatedLocales> productRelatedLocales;

        // Getters and Setters
    }

    public static class ProductRelatedLocales {
        @JsonProperty("ID")
        public String id;
        @JsonProperty("Language")
        public String language;
        @JsonProperty("Preferred")
        public String preferred;
        @JsonProperty("StartDate")
        public String startDate;
        @JsonProperty("EndDate")
        public String endDate;

        // Getters and Setters
    }

    public static class Variants {
        // Empty class based on provided JSON structure

        // Getters and Setters
    }

    public static class ProductStory {
        // Empty class based on provided JSON structure

        // Getters and Setters
    }
}