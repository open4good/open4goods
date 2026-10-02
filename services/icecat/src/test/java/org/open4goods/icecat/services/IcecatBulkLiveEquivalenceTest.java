package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.icecat.jaxb.BulletPoint;
import org.open4goods.icecat.jaxb.BulletPoints;
import org.open4goods.icecat.jaxb.Category;
import org.open4goods.icecat.jaxb.EANCode;
import org.open4goods.icecat.jaxb.Feature;
import org.open4goods.icecat.jaxb.FeatureLogo;
import org.open4goods.icecat.jaxb.LocalValue;
import org.open4goods.icecat.jaxb.Measure;
import org.open4goods.icecat.jaxb.MultimediaObject;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.jaxb.ObjectFactory;
import org.open4goods.icecat.jaxb.Product;
import org.open4goods.icecat.jaxb.ProductDescription;
import org.open4goods.icecat.jaxb.ProductFamily;
import org.open4goods.icecat.jaxb.ProductFeature;
import org.open4goods.icecat.jaxb.ProductGallery;
import org.open4goods.icecat.jaxb.ProductMultimediaObject;
import org.open4goods.icecat.jaxb.ProductPicture;
import org.open4goods.icecat.jaxb.Supplier;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Description;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureDetail;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeaturesGroups;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Gallery;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Image;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Multimedia;

/**
 * GOU-40 AC1/AC8: a bulk catalogue import and a live per-product refresh of the same
 * underlying Icecat product must converge on the same {@link SourceRecordHead}. This test
 * builds the same product two ways - once as a live-API {@link IceDataItem}, once as a JAXB
 * bulk-export {@link Product} carrying extra other-language data the live call never sees -
 * and checks {@link IcecatSourceRecordAdapter} produces an identical head for both.
 */
class IcecatBulkLiveEquivalenceTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();
    private static final Instant RETRIEVED = Instant.parse("2026-09-30T12:00:00Z");
    private static final LanguageTag EN = new LanguageTag("en");
    private static final LanguageTag FR = new LanguageTag("fr");
    private final IcecatSourceRecordAdapter adapter = new IcecatSourceRecordAdapter();

    @Test
    void bulkImportAndLiveRefreshOfTheSameProductConvergeOnTheSameHead() {
        SourceRecordMutation liveMutation = adapter.adapt(liveItem(), EN, Set.of(EN), "v1", RETRIEVED).orElseThrow();
        SourceRecordMutation bulkMutation = adapter.adapt(bulkProduct(), EN, Set.of(EN), "v1", RETRIEVED, IcecatBulkLiveEquivalenceTest::languageOf)
                .orElseThrow();

        SourceRecordHead liveHead = liveMutation.candidate();
        SourceRecordHead bulkHead = bulkMutation.candidate();

        assertThat(bulkHead.key()).isEqualTo(liveHead.key());
        assertThat(bulkHead.payloadHash()).isEqualTo(liveHead.payloadHash());
        assertThat(bulkHead.gtinLinks()).isEqualTo(liveHead.gtinLinks());
        assertThat(bulkHead.assertions()).containsExactlyInAnyOrderElementsOf(liveHead.assertions());
    }

    private static LanguageTag languageOf(int langid) {
        return switch (langid) {
            case 1 -> EN;
            case 2 -> FR;
            default -> null;
        };
    }

    private static IceDataItem liveItem() {
        IceDataItem item = new IceDataItem();
        item.generalInfo = new GeneralInfo();
        item.generalInfo.icecatId = 4502;
        item.generalInfo.title = "Robot vacuum X9";
        item.generalInfo.productName = "X9 Pro";
        item.generalInfo.brand = "AcmeBrand";
        item.generalInfo.brandPartCode = "X9-PRO-001";
        item.generalInfo.releaseDate = "2024-01-15";
        item.generalInfo.gtin = List.of("4006381333931");

        item.generalInfo.category = new org.open4goods.icecat.model.IcecatLiveApiResponse.Category();
        item.generalInfo.category.categoryID = "123";
        item.generalInfo.category.name = new org.open4goods.icecat.model.IcecatLiveApiResponse.Name();
        item.generalInfo.category.name.value = "Vacuum cleaners";
        item.generalInfo.category.name.language = "en";

        item.generalInfo.productFamily = new org.open4goods.icecat.model.IcecatLiveApiResponse.ProductFamily();
        item.generalInfo.productFamily.productFamilyID = "55";
        item.generalInfo.productFamily.value = "X Series";
        item.generalInfo.productFamily.language = "en";

        item.generalInfo.description = new Description();
        item.generalInfo.description.longDesc = "Long description text";
        item.generalInfo.description.leafletPDFURL = "https://icecat.example/leaflet.pdf";
        item.generalInfo.description.manualPDFURL = "https://icecat.example/manual.pdf";

        item.generalInfo.bulletPoints = new org.open4goods.icecat.model.IcecatLiveApiResponse.BulletPoints();
        item.generalInfo.bulletPoints.language = "en";
        item.generalInfo.bulletPoints.values = List.of("Fast", "Quiet");

        FeaturesGroups numericGroup = new FeaturesGroups();
        numericGroup.features = List.of(numericFeature(), localizedFeature("Stainless steel finish"));
        item.featuresGroups = List.of(numericGroup);

        item.image = new Image();
        item.image.highPic = "https://icecat.example/high.jpg";

        Gallery gallery = new Gallery();
        gallery.id = "g1";
        gallery.pic = "https://icecat.example/gallery/1.jpg";
        item.gallery = List.of(gallery);

        Multimedia multimedia = new Multimedia();
        multimedia.id = "501";
        multimedia.url = "https://icecat.example/video.mp4";
        multimedia.contentType = "video/mp4";
        multimedia.type = "video";
        multimedia.language = "en";
        item.multimedia = List.of(multimedia);

        var logo = new org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureLogos();
        logo.featureID = "10";
        logo.logoPic = "https://icecat.example/logo10.png";
        item.featureLogos = List.of(logo);

        return item;
    }

    private static org.open4goods.icecat.model.IcecatLiveApiResponse.Feature numericFeature() {
        var feature = new org.open4goods.icecat.model.IcecatLiveApiResponse.Feature();
        feature.id = "77";
        feature.categoryFeatureId = "77";
        feature.localized = "0";
        feature.rawValue = "15";
        feature.value = "15";
        feature.featureDetail = new FeatureDetail();
        feature.featureDetail.id = "77";
        feature.featureDetail.sign = "cm";
        return feature;
    }

    private static org.open4goods.icecat.model.IcecatLiveApiResponse.Feature localizedFeature(String value) {
        var feature = new org.open4goods.icecat.model.IcecatLiveApiResponse.Feature();
        feature.id = "99";
        feature.categoryFeatureId = "99";
        feature.localized = "1";
        feature.rawValue = value;
        feature.value = value;
        return feature;
    }

    private static Product bulkProduct() {
        Product product = new Product();
        product.setID("4502");
        product.setName("X9 Pro");
        product.setProdId("X9-PRO-001");
        product.setThumbPic("https://icecat.example/thumb.jpg");
        product.setTitle("Robot vacuum X9");
        product.setReleaseDate("2024-01-15");
        product.setHighPic("https://icecat.example/high.jpg");

        List<jakarta.xml.bind.JAXBElement<?>> elements = product.getCategoryFeatureGroupOrCategoryOrReleaseDate();

        Category category = new Category();
        category.setID(BigInteger.valueOf(123));
        Name categoryName = new Name();
        categoryName.setValueAttribute("Vacuum cleaners");
        categoryName.setLangid(BigInteger.ONE);
        category.getName().add(categoryName);
        elements.add(FACTORY.createProductCategory(category));

        ProductFamily family = new ProductFamily();
        family.setID(BigInteger.valueOf(55));
        Name familyName = new Name();
        familyName.setValueAttribute("X Series");
        familyName.setLangid(BigInteger.ONE);
        family.getName().add(familyName);
        elements.add(FACTORY.createProductProductFamily(family));

        Supplier supplier = new Supplier();
        supplier.setID(BigInteger.valueOf(1));
        supplier.setName("AcmeBrand");
        elements.add(FACTORY.createProductSupplier(supplier));

        EANCode ean = new EANCode();
        ean.setEAN(new BigInteger("4006381333931"));
        elements.add(FACTORY.createProductEANCode(ean));

        ProductDescription englishDescription = new ProductDescription();
        englishDescription.setLangid(BigInteger.ONE);
        englishDescription.setLongDesc("Long description text");
        englishDescription.setPDFURL("https://icecat.example/leaflet.pdf");
        englishDescription.setManualPDFURL("https://icecat.example/manual.pdf");
        elements.add(FACTORY.createProductProductDescription(englishDescription));
        ProductDescription frenchDescription = new ProductDescription();
        frenchDescription.setLangid(BigInteger.TWO);
        frenchDescription.setLongDesc("Texte de description longue");
        elements.add(FACTORY.createProductProductDescription(frenchDescription));

        BulletPoints bulletPoints = new BulletPoints();
        bulletPoints.getBulletPoint().add(bulletPoint("Fast", 1, 0));
        bulletPoints.getBulletPoint().add(bulletPoint("Quiet", 1, 1));
        bulletPoints.getBulletPoint().add(bulletPoint("Rapide", 2, 0));
        elements.add(FACTORY.createProductBulletPoints(bulletPoints));

        elements.add(FACTORY.createProductProductFeature(numericProductFeature()));
        elements.add(FACTORY.createProductProductFeature(localizedProductFeature()));

        ProductGallery gallery = new ProductGallery();
        ProductPicture picture = new ProductPicture();
        picture.setProductPictureID("g1");
        picture.setPic("https://icecat.example/gallery/1.jpg");
        gallery.getProductPicture().add(picture);
        elements.add(FACTORY.createProductProductGallery(gallery));

        ProductMultimediaObject multimediaWrapper = new ProductMultimediaObject();
        multimediaWrapper.getMultimediaObject().add(multimediaObject("https://icecat.example/video.mp4", "video/mp4", "video", 1));
        multimediaWrapper.getMultimediaObject().add(multimediaObject("https://icecat.example/video-fr.mp4", "video/mp4", "video", 2));
        elements.add(FACTORY.createProductProductMultimediaObject(multimediaWrapper));

        FeatureLogo logo = new FeatureLogo();
        logo.setID(BigInteger.valueOf(1));
        logo.setFeatureID(BigInteger.valueOf(10));
        logo.setLogoPic("https://icecat.example/logo10.png");
        logo.setWidth(BigInteger.valueOf(32));
        logo.setHeight(BigInteger.valueOf(32));
        logo.setSize(BigInteger.valueOf(1024));
        elements.add(FACTORY.createProductFeatureLogo(logo));

        return product;
    }

    private static BulletPoint bulletPoint(String value, int langid, int no) {
        BulletPoint point = new BulletPoint();
        point.setValue(value);
        point.setLangid(langid);
        point.setNo(no);
        return point;
    }

    private static MultimediaObject multimediaObject(String url, String contentType, String type, int langid) {
        MultimediaObject media = new MultimediaObject();
        media.setMultimediaObjectID(BigInteger.valueOf(501));
        media.setURL(url);
        media.setContentType(contentType);
        media.setType(type);
        media.setLangid(BigInteger.valueOf(langid));
        media.setDate("2024-01-01");
        media.setKeepAsURL(false);
        media.setHeight(BigInteger.ZERO);
        media.setWidth(BigInteger.ZERO);
        return media;
    }

    private static ProductFeature numericProductFeature() {
        ProductFeature feature = new ProductFeature();
        feature.setCategoryFeatureGroupID(BigInteger.valueOf(1));
        feature.setCategoryFeatureID(BigInteger.valueOf(77));
        feature.setID(BigInteger.valueOf(77));
        feature.setNo(BigInteger.ZERO);
        feature.setLocalized(false);
        feature.setValue("15");

        Feature metadata = new Feature();
        metadata.setID(BigInteger.valueOf(77));
        Measure measure = new Measure();
        measure.setSignAttribute("cm");
        metadata.setMeasure(measure);
        feature.getFeature().add(metadata);
        return feature;
    }

    private static ProductFeature localizedProductFeature() {
        ProductFeature feature = new ProductFeature();
        feature.setCategoryFeatureGroupID(BigInteger.valueOf(1));
        feature.setCategoryFeatureID(BigInteger.valueOf(99));
        feature.setID(BigInteger.valueOf(99));
        feature.setNo(BigInteger.ZERO);
        feature.setLocalized(true);
        feature.setValue("Stainless steel finish");

        LocalValue english = new LocalValue();
        english.setLangid(1);
        english.setValue("Stainless steel finish");
        LocalValue french = new LocalValue();
        french.setLangid(2);
        french.setValue("Finition inox");
        feature.getLocalValue().add(english);
        feature.getLocalValue().add(french);
        return feature;
    }
}
