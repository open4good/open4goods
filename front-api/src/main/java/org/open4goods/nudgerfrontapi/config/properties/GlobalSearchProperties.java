package org.open4goods.nudgerfrontapi.config.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Relevance rules of the global search ({@code SearchService.globalSearch}, page {@code /rechercher}).
 * <p>
 * They never apply to {@code SearchService.search} (category pages, {@code /api/products}). See the README section
 * "Global search relevance" for the rules and their limits (GOU-2029).
 */
@ConfigurationProperties(prefix = "front.global-search")
public class GlobalSearchProperties {

    /**
     * Words or expressions naming an accessory or a spare part (lower case, accents allowed). A product is an
     * accessory when one of them starts its offer name, within {@link #accessoryHeadWords} words. The plural with a
     * trailing {@code s} and the form without accents are matched too.
     */
    private List<String> accessoryTerms = new ArrayList<>();

    /** Number of leading words of the offer name searched for an accessory term. */
    private int accessoryHeadWords = 3;

    /** Factor applied to the score of an accessory in the category groups ({@code boosting} negative boost). */
    private double accessoryNegativeBoost = 0.1;

    /** Maximum number of accessories returned apart, in {@code accessoryResults}. */
    private int accessoryResultsLimit = 8;

    /**
     * Weight of {@code log1p(offersCount)} added to the text score, so that at comparable relevance the product
     * sold by more merchants comes first. {@code 0} disables it.
     */
    private double offersCountWeight = 1.0;

    /** Boost of the exact expression ({@code match_phrase}) added to the text score. {@code 0} disables it. */
    private double phraseBoost = 2.0;

    /** Slop of the exact expression bonus. */
    private int phraseSlop = 1;

    /**
     * Long tail over-read: candidates read from Elasticsearch per displayed product, before the image and title
     * filter done in Java.
     */
    private int longTailOverRead = 3;

    /** Maximum number of long tail candidates read for one request; deeper pages are not reachable. */
    private int longTailMaxCandidates = 1200;

    /** Minimum number of words of a readable offer name. */
    private int minTitleWords = 3;

    /** Minimum length of the alphabetic word (other than the brand) a readable offer name must hold. */
    private int minTitleWordLength = 3;

    public List<String> getAccessoryTerms() {
        return accessoryTerms;
    }

    public void setAccessoryTerms(List<String> accessoryTerms) {
        this.accessoryTerms = accessoryTerms == null ? new ArrayList<>() : accessoryTerms;
    }

    public int getAccessoryHeadWords() {
        return accessoryHeadWords;
    }

    public void setAccessoryHeadWords(int accessoryHeadWords) {
        this.accessoryHeadWords = accessoryHeadWords;
    }

    public double getAccessoryNegativeBoost() {
        return accessoryNegativeBoost;
    }

    public void setAccessoryNegativeBoost(double accessoryNegativeBoost) {
        this.accessoryNegativeBoost = accessoryNegativeBoost;
    }

    public int getAccessoryResultsLimit() {
        return accessoryResultsLimit;
    }

    public void setAccessoryResultsLimit(int accessoryResultsLimit) {
        this.accessoryResultsLimit = accessoryResultsLimit;
    }

    public double getOffersCountWeight() {
        return offersCountWeight;
    }

    public void setOffersCountWeight(double offersCountWeight) {
        this.offersCountWeight = offersCountWeight;
    }

    public double getPhraseBoost() {
        return phraseBoost;
    }

    public void setPhraseBoost(double phraseBoost) {
        this.phraseBoost = phraseBoost;
    }

    public int getPhraseSlop() {
        return phraseSlop;
    }

    public void setPhraseSlop(int phraseSlop) {
        this.phraseSlop = phraseSlop;
    }

    public int getLongTailOverRead() {
        return longTailOverRead;
    }

    public void setLongTailOverRead(int longTailOverRead) {
        this.longTailOverRead = longTailOverRead;
    }

    public int getLongTailMaxCandidates() {
        return longTailMaxCandidates;
    }

    public void setLongTailMaxCandidates(int longTailMaxCandidates) {
        this.longTailMaxCandidates = longTailMaxCandidates;
    }

    public int getMinTitleWords() {
        return minTitleWords;
    }

    public void setMinTitleWords(int minTitleWords) {
        this.minTitleWords = minTitleWords;
    }

    public int getMinTitleWordLength() {
        return minTitleWordLength;
    }

    public void setMinTitleWordLength(int minTitleWordLength) {
        this.minTitleWordLength = minTitleWordLength;
    }
}
