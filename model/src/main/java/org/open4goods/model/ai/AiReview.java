package org.open4goods.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents an AI-generated review of a product, including descriptions, pros and cons, 
 * data quality assessment, and sourced information.
 */
public record AiReview(
		
	
    /**
     * A detailed description of the product.
     */
    @JsonProperty(required = true, value = "description") 
    @AiGeneratedField(instruction="Description du produit, 150 mots maximum")
    String description,
    
    /**
     * A brief summary of the product.
     */
    @JsonProperty(required = true, value = "short_description") 
    @AiGeneratedField(instruction="Description courte du produit, 50 mots maximum")
    String shortDescription,

    /**
     * A medium-length title summarizing the product.
     */
    @JsonProperty(required = true, value = "mediumTitle") 
    @AiGeneratedField(instruction="Titre de longueur moyenne, 10 mots maximum")
    String mediumTitle,

    /**
     * A short title for the product.
     */
    @JsonProperty(required = true, value = "shortTitle") 
    @AiGeneratedField(instruction="Titre court, 5 mots maximum")
    String shortTitle,

    /**
     * The technical review of the product.
     */
    @JsonProperty(required = true, value = "technicalReview")
    @AiGeneratedField(instruction="Revue technique approfondie du produit, uniquement basée sur le contenu des pages webs fournies.")
    String technicalReview,

    /**
     * The ecological review of the product.
     */
    @JsonProperty(required = true, value = "ecologicalReview") 
    @AiGeneratedField(instruction="Revue écologique du produit")
    String ecologicalReview,

    /**
     * A summary of the product review.
     */
    @JsonProperty(required = true, value = "summary") 
    @AiGeneratedField(instruction="Synthèse des évaluations et tests réalisés sur ce produit")
    String summary,

    /**
     * The pros of the product.
     */
    @AiGeneratedField(instruction="Les avantages du produit")
    @JsonProperty(required = true, value = "pros") List<String> pros,

    /**
     * The cons of the product.
     */
    @AiGeneratedField(instruction="Les inconvénients du produit")
    @JsonProperty(required = true, value = "cons") List<String> cons,

    /**
     * The sources providing the information for this review.
     */
    @JsonProperty(required = true, value = "sources") List<AiSource> sources,

    /**
     * The attributes related to the product.
     */
    @JsonProperty(required = true, value = "attributes") List<AiAttribute> attributes,

    /**
     * The quality of data used for the review.
     */
    @JsonProperty(required = true, value = "dataQuality") 
    @AiGeneratedField(instruction="Analyse de la qualité et de la richesse des contenus webs qui te sont fournis")
    String dataQuality
) {


    /**
     * Represents a source of information for an AI-generated review.
     */
    public record AiSource(
        /**
         * The reference number of the source.
         */
        @JsonProperty(required = true, value = "number") 
        @AiGeneratedField(instruction="Le numéro de la source documentaire fournie")
        Integer number,

        /**
         * The name of the source.
         */
        @AiGeneratedField(instruction="Le nom de la source documentaire fournie")
        @JsonProperty(required = true, value = "name") String name,

        /**
         * A description of the source.
         */
        @AiGeneratedField(instruction="Courte description de la source documentaire fournie")
        @JsonProperty(required = true, value = "description") String description,

        /**
         * The URL of the source.
         */
        @AiGeneratedField(instruction="URL de la source documentaire fournie")
        @JsonProperty(required = true, value = "url") String url
    ) {}

    /**
     * Represents an attribute of the product.
     */
    public record AiAttribute(
        /**
         * The name of the attribute.
         */
        @JsonProperty(required = true, value = "name") 
        @AiGeneratedField(instruction="Le nom de l'attribut")
        String name,

        /**
         * The value of the attribute.
         */
        @JsonProperty(required = true, value = "value") 
        @AiGeneratedField(instruction="La valeur de l'attribut")
        String value,
        
        /**
         * The source of the attribute.
         */
        @JsonProperty(required = true, value = "number") 
        @AiGeneratedField(instruction="La référence de la source qui indique cet attribut")
        Integer number
        
    ) {}
}
