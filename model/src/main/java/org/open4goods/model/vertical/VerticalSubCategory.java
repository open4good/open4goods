package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.Localisable;

import com.fasterxml.jackson.annotation.JsonMerge;

/**
 * Search-intent landing page configured under a vertical.
 *
 * <p>A sub-category keeps the parent vertical routing and product model, while
 * applying a fixed set of filter criteria and localized page copy.</p>
 */
public class VerticalSubCategory {

    private String id;

    @JsonMerge
    private Localisable<String, String> slug = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> h1Title = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> description = new Localisable<>();

    @JsonMerge
    private VerticalSubCategoryHeroBlock heroBlock;

    @JsonMerge
    private VerticalSubCategoryReadMore readMore;

    @JsonMerge
    private Localisable<String, String> metaTitle = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> metaDescription = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> metaOpenGraphTitle = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> metaOpenGraphDescription = new Localisable<>();

    private String image;

    @JsonMerge
    private List<SubsetCriteria> activatedFilters = new ArrayList<>();

    /**
     * Return the stable sub-category identifier.
     *
     * @return configured identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Set the stable sub-category identifier.
     *
     * @param id configured identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Return localized URL slugs.
     *
     * @return localized slug values
     */
    public Localisable<String, String> getSlug() {
        return slug;
    }

    /**
     * Set localized URL slugs.
     *
     * @param slug localized slug values
     */
    public void setSlug(Localisable<String, String> slug) {
        this.slug = slug;
    }

    /**
     * Return localized page titles.
     *
     * @return localized title values
     */
    public Localisable<String, String> getH1Title() {
        return h1Title;
    }

    /**
     * Set localized page titles.
     *
     * @param h1Title localized title values
     */
    public void setH1Title(Localisable<String, String> h1Title) {
        this.h1Title = h1Title;
    }

    /**
     * Return localized page descriptions.
     *
     * @return localized description values
     */
    public Localisable<String, String> getDescription() {
        return description;
    }

    /**
     * Set localized page descriptions.
     *
     * @param description localized description values
     */
    public void setDescription(Localisable<String, String> description) {
        this.description = description;
    }

    /**
     * Return the optional localized hero information block.
     *
     * @return localized hero information block
     */
    public VerticalSubCategoryHeroBlock getHeroBlock() {
        return heroBlock;
    }

    /**
     * Set the optional localized hero information block.
     *
     * @param heroBlock localized hero information block
     */
    public void setHeroBlock(VerticalSubCategoryHeroBlock heroBlock) {
        this.heroBlock = heroBlock;
    }

    /**
     * Return the optional expandable editorial section.
     *
     * @return localized read-more content
     */
    public VerticalSubCategoryReadMore getReadMore() {
        return readMore;
    }

    /**
     * Set the optional expandable editorial section.
     *
     * @param readMore localized read-more content
     */
    public void setReadMore(VerticalSubCategoryReadMore readMore) {
        this.readMore = readMore;
    }

    /**
     * Return localized SEO meta titles.
     *
     * @return localized meta title values
     */
    public Localisable<String, String> getMetaTitle() {
        return metaTitle;
    }

    /**
     * Set localized SEO meta titles.
     *
     * @param metaTitle localized meta title values
     */
    public void setMetaTitle(Localisable<String, String> metaTitle) {
        this.metaTitle = metaTitle;
    }

    /**
     * Return localized SEO meta descriptions.
     *
     * @return localized meta description values
     */
    public Localisable<String, String> getMetaDescription() {
        return metaDescription;
    }

    /**
     * Set localized SEO meta descriptions.
     *
     * @param metaDescription localized meta description values
     */
    public void setMetaDescription(Localisable<String, String> metaDescription) {
        this.metaDescription = metaDescription;
    }

    /**
     * Return localized Open Graph titles.
     *
     * @return localized Open Graph title values
     */
    public Localisable<String, String> getMetaOpenGraphTitle() {
        return metaOpenGraphTitle;
    }

    /**
     * Set localized Open Graph titles.
     *
     * @param metaOpenGraphTitle localized Open Graph title values
     */
    public void setMetaOpenGraphTitle(Localisable<String, String> metaOpenGraphTitle) {
        this.metaOpenGraphTitle = metaOpenGraphTitle;
    }

    /**
     * Return localized Open Graph descriptions.
     *
     * @return localized Open Graph description values
     */
    public Localisable<String, String> getMetaOpenGraphDescription() {
        return metaOpenGraphDescription;
    }

    /**
     * Set localized Open Graph descriptions.
     *
     * @param metaOpenGraphDescription localized Open Graph description values
     */
    public void setMetaOpenGraphDescription(Localisable<String, String> metaOpenGraphDescription) {
        this.metaOpenGraphDescription = metaOpenGraphDescription;
    }

    /**
     * Return the optional page image override.
     *
     * @return image URL or path
     */
    public String getImage() {
        return image;
    }

    /**
     * Set the optional page image override.
     *
     * @param image image URL or path
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Return criteria that are always applied on the sub-category page.
     *
     * @return fixed filter criteria
     */
    public List<SubsetCriteria> getActivatedFilters() {
        return activatedFilters;
    }

    /**
     * Set criteria that are always applied on the sub-category page.
     *
     * @param activatedFilters fixed filter criteria
     */
    public void setActivatedFilters(List<SubsetCriteria> activatedFilters) {
        this.activatedFilters = activatedFilters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(activatedFilters, description, h1Title, heroBlock, id, image,
                metaDescription, metaOpenGraphDescription, metaOpenGraphTitle, metaTitle, readMore, slug);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VerticalSubCategory other = (VerticalSubCategory) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(slug, other.slug)
                && Objects.equals(h1Title, other.h1Title)
                && Objects.equals(description, other.description)
                && Objects.equals(heroBlock, other.heroBlock)
                && Objects.equals(readMore, other.readMore)
                && Objects.equals(metaTitle, other.metaTitle)
                && Objects.equals(metaDescription, other.metaDescription)
                && Objects.equals(metaOpenGraphTitle, other.metaOpenGraphTitle)
                && Objects.equals(metaOpenGraphDescription, other.metaOpenGraphDescription)
                && Objects.equals(image, other.image)
                && Objects.equals(activatedFilters, other.activatedFilters);
    }
}
