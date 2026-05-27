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
        return Objects.hash(activatedFilters, description, h1Title, id, image, slug);
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
                && Objects.equals(image, other.image)
                && Objects.equals(activatedFilters, other.activatedFilters);
    }
}
