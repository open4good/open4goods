package org.open4goods.model.vertical;

import java.util.Objects;

import org.open4goods.model.Localisable;

import com.fasterxml.jackson.annotation.JsonMerge;

/**
 * Localized expandable editorial content displayed below a sub-category listing.
 */
public class VerticalSubCategoryReadMore {

    @JsonMerge
    private Localisable<String, String> title = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> shortText = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> longText = new Localisable<>();

    /**
     * Return localized section titles.
     *
     * @return localized title values
     */
    public Localisable<String, String> getTitle() {
        return title;
    }

    /**
     * Set localized section titles.
     *
     * @param title localized title values
     */
    public void setTitle(Localisable<String, String> title) {
        this.title = title;
    }

    /**
     * Return localized short text shown before expansion.
     *
     * @return localized markdown short text
     */
    public Localisable<String, String> getShortText() {
        return shortText;
    }

    /**
     * Set localized short text shown before expansion.
     *
     * @param shortText localized markdown short text
     */
    public void setShortText(Localisable<String, String> shortText) {
        this.shortText = shortText;
    }

    /**
     * Return localized long text shown after expansion.
     *
     * @return localized markdown long text
     */
    public Localisable<String, String> getLongText() {
        return longText;
    }

    /**
     * Set localized long text shown after expansion.
     *
     * @param longText localized markdown long text
     */
    public void setLongText(Localisable<String, String> longText) {
        this.longText = longText;
    }

    @Override
    public int hashCode() {
        return Objects.hash(longText, shortText, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VerticalSubCategoryReadMore other = (VerticalSubCategoryReadMore) obj;
        return Objects.equals(title, other.title)
                && Objects.equals(shortText, other.shortText)
                && Objects.equals(longText, other.longText);
    }
}
