package org.open4goods.model.vertical;

import java.util.Objects;

import org.open4goods.model.Localisable;

/**
 * Describes a score threshold highlighted by the nudge tool.
 */
public class NudgeToolScore {

    private String scoreName;
    private Double scoreMinValue;
    private String mdiIcon;
    private Localisable<String, String> title = new Localisable<>();
    private Localisable<String, String> description = new Localisable<>();
    /**
     * Flag indicating the score should be disabled in the UI.
     */
    private Boolean disabled;

    public String getScoreName() {
        return scoreName;
    }

    public void setScoreName(String scoreName) {
        this.scoreName = scoreName;
    }

    public Double getScoreMinValue() {
        return scoreMinValue;
    }

    public void setScoreMinValue(Double scoreMinValue) {
        this.scoreMinValue = scoreMinValue;
    }

    private Integer fromPercent;
    private Integer toPercent;

    public Integer getFromPercent() {
        return fromPercent;
    }

    public void setFromPercent(Integer fromPercent) {
        this.fromPercent = fromPercent;
    }

    public Integer getToPercent() {
        return toPercent;
    }

    public void setToPercent(Integer toPercent) {
        this.toPercent = toPercent;
    }

    public String getMdiIcon() {
        return mdiIcon;
    }

    public void setMdiIcon(String mdiIcon) {
        this.mdiIcon = mdiIcon;
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

    /**
     * Returns whether this score is disabled in the UI.
     *
     * @return {@code true} when the score should be disabled, {@code false} otherwise.
     */
    public Boolean getDisabled() {
        return disabled;
    }

    /**
     * Defines whether this score is disabled in the UI.
     *
     * @param disabled the flag to apply
     */
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NudgeToolScore)) {
            return false;
        }
        NudgeToolScore that = (NudgeToolScore) o;
        return Objects.equals(scoreName, that.scoreName) && Objects.equals(scoreMinValue, that.scoreMinValue)
                && Objects.equals(mdiIcon, that.mdiIcon) && Objects.equals(title, that.title)
                && Objects.equals(description, that.description) && Objects.equals(disabled, that.disabled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scoreName, scoreMinValue, mdiIcon, title, description, disabled);
    }
}
