package org.open4goods.icecat.services.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.icecat.jaxb.BulletPoints;
import org.open4goods.icecat.jaxb.Category;
import org.open4goods.icecat.jaxb.EANCode;
import org.open4goods.icecat.jaxb.FeatureLogo;
import org.open4goods.icecat.jaxb.LocalValue;
import org.open4goods.icecat.jaxb.MultimediaObject;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.jaxb.Product;
import org.open4goods.icecat.jaxb.ProductDescription;
import org.open4goods.icecat.jaxb.ProductFamily;
import org.open4goods.icecat.jaxb.ProductFeature;
import org.open4goods.icecat.jaxb.ProductGallery;
import org.open4goods.icecat.jaxb.ProductMultimediaObject;
import org.open4goods.icecat.jaxb.ProductPicture;
import org.open4goods.icecat.jaxb.Supplier;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Description;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Feature;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureDetail;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureLogos;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeaturesGroups;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Gallery;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Image;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Multimedia;

import jakarta.xml.bind.JAXBElement;

/**
 * Maps a JAXB-generated Icecat bulk-export {@link Product} onto the same neutral
 * {@link IceDataItem} shape the live API client produces, so that
 * {@code IcecatSourceRecordAdapter.adapt} can turn either path into an identical
 * {@code SourceRecordHead} for the same underlying Icecat product id.
 *
 * <p>Each bulk export file describes one product in one language: its scalar attributes
 * ({@code Title}, {@code ReleaseDate}, ...) already belong to that language, exactly like a
 * live-API response. The handful of elements that do carry an explicit {@code langid} (per-
 * language title/description/bullet-point variants, kept in the same file for convenience)
 * are filtered down to the caller-selected {@link LanguageTag} using {@code languageOf}, with
 * the matching Product-level attribute as fallback when no such element is present.
 *
 * <p>Only the fields {@code IcecatSourceRecordAdapter} turns into assertions are mapped;
 * bulk-only metadata without a live-API analogue (variants, reviews, related products) is
 * intentionally left out, the same way {@code IceDataItem} itself ignores it.
 */
public final class IcecatBulkProductConverter {

    private IcecatBulkProductConverter() {
    }

    /**
     * @param product bulk-export product to convert
     * @param language language this bulk file is considered to represent
     * @param languageOf resolves an Icecat {@code langid} to the language it designates
     * @return a live-API-shaped item carrying the same identity, text, features and media
     */
    public static IceDataItem toLiveModel(Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        Objects.requireNonNull(product, "product must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(languageOf, "languageOf must not be null");

        IceDataItem item = new IceDataItem();
        item.generalInfo = generalInfo(product, language, languageOf);
        item.image = image(product);
        item.gallery = gallery(product);
        item.multimedia = multimedia(product, language, languageOf);
        item.featuresGroups = featuresGroups(product, language, languageOf);
        item.featureLogos = featureLogos(product);
        return item;
    }

    private static GeneralInfo generalInfo(Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        GeneralInfo info = new GeneralInfo();
        info.icecatId = parseIcecatId(product.getID());
        info.title = product.getTitle();
        info.productName = product.getName();
        info.brandPartCode = product.getProdId();
        info.releaseDate = product.getReleaseDate();
        info.brand = firstElement(product, Supplier.class).map(IcecatBulkModelSupport::effectiveName).orElse(null);
        info.gtin = gtins(product);
        info.category = category(product, language, languageOf);
        info.productFamily = productFamily(product, language, languageOf);
        info.description = description(product, language, languageOf);
        info.bulletPoints = bulletPoints(product, language, languageOf);
        return info;
    }

    private static org.open4goods.icecat.model.IcecatLiveApiResponse.Category category(
            Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        return firstElement(product, Category.class).map(category -> {
            org.open4goods.icecat.model.IcecatLiveApiResponse.Category result =
                    new org.open4goods.icecat.model.IcecatLiveApiResponse.Category();
            result.categoryID = category.getID() == null ? null : category.getID().toString();
            String value = matchingName(category.getName(), language, languageOf);
            if (value != null) {
                result.name = new org.open4goods.icecat.model.IcecatLiveApiResponse.Name();
                result.name.value = value;
                result.name.language = language.value();
            }
            return result;
        }).orElse(null);
    }

    private static org.open4goods.icecat.model.IcecatLiveApiResponse.ProductFamily productFamily(
            Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        return firstElement(product, ProductFamily.class).map(family -> {
            org.open4goods.icecat.model.IcecatLiveApiResponse.ProductFamily result =
                    new org.open4goods.icecat.model.IcecatLiveApiResponse.ProductFamily();
            result.productFamilyID = family.getID() == null ? null : family.getID().toString();
            String value = matchingName(family.getName(), language, languageOf);
            result.value = value != null ? value : family.getNameAttribute();
            result.language = language.value();
            return result;
        }).orElse(null);
    }

    private static Description description(Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        List<ProductDescription> descriptions = elements(product, ProductDescription.class);
        Optional<ProductDescription> matched = descriptions.stream()
                .filter(candidate -> matchesLanguage(candidate.getLangid(), language, languageOf))
                .findFirst()
                .or(descriptions.stream()::findFirst);
        if (matched.isEmpty()) {
            return null;
        }
        ProductDescription source = matched.orElseThrow();
        Description description = new Description();
        description.longDesc = source.getLongDesc();
        description.middleDesc = source.getMiddleDesc();
        description.leafletPDFURL = source.getPDFURL();
        description.manualPDFURL = source.getManualPDFURL();
        return description;
    }

    private static org.open4goods.icecat.model.IcecatLiveApiResponse.BulletPoints bulletPoints(
            Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        return firstElement(product, BulletPoints.class).map(bulletPoints -> {
            List<String> matching = bulletPoints.getBulletPoint().stream()
                    .filter(point -> matchesLanguage(java.math.BigInteger.valueOf(point.getLangid()), language, languageOf))
                    .sorted((a, b) -> Integer.compare(a.getNo(), b.getNo()))
                    .map(org.open4goods.icecat.jaxb.BulletPoint::getValue)
                    .toList();
            if (matching.isEmpty()) {
                return null;
            }
            org.open4goods.icecat.model.IcecatLiveApiResponse.BulletPoints result =
                    new org.open4goods.icecat.model.IcecatLiveApiResponse.BulletPoints();
            result.language = language.value();
            result.values = matching;
            return result;
        }).orElse(null);
    }

    private static List<String> gtins(Product product) {
        List<String> values = new ArrayList<>();
        for (EANCode ean : elements(product, EANCode.class)) {
            if (ean.getEAN() != null) {
                values.add(ean.getEAN().toString());
            }
        }
        if (StringUtils.isNotBlank(product.getEANCode())) {
            values.add(product.getEANCode().trim());
        }
        return values.isEmpty() ? null : values;
    }

    private static Image image(Product product) {
        if (StringUtils.isBlank(product.getHighPic())) {
            return null;
        }
        Image image = new Image();
        image.highPic = product.getHighPic();
        return image;
    }

    private static List<Gallery> gallery(Product product) {
        List<Gallery> galleries = new ArrayList<>();
        for (ProductGallery productGallery : elements(product, ProductGallery.class)) {
            for (ProductPicture picture : productGallery.getProductPicture()) {
                if (StringUtils.isBlank(picture.getPic())) {
                    continue;
                }
                Gallery gallery = new Gallery();
                gallery.id = picture.getProductPictureID();
                gallery.pic = picture.getPic();
                galleries.add(gallery);
            }
        }
        return galleries.isEmpty() ? null : galleries;
    }

    private static List<Multimedia> multimedia(Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        List<Multimedia> result = new ArrayList<>();
        for (ProductMultimediaObject productMultimedia : elements(product, ProductMultimediaObject.class)) {
            for (MultimediaObject media : productMultimedia.getMultimediaObject()) {
                if (StringUtils.isBlank(media.getURL()) || !matchesLanguage(media.getLangid(), language, languageOf)) {
                    continue;
                }
                Multimedia item = new Multimedia();
                item.id = media.getMultimediaObjectID() == null ? null : media.getMultimediaObjectID().toString();
                item.url = media.getURL();
                item.contentType = media.getContentType();
                item.type = media.getType();
                item.language = language.value();
                result.add(item);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static List<FeatureLogos> featureLogos(Product product) {
        List<FeatureLogos> result = new ArrayList<>();
        for (FeatureLogo logo : elements(product, FeatureLogo.class)) {
            if (StringUtils.isBlank(logo.getLogoPic())) {
                continue;
            }
            FeatureLogos item = new FeatureLogos();
            item.logoPic = logo.getLogoPic();
            item.featureID = logo.getFeatureID() == null ? null : logo.getFeatureID().toString();
            result.add(item);
        }
        return result.isEmpty() ? null : result;
    }

    private static List<FeaturesGroups> featuresGroups(
            Product product, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        Map<String, List<Feature>> byGroup = new LinkedHashMap<>();
        for (ProductFeature productFeature : elements(product, ProductFeature.class)) {
            if (productFeature.getCategoryFeatureID() == null) {
                continue;
            }
            String featureId = productFeature.getCategoryFeatureID().toString();
            boolean localized = productFeature.isLocalized();
            String rawValue = localized ? localizedValue(productFeature, language, languageOf) : productFeature.getValue();
            if (StringUtils.isBlank(rawValue)) {
                continue;
            }
            Feature feature = new Feature();
            feature.id = featureId;
            feature.categoryFeatureId = featureId;
            feature.localized = localized ? "1" : "0";
            feature.rawValue = rawValue;
            feature.value = rawValue;
            feature.featureDetail = featureDetail(productFeature, featureId);
            String groupId = productFeature.getCategoryFeatureGroupID() == null
                    ? "" : productFeature.getCategoryFeatureGroupID().toString();
            byGroup.computeIfAbsent(groupId, key -> new ArrayList<>()).add(feature);
        }
        if (byGroup.isEmpty()) {
            return null;
        }
        List<FeaturesGroups> groups = new ArrayList<>();
        for (Map.Entry<String, List<Feature>> entry : byGroup.entrySet()) {
            FeaturesGroups group = new FeaturesGroups();
            group.id = entry.getKey();
            group.features = entry.getValue();
            groups.add(group);
        }
        return groups;
    }

    private static String localizedValue(ProductFeature productFeature, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        for (LocalValue localValue : productFeature.getLocalValue()) {
            if (matchesLanguage(java.math.BigInteger.valueOf(localValue.getLangid()), language, languageOf)) {
                return localValue.getValue();
            }
        }
        return StringUtils.firstNonBlank(productFeature.getLocalValueAttribute(), productFeature.getValue());
    }

    private static FeatureDetail featureDetail(ProductFeature productFeature, String featureId) {
        if (productFeature.getFeature().isEmpty()) {
            return null;
        }
        org.open4goods.icecat.jaxb.Feature metadata = productFeature.getFeature().get(0);
        FeatureDetail detail = new FeatureDetail();
        detail.id = featureId;
        if (metadata.getMeasure() != null) {
            detail.sign = StringUtils.firstNonBlank(metadata.getMeasure().getSignAttribute(), metadata.getMeasure().getSign());
        }
        return detail;
    }

    private static String matchingName(List<Name> names, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        return names.stream()
                .filter(name -> matchesLanguage(name.getLangid(), language, languageOf))
                .findFirst()
                .map(name -> StringUtils.firstNonBlank(name.getValueAttribute(), name.getValue()))
                .or(() -> names.stream().findFirst().map(name -> StringUtils.firstNonBlank(name.getValueAttribute(), name.getValue())))
                .orElse(null);
    }

    private static boolean matchesLanguage(java.math.BigInteger langid, LanguageTag language, IntFunction<LanguageTag> languageOf) {
        if (langid == null) {
            return false;
        }
        LanguageTag resolved = languageOf.apply(langid.intValue());
        return language.equals(resolved);
    }

    private static int parseIcecatId(String id) {
        try {
            return id == null ? 0 : Integer.parseInt(id.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static <T> List<T> elements(Product product, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (JAXBElement<?> element : product.getCategoryFeatureGroupOrCategoryOrReleaseDate()) {
            if (element != null && type.isInstance(element.getValue())) {
                result.add(type.cast(element.getValue()));
            }
        }
        return result;
    }

    private static <T> Optional<T> firstElement(Product product, Class<T> type) {
        return elements(product, type).stream().findFirst();
    }
}
