package org.open4goods.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents an AI-generated review of a product, including descriptions, pros and cons, 
 * data quality assessment, and sourced information.
 */
public record AiReview(
		
	/**
	 * Date the AIReview was created
	 */
	 @JsonProperty(required = true, value = "createdMs") Long createdMs,
	
    /**
     * A detailed description of the product.
     */
    @JsonProperty(required = true, value = "description") String description,

    /**
     * A brief summary of the product.
     */
    @JsonProperty(required = true, value = "short_description") String shortDescription,

    /**
     * A medium-length title summarizing the product.
     */
    @JsonProperty(required = true, value = "mediumTitle") String mediumTitle,

    /**
     * A short title for the product.
     */
    @JsonProperty(required = true, value = "shortTitle") String shortTitle,

    /**
     * The technical review of the product.
     */
    @JsonProperty(required = true, value = "technicalReview") String technicalReview,

    /**
     * The ecological review of the product.
     */
    @JsonProperty(required = true, value = "ecologicalReview") String ecologicalReview,

    /**
     * A summary of the product review.
     */
    @JsonProperty(required = true, value = "summary") String summary,

    /**
     * The pros of the product.
     */
    @JsonProperty(required = true, value = "pros") List<String> pros,

    /**
     * The cons of the product.
     */
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
    @JsonProperty(required = true, value = "dataQuality") String dataQuality
) {


    /**
     * Represents a source of information for an AI-generated review.
     */
    public record AiSource(
        /**
         * The reference number of the source.
         */
        @JsonProperty(required = true, value = "number") Integer number,

        /**
         * The name of the source.
         */
        @JsonProperty(required = true, value = "name") String name,

        /**
         * A description of the source.
         */
        @JsonProperty(required = true, value = "description") String description,

        /**
         * The URL of the source.
         */
        @JsonProperty(required = true, value = "url") String url
    ) {}

    /**
     * Represents an attribute of the product.
     */
    public record AiAttribute(
        /**
         * The name of the attribute.
         */
        @JsonProperty(required = true, value = "name") String name,

        /**
         * The value of the attribute.
         */
        @JsonProperty(required = true, value = "value") String value
    ) {}
}
