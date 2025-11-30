package org.open4goods.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an AI-generated review of a product, including descriptions, pros and cons,
 * data quality assessment, and sourced information.
 */
public class AiReview {

    public AiReview(String description, String shortDescription, String mediumTitle, String shortTitle, String technicalReview, String ecologicalReview, String summary, List<String> pros, List<String> cons, List<AiSource> sources, List<AiAttribute> attributes, String dataQuality) {
		super();
		this.description = description;
		this.shortDescription = shortDescription;
		this.mediumTitle = mediumTitle;
		this.shortTitle = shortTitle;
		this.technicalReview = technicalReview;
		this.ecologicalReview = ecologicalReview;
		this.summary = summary;
		this.pros = pros;
		this.cons = cons;
		this.sources = sources;
		this.attributes = attributes;
		this.dataQuality = dataQuality;
	}

	/** A detailed description of the product. */
    @JsonProperty(required = true, value = "description")
    @AiGeneratedField(instruction = "Description du produit, 150 mots maximum")
    private String description;

    /** A brief summary of the product. */
    @JsonProperty(required = true, value = "short_description")
    @AiGeneratedField(instruction = "Description courte du produit, 50 mots maximum")
    private String shortDescription;

    /** A medium-length title summarizing the product. */
    @JsonProperty(required = true, value = "mediumTitle")
    @AiGeneratedField(instruction = "Titre de longueur moyenne, 10 mots maximum")
    private String mediumTitle;

    /** A short title for the product. */
    @JsonProperty(required = true, value = "shortTitle")
    @AiGeneratedField(instruction = "Titre court, 5 mots maximum")
    private String shortTitle;

    /** The technical review of the product. */
    @JsonProperty(required = true, value = "technicalReview")
    @AiGeneratedField(instruction = "Revue technique approfondie du produit, uniquement basée sur le contenu des pages webs fournies.")
    private String technicalReview;

    /** The ecological review of the product. */
    @JsonProperty(required = true, value = "ecologicalReview")
    @AiGeneratedField(instruction = "Revue écologique du produit")
    private String ecologicalReview;

    /** A summary of the product review. */
    @JsonProperty(required = true, value = "summary")
    @AiGeneratedField(instruction = "Synthèse des évaluations et tests réalisés sur ce produit")
    private String summary;

    /** The pros of the product. */
    @JsonProperty(required = true, value = "pros")
    @AiGeneratedField(instruction = "Les avantages du produit")
    private List<String> pros = new ArrayList<>();

    /** The cons of the product. */
    @JsonProperty(required = true, value = "cons")
    @AiGeneratedField(instruction = "Les inconvénients du produit")
    private List<String> cons = new ArrayList<>();

    /** The sources providing the information for this review. */
    @JsonProperty(required = true, value = "sources")
    private List<AiSource> sources = new ArrayList<>();

    /** The attributes related to the product. */
    @JsonProperty(required = true, value = "attributes")
    private List<AiAttribute> attributes = new ArrayList<>();

    /** The quality of data used for the review. */
    @JsonProperty(required = true, value = "dataQuality")
    @AiGeneratedField(instruction = "Analyse de la qualité et de la richesse des contenus webs qui te sont fournis")
    private String dataQuality;

    /** No-args constructor (required for deserialization) */
    public AiReview() {}

    // Getters and setters

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getMediumTitle() {
        return mediumTitle;
    }

    public void setMediumTitle(String mediumTitle) {
        this.mediumTitle = mediumTitle;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    public String getTechnicalReview() {
        return technicalReview;
    }

    public void setTechnicalReview(String technicalReview) {
        this.technicalReview = technicalReview;
    }

    public String getEcologicalReview() {
        return ecologicalReview;
    }

    public void setEcologicalReview(String ecologicalReview) {
        this.ecologicalReview = ecologicalReview;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getPros() {
        return pros;
    }

    public void setPros(List<String> pros) {
        this.pros = pros;
    }

    public List<String> getCons() {
        return cons;
    }

    public void setCons(List<String> cons) {
        this.cons = cons;
    }

    public List<AiSource> getSources() {
        return sources;
    }

    public void setSources(List<AiSource> sources) {
        this.sources = sources;
    }

    public List<AiAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AiAttribute> attributes) {
        this.attributes = attributes;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    // --- Inner static classes converted from records ---

    /**
     * Represents a source of information for an AI-generated review.
     */
    public static record AiSource(
            @JsonProperty(required = true, value = "number")
            @AiGeneratedField(instruction = "Le numéro de la source documentaire fournie")
            Integer number,
            @JsonProperty(required = true, value = "name")
            @AiGeneratedField(instruction = "Le nom de la source documentaire fournie")
            String name,
            @JsonProperty(required = true, value = "description")
            @AiGeneratedField(instruction = "Courte description de la source documentaire fournie")
            String description,
            @JsonProperty(required = true, value = "url")
            @AiGeneratedField(instruction = "URL de la source documentaire fournie")
            String url) {

        public Integer getNumber() { return number; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
    }

    /**
     * Represents an attribute of the product.
     */
    public static record AiAttribute(
            @JsonProperty(value = "name")
            @AiGeneratedField(instruction = "Le nom de l'attribut")
            String name,
            @JsonProperty(value = "value")
            @AiGeneratedField(instruction = "La valeur de l'attribut")
            String value,
            @JsonProperty(value = "number")
            @AiGeneratedField(instruction = "La référence de la source qui indique cet attribut")
            Integer number) {

        public String getName() { return name; }
        public String getValue() { return value; }
        public Integer getNumber() { return number; }
    }
}
