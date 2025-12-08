package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonMerge;

/**
 * Configuration for the nudge tool displayed on vertical pages.
 * It combines curated score thresholds and reusable subsets to simplify
 * user discovery.
 */
public class NudgeToolConfig {

    @JsonMerge
    private List<NudgeToolScore> scores = new ArrayList<>();

    @JsonMerge
    private List<VerticalSubset> subsets = new ArrayList<>();

    @JsonMerge
    private List<NudgeToolSubsetGroup> subsetGroups = new ArrayList<>();

    public List<NudgeToolScore> getScores() {
        return scores;
    }

    public void setScores(List<NudgeToolScore> scores) {
        this.scores = scores;
    }

    public List<VerticalSubset> getSubsets() {
        return subsets;
    }

    public void setSubsets(List<VerticalSubset> subsets) {
        this.subsets = subsets;
    }

    public List<NudgeToolSubsetGroup> getSubsetGroups() {
        return subsetGroups;
    }

    public void setSubsetGroups(List<NudgeToolSubsetGroup> subsetGroups) {
        this.subsetGroups = subsetGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NudgeToolConfig)) {
            return false;
        }
        NudgeToolConfig that = (NudgeToolConfig) o;
        return Objects.equals(scores, that.scores) && Objects.equals(subsets, that.subsets)
                && Objects.equals(subsetGroups, that.subsetGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scores, subsets, subsetGroups);
    }
}
