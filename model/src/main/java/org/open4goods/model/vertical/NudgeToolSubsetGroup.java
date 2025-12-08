package org.open4goods.model.vertical;

import java.util.Objects;

import org.open4goods.model.Localisable;

/**
 * Metadata describing a subset group displayed by the nudge tool.
 * Allows the frontend to render group-level headings and layout hints.
 */
public class NudgeToolSubsetGroup {

    private String id;

    /**
     * Localised title shown above the group's subset options.
     */
    private Localisable<String, String> title = new Localisable<>();

    /**
     * Optional helper description for the group.
     */
    private Localisable<String, String> description = new Localisable<>();

    /**
     * Material Design icon decorating the group header.
     */
    private String mdiIcon;

    /**
     * Layout hint consumed by the UI (e.g. grid or list).
     */
    private String layout;

    /**
     * Customisable CTA label for the group step when multi-select is enabled.
     */
    private Localisable<String, String> ctaLabel = new Localisable<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Localisable<String, String> getTitle() {
        return title;
    }

    public void setTitle(Localisable<String, String> title) {
        this.title = title;
    }

    public Localisable<String, String> getDescription() {
        return description;
    }

    public void setDescription(Localisable<String, String> description) {
        this.description = description;
    }

    public String getMdiIcon() {
        return mdiIcon;
    }

    public void setMdiIcon(String mdiIcon) {
        this.mdiIcon = mdiIcon;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public Localisable<String, String> getCtaLabel() {
        return ctaLabel;
    }

    public void setCtaLabel(Localisable<String, String> ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NudgeToolSubsetGroup)) {
            return false;
        }
        NudgeToolSubsetGroup that = (NudgeToolSubsetGroup) o;
        return Objects.equals(id, that.id)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(mdiIcon, that.mdiIcon)
                && Objects.equals(layout, that.layout)
                && Objects.equals(ctaLabel, that.ctaLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, mdiIcon, layout, ctaLabel);
    }
}
