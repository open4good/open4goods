package org.open4goods.model.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents an AI-generated review of a product, including descriptions, pros and cons,
 * data quality assessment, and sourced information.
 */
@Schema(description = "Represents an AI-generated review of a product, including descriptions, pros and cons, data quality assessment, and sourced information.")
// TODO : Could we convert to record ?
public class AiReview {



	    public AiReview(String description, String technicalOneline, String ecologicalOneline, String communityOneline,
			String shortDescription, String mediumTitle, String shortTitle, String manufacturingCountry,
			String technicalReview, String ecologicalReview, String communityReview, String summary, List<String> pros,
			List<String> cons, List<AiSource> sources, List<AiAttribute> attributes, String dataQuality,
			List<AiRating> ratings, List<String> pdfs, List<String> images, List<String> videos,
			List<String> socialLinks) {
		super();
		this.description = description;
		this.technicalOneline = technicalOneline;
		this.ecologicalOneline = ecologicalOneline;
		this.communityOneline = communityOneline;
		this.shortDescription = shortDescription;
		this.mediumTitle = mediumTitle;
		this.shortTitle = shortTitle;
		this.manufacturingCountry = manufacturingCountry;
		this.technicalReview = technicalReview;
		this.ecologicalReview = ecologicalReview;
		this.communityReview = communityReview;
		this.summary = summary;
		this.pros = pros;
		this.cons = cons;
		this.sources = sources;
		this.attributes = attributes;
		this.dataQuality = dataQuality;
		this.ratings = ratings;
		this.pdfs = pdfs;
		this.images = images;
		this.videos = videos;
		this.socialLinks = socialLinks;
	}

		/** A detailed description of the product. */
    @JsonProperty(required = true, value = "description")
    @AiGeneratedField(instruction = "Description du produit, 150 mots maximum")
    @Schema(description = "Detailed description of the product")
    private String description;


    /** A detailed description of the product. */
    @JsonProperty(required = true, value = "technicalOneline")
    @AiGeneratedField(instruction = "Description technique du produit, aux vues de ces caracteristiques principales. 20 mots maximum")
    @Schema(description = "Detailed description of the product")
    private String technicalOneline;


    /** A detailed description of the product. */
    @JsonProperty(required = true, value = "ecologicalOneline")
    @AiGeneratedField(instruction = "Description de l'impact écologique et du positionnement du téléviseur, au regard des informations environnementales et du positionnement de ce produit le classement obtenu grâce à l'impactscore, 20 mots maximum")
    @Schema(description = "Detailed description of the product")
    private String ecologicalOneline;



    /** A detailed description of the product. */
    @JsonProperty(required = true, value = "communityOneline")
    @AiGeneratedField(instruction = "Retours d'utilisateurs, synthèse d'avis. 20 mots maximum")
    @Schema(description = "Detailed description of the product")
    private String communityOneline;



    /** A brief summary of the product. */
    @JsonProperty(required = true, value = "short_description")
    @AiGeneratedField(instruction = "Deux ou trois phrases dur la description générale du produit, un bilan objectif de ses performances, de son impact environnemental et des retours utilisateurs et / ou sites spécialisés")
    @Schema(description = "Brief summary of the product")
    private String shortDescription;

    /** A medium-length title summarizing the product. */
    @JsonProperty(required = true, value = "mediumTitle")
    @AiGeneratedField(instruction = "Titre de longueur moyenne, 10 mots maximum")
    @Schema(description = "Medium-length title")
    private String mediumTitle;

    /** A short title for the product. */
    @JsonProperty(required = true, value = "shortTitle")
    @AiGeneratedField(instruction = "Titre court, 5 mots maximum")
    @Schema(description = "Short title")
    private String shortTitle;


    @JsonProperty(required = true, value = "manufacturingCountry")
    @AiGeneratedField(instruction = "Le ou les lieux de fabrication si connus, séparés par des virgules")
    @Schema(description = "Probable manufacturing countries of this product")
    private String manufacturingCountry;


    /** The technical review of the product. */
    @JsonProperty(required = true, value = "technicalReview")
    @AiGeneratedField(instruction = "Revue technique approfondie du produit, axée sur les performances et la qualité.")
    @Schema(description = "Technical review of the product")
    private String technicalReview;

    /** The ecological review of the product. */
    @JsonProperty(required = true, value = "ecologicalReview")
    @AiGeneratedField(instruction = "Revue écologique du produit, incluant réparabilité, durabilité, efficacité énergétique et toutes informations contenues dans les sources ou dans les scores fournis permettant d'orienter le jugement. ")
    @Schema(description = "Ecological review of the product")
    private String ecologicalReview;


    /** The ecological review of the product. */
    @JsonProperty(required = true, value = "communityReview")
    @AiGeneratedField(instruction = "Synthèse des retours de sites experts et utilisateurs")
    @Schema(description = "Community review of the product")
    private String communityReview;



    /** A summary of the product review. */
    // TODO : Remove
    @JsonProperty(required = true, value = "summary")
    @AiGeneratedField(instruction = "Synthèse des évaluations et tests réalisés sur ce produit")
    @Schema(description = "Summary of the product review")
    private String summary;

    /** The pros of the product. */
    @JsonProperty(required = true, value = "pros")
    @AiGeneratedField(instruction = "Les avantages du produit")
    @Schema(description = "List of pros")
    private List<String> pros = new ArrayList<>();

    /** The cons of the product. */
    @JsonProperty(required = true, value = "cons")
    @AiGeneratedField(instruction = "Les inconvénients du produit")
    @Schema(description = "List of cons")
    private List<String> cons = new ArrayList<>();

    /** The sources providing the information for this review. */
    @JsonProperty(required = true, value = "sources")
    @JsonDeserialize(using = AiSourcesDeserializer.class)
    @Schema(description = "List of sources used for the review")
    private List<AiSource> sources = new ArrayList<>();

    /** The attributes related to the product. */
    @JsonProperty(required = true, value = "attributes")
    @JsonDeserialize(using = AiAttributesDeserializer.class)
    @Schema(description = "List of product attributes")
    private List<AiAttribute> attributes = new ArrayList<>();

    /** The quality of data used for the review. */
    @JsonProperty(required = true, value = "dataQuality")
    @AiGeneratedField(instruction = "Analyse de la qualité et de la richesse des contenus, références, évaluations externes qui te sont fournis")
    @Schema(description = "Quality of external data used for the review")
    private String dataQuality;

    /** The ratings found in the sources. */
    @JsonProperty(required = false, value = "ratings")
    @Schema(description = "List of ratings found in sources")
    private List<AiRating> ratings = new ArrayList<>();



    /** The pdfs found in the sources. */
    @JsonProperty(required = false, value = "pdfs")
    @AiGeneratedField(instruction = "Liste des urls des documents PDFs trouvés (manuels, fiches techniques, réparabilité ...)")
    @Schema(description = "List of PDF's url's for this product")
    private List<String> pdfs = new ArrayList<>();



    /** The pdfs found in the sources. */
    @JsonProperty(required = false, value = "images")
    @AiGeneratedField(instruction = "Liste des urls des images produit trouvées (haute qualité de préférence)")
    @Schema(description = "List of quality images url's for this product")
    private List<String> images = new ArrayList<>();


    /** The pdfs found in the sources. */
    @JsonProperty(required = false, value = "videos")
    @AiGeneratedField(instruction = "Liste des urls des vidéos trouvées (youtube, dailymotion, vimeo ...)")
    @Schema(description = "List of product related videos (vide platform, like youtube, direct file, daylymotion, ... social networks, )")
    private List<String> videos = new ArrayList<>();

    /** The social network references */
    @JsonProperty(required = false, value = "social")
    @AiGeneratedField(instruction = "Liste des urls des posts réseaux sociaux trouvés (facebook, twitter, instagram ...)")
    @Schema(description = "List of social networks references")
    private List<String> socialLinks = new ArrayList<>();




    /** No-args constructor (required for deserialization) */
    public AiReview() {}

    // Getters and setters

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTechnicalOneline() {
        return technicalOneline;
    }

    public void setTechnicalOneline(String technicalOneline) {
        this.technicalOneline = technicalOneline;
    }

    public String getEcologicalOneline() {
        return ecologicalOneline;
    }

    public void setEcologicalOneline(String ecologicalOneline) {
        this.ecologicalOneline = ecologicalOneline;
    }

    public String getCommunityOneline() {
        return communityOneline;
    }

    public void setCommunityOneline(String communityOneline) {
        this.communityOneline = communityOneline;
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

    public String getManufacturingCountry() {
        return manufacturingCountry;
    }

    public void setManufacturingCountry(String manufacturingCountry) {
        this.manufacturingCountry = manufacturingCountry;
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

    public String getCommunityReview() {
        return communityReview;
    }

    public void setCommunityReview(String communityReview) {
        this.communityReview = communityReview;
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

    public List<String> getPdfs() {
		return pdfs;
	}

	public void setPdfs(List<String> pdfs) {
		this.pdfs = pdfs;
	}

	public List<String> getImages() {
		return images;
	}

	public void setImages(List<String> images) {
		this.images = images;
	}

	public List<String> getVideos() {
		return videos;
	}

	public void setVideos(List<String> videos) {
		this.videos = videos;
	}

	public List<String> getSocialLinks() {
		return socialLinks;
	}

	public void setSocialLinks(List<String> socialLinks) {
		this.socialLinks = socialLinks;
	}

	public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }

    public List<AiRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<AiRating> ratings) {
        this.ratings = ratings;
    }

    // --- Inner static classes converted from records ---

    /**
     * Represents a source of information for an AI-generated review.
     */
    @Schema(description = "Source information for the review")
    public static record AiSource(
            @JsonProperty(required = true, value = "number")
            @AiGeneratedField(instruction = "Le numéro de la source documentaire fournie")
            @Schema(description = "Source number")
            Integer number,
            @JsonProperty(required = true, value = "name")
            @AiGeneratedField(instruction = "Le nom de la source documentaire fournie")
            @Schema(description = "Source name")
            String name,
            @JsonProperty(required = true, value = "description")
            @AiGeneratedField(instruction = "Courte description de la source documentaire fournie")
            @Schema(description = "Source description")
            String description,
            @JsonProperty(required = true, value = "url")
            @AiGeneratedField(instruction = "URL de la source documentaire fournie")
            @Schema(description = "Source URL")
            String url) {

        public Integer getNumber() { return number; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getUrl() { return url; }
    }

    /**
     * Represents an attribute of the product.
     */
    @JsonDeserialize(using = AiAttributeDeserializer.class)
    @Schema(description = "Product attribute derived from review")
    public static record AiAttribute(
            @JsonProperty(required = true, value = "name")
            @AiGeneratedField(instruction = "Le nom de l'attribut")
            @Schema(description = "Attribute name")
            String name,
            @JsonProperty(required = true, value = "value")
            @AiGeneratedField(instruction = "La valeur de l'attribut")
            @Schema(description = "Attribute value")
            String value,
            @JsonProperty(required = true, value = "number")
            @AiGeneratedField(instruction = "La référence de la source qui indique cet attribut")
            @Schema(description = "Reference source number")
            Integer number) {

        public String getName() { return name; }
        public String getValue() { return value; }
        public Integer getNumber() { return number; }
    }

    /**
     * Represents a rating found in a source.
     */
    @Schema(description = "Rating found in a source")
    public static record AiRating(
            @JsonProperty(required = true, value = "source")
            @AiGeneratedField(instruction = "Le nom de la source qui donne la note")
            @Schema(description = "Source providing the rating")
            String source,
            @JsonProperty(required = true, value = "score")
            @AiGeneratedField(instruction = "La note donnée")
            @Schema(description = "The score value")
            String score,
            @JsonProperty(required = true, value = "max")
            @AiGeneratedField(instruction = "La note maximale possible (ex: 5, 10, 20)")
            @Schema(description = "The maximum possible score")
            String max,
             @JsonProperty(required = true, value = "comment")
            @AiGeneratedField(instruction = "Court commentaire associé à la note, si disponible")
            @Schema(description = "Short comment associated with the rating")
            String comment,
            @JsonProperty(required = true, value = "number")
            @AiGeneratedField(instruction = "Le numéro de la source documentaire correspondante")
            @Schema(description = "Reference source number")
            Integer number) {
    }

    public static class AiAttributeDeserializer extends JsonDeserializer<AiAttribute> {
        @Override
        public AiAttribute deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonNode node = p.getCodec().readTree(p);
            String name = node.has("name") ? node.get("name").asText() : null;
            String value = node.has("value") && !node.get("value").isNull() ? node.get("value").asText() : "";
            Integer number = node.has("number") && !node.get("number").isNull() ? node.get("number").asInt() : null;
            return new AiAttribute(name, value, number);
        }
    }

    public static class AiAttributesDeserializer extends JsonDeserializer<List<AiAttribute>> {
        @Override
        public List<AiAttribute> deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<AiAttribute> attributes = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode element : node) {
                    attributes.add(p.getCodec().treeToValue(element, AiAttribute.class));
                }
            } else if (node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    attributes.add(p.getCodec().treeToValue(fields.next().getValue(), AiAttribute.class));
                }
            }
            return attributes;
        }
    }

    public static class AiSourcesDeserializer extends JsonDeserializer<List<AiSource>> {
        @Override
        public List<AiSource> deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<AiSource> sources = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode element : node) {
                    sources.add(p.getCodec().treeToValue(element, AiSource.class));
                }
            } else if (node.isObject()) {
                sources.add(p.getCodec().treeToValue(node, AiSource.class));
            }
            return sources;
        }
    }
}
