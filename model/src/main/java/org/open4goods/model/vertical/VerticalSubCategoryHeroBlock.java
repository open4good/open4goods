package org.open4goods.model.vertical;

import java.util.Objects;

import org.open4goods.model.Localisable;

import com.fasterxml.jackson.annotation.JsonMerge;

/**
 * Localized markdown content displayed beside a vertical sub-category hero.
 */
public class VerticalSubCategoryHeroBlock {

    @JsonMerge
    private Localisable<String, String> title = new Localisable<>();

    @JsonMerge
    private Localisable<String, String> body = new Localisable<>();

    private String mdiIcon;

    /**
     * Return localized card titles.
     *
     * @return localized title values
     */
    public Localisable<String, String> getTitle() {
        return title;
    }

    /**
     * Set localized card titles.
     *
     * @param title localized title values
     */
    public void setTitle(Localisable<String, String> title) {
        this.title = title;
    }

    /**
     * Return localized markdown body values.
     *
     * @return localized markdown body
     */
    public Localisable<String, String> getBody() {
        return body;
    }

    /**
     * Set localized markdown body values.
     *
     * @param body localized markdown body
     */
    public void setBody(Localisable<String, String> body) {
        this.body = body;
    }

    /**
     * Return the Material Design icon decorating the hero information card.
     *
     * @return MDI icon name
     */
    public String getMdiIcon() {
        return mdiIcon;
    }

    /**
     * Set the Material Design icon decorating the hero information card.
     *
     * @param mdiIcon MDI icon name
     */
    public void setMdiIcon(String mdiIcon) {
        this.mdiIcon = mdiIcon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, mdiIcon, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        VerticalSubCategoryHeroBlock other = (VerticalSubCategoryHeroBlock) obj;
        return Objects.equals(title, other.title) && Objects.equals(body, other.body)
                && Objects.equals(mdiIcon, other.mdiIcon);
    }
}
