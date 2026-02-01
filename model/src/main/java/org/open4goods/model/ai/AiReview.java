package org.open4goods.model.ai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({
        "description",
        "technicalOneline",
        "technicalShortReview",
        "ecologicalOneline",
        "communityOneline",
        "short_description",
        "mediumTitle",
        "shortTitle",
        "baseLine",
        "manufacturingCountry",

        "technicalReviewNovice",
        "technicalReviewIntermediate",
        "technicalReviewAdvanced",

        "ecologicalReviewNovice",
        "ecologicalReviewIntermediate",
        "ecologicalReviewAdvanced",

        "communityReviewNovice",
        "communityReviewIntermediate",
        "communityReviewAdvanced",

        "obsolescenceWarning",

        "summary",
        "pros",
        "cons",
        "sources",
        "attributes",
        "dataQuality",
        "ratings",
        "pdfs",
        "images",
        "videos",
        "social"
})
public class AiReview {

    public AiReview(
            String description,
            String technicalOneline,
            String technicalShortReview,
            String ecologicalOneline,
            String communityOneline,
            String shortDescription,
            String mediumTitle,
            String shortTitle,
            String baseLine,
            String manufacturingCountry,

            String technicalReviewNovice,
            String technicalReviewIntermediate,
            String technicalReviewAdvanced,

            String ecologicalReviewNovice,
            String ecologicalReviewIntermediate,
            String ecologicalReviewAdvanced,

            String communityReviewNovice,
            String communityReviewIntermediate,
            String communityReviewAdvanced,


            String obsolescenceWarning,


            String summary,
            List<String> pros,
            List<String> cons,
            List<AiSource> sources,
            List<AiAttribute> attributes,
            String dataQuality,
            List<AiRating> ratings,
            List<String> pdfs,
            List<String> images,
            List<String> videos,
            List<String> socialLinks
    ) {
        super();
        this.description = description;
        this.technicalOneline = technicalOneline;
        this.technicalShortReview = technicalShortReview;
        this.ecologicalOneline = ecologicalOneline;
        this.communityOneline = communityOneline;
        this.shortDescription = shortDescription;
        this.mediumTitle = mediumTitle;
        this.shortTitle = shortTitle;
        this.baseLine = baseLine;
        this.manufacturingCountry = manufacturingCountry;

        this.technicalReviewNovice = technicalReviewNovice;
        this.technicalReviewIntermediate = technicalReviewIntermediate;
        this.technicalReviewAdvanced = technicalReviewAdvanced;

        this.ecologicalReviewNovice = ecologicalReviewNovice;
        this.ecologicalReviewIntermediate = ecologicalReviewIntermediate;
        this.ecologicalReviewAdvanced = ecologicalReviewAdvanced;

        this.communityReviewNovice = communityReviewNovice;
        this.communityReviewIntermediate = communityReviewIntermediate;
        this.communityReviewAdvanced = communityReviewAdvanced;

        this.obsolescenceWarning = obsolescenceWarning;

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
    @AiGeneratedField(instruction =
            "Write in FRENCH. Neutral product overview (max 150 words). Complete sentences only. "
          + "No marketing tone, No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add Wikipedia-style citations [n] immediately after each specific factual claim (spec, feature, certification, etc.). "
          + "If a claim is not supported by sources, omit it.")
    @Schema(description = "Detailed description of the product")
    private String description;

    /** One-line technical summary. */
    @JsonProperty(required = true, value = "technicalOneline")
    @AiGeneratedField(instruction =
            "Write in FRENCH. One sentence (max 20 words) summarising key technical specs/performance. Plain text only. "
          + "Never mention prices, dates, or product condition (new/used). "
          + "Citations [n] are OPTIONAL; if you include a concrete spec/performance claim, you MAY append compact citations [n].")
    @Schema(description = "One-line technical summary")
    private String technicalOneline;

    /** A short technical summary of the product. */
    @JsonProperty(required = true, value = "technicalShortReview")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Short technical summary in 2–3 sentences. Mention the most important specs and observed performance. "
          + "No lists, no Markdown. Minimal HTML allowed (<strong>, <em>, <br>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete claim (spec, benchmark, measured result, limitation). "
          + "If you cannot source it, remove it.")
    @Schema(description = "Short technical summary of the product")
    private String technicalShortReview;

    /** One-line ecological summary. */
    @JsonProperty(required = true, value = "ecologicalOneline")
    @AiGeneratedField(instruction =
            "Write in FRENCH. One sentence (max 20 words) describing environmental impact signals: repairability, durability, energy efficiency/class if available. Plain text only. "
          + "Never mention prices, dates, or product condition (new/used). "
          + "Citations [n] are OPTIONAL; if you include a concrete claim (e.g., energy class, repairability statement), you MAY append [n].")
    @Schema(description = "One-line ecological summary")
    private String ecologicalOneline;

    /** One-line community summary. */
    @JsonProperty(required = true, value = "communityOneline")
    @AiGeneratedField(instruction =
            "Write in FRENCH. One sentence (max 20 words) summarising user + expert feedback. Plain text only. "
          + "Never mention prices, dates, or product condition (new/used). "
          + "Citations [n] are OPTIONAL; if you include a concrete consensus/criticism, you MAY append [n].")
    @Schema(description = "One-line community summary")
    private String communityOneline;

    /** A brief summary of the product. */
    @JsonProperty(required = true, value = "short_description")
    @AiGeneratedField(instruction =
            "Write in FRENCH. 2–3 sentences covering (1) performance/use, (2) environmental signals, (3) feedback. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete claim.")
    @Schema(description = "Brief summary of the product")
    private String shortDescription;

    /** A medium-length title summarizing the product. */
    @JsonProperty(required = true, value = "mediumTitle")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Medium-length factual title, max 7 words. Neutral tone. "
          + "VALUE-ONLY FIELD: do NOT include citations [n]. "
          + "Only use wording supported by sources; avoid adding claims.")
    @Schema(description = "Medium-length title")
    private String mediumTitle;

    /** A short title for the product. */
    @JsonProperty(required = true, value = "shortTitle")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Short factual title (max 5 words). No quotes or ending punctuation. "
          + "VALUE-ONLY FIELD: do NOT include citations [n]. "
          + "Only use wording supported by sources; avoid adding claims.")
    @Schema(description = "Short title")
    private String shortTitle;

    /** Ultra-synthetic baseline shown under the title. */
    @JsonProperty(required = true, value = "baseLine")
    @AiGeneratedField(instruction =
            "Write in FRENCH. ONE ultra-synthetic sentence (max ~14 words) summarizing the most striking highlights and trade-offs across: "
          + "(1) technical performance, (2) environmental impact/repairability, (3) community/expert feedback. "
          + "Neutral tone, no marketing. No prices, no dates, no product condition (new/used). "
          + "NO SOURCING: no sourcing, no citations ")
    @Schema(description = "Ultra-synthetic baseline under the title")
    private String baseLine;

    @JsonProperty(required = true, value = "manufacturingCountry")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Comma-separated manufacturing countries if explicitly stated in sources (e.g., \"France, Allemagne\"). "
          + "Return an empty string \"\" when unknown or uncertain. "
          + "VALUE-ONLY FIELD: do NOT include citations [n].")
    @Schema(description = "Probable manufacturing countries of this product")
    private String manufacturingCountry;

    // -------------------------
    // TECHNICAL REVIEWS (3 levels)
    // -------------------------

    @JsonProperty(required = true, value = "technicalReviewNovice")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Technical review for NOVICE readers: 2-3 short paragraphs, simple vocabulary. Explain jargon briefly. "
          + "Focus on real-world use: performance, key features, reliability/repairability when stated in sources. "
          + "no Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete claim (specs, tested results, limitations). Omit anything unsourced.")
    @Schema(description = "Technical review (novice)")
    private String technicalReviewNovice;

    @JsonProperty(required = true, value = "technicalReviewIntermediate")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Technical review for INTERMEDIATE readers: 3-4 paragraphs. Standard technical vocabulary allowed (avoid heavy jargon). "
          + "Include strengths, limits, and any repairability/reliability signals from sources. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete claim; do not infer beyond sources.")
    @Schema(description = "Technical review (intermediate)")
    private String technicalReviewIntermediate;

    @JsonProperty(required = true, value = "technicalReviewAdvanced")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Technical review for ADVANCED readers: 6-8 advanced paragraphs, nuanced. Discuss trade-offs and constraints only if sources provide evidence. "
          + "Benchmarks/tests must come from sources; do not invent numbers. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each technical claim; if uncertain, omit.")
    @Schema(description = "Technical review (advanced)")
    private String technicalReviewAdvanced;

    // -------------------------
    // ECOLOGICAL REVIEWS (3 levels)
    // -------------------------

    @JsonProperty(required = true, value = "ecologicalReviewNovice")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Environmental review for NOVICE readers: 2-3 short paragraphs, simple wording. "
          + "Cover durability signals, repairability, energy use/efficiency, materials/recycling ONLY if sourced. "
          + "If ecoscore/subscores are provided, use them ONLY for broad positioning (e.g., bien noté / contrasté / mal noté). Never mention rank/position numbers. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete claim; omit unsourced statements.")
    @Schema(description = "Ecological review (novice)")
    private String ecologicalReviewNovice;

    @JsonProperty(required = true, value = "ecologicalReviewIntermediate")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Environmental review for INTERMEDIATE readers: 3-4 paragraphs. "
          + "Discuss repairability, durability, energy efficiency, and sustainability evidence from sources. "
          + "Use ecoscore/subscores only for macro positioning; never mention ranks/precise positions. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each claim; do not infer beyond sources.")
    @Schema(description = "Ecological review (intermediate)")
    private String ecologicalReviewIntermediate;

    @JsonProperty(required = true, value = "ecologicalReviewAdvanced")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Environmental review for ADVANCED readers: 6-8 advanced paragraphs, include nuance and uncertainty. "
          + "Explicitly distinguish sourced facts vs cautious interpretation; never invent lifecycle data. "
          + "Use ecoscore/subscores only for broad interpretation; never mention ranks/precise positions. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each statement; omit unsupported claims.")
    @Schema(description = "Ecological review (advanced)")
    private String ecologicalReviewAdvanced;

    // -------------------------
    // COMMUNITY REVIEWS (3 levels)
    // -------------------------

    @JsonProperty(required = true, value = "communityReviewNovice")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Community review for NOVICE readers: 2-3 short paragraph summarising the main consensus points from users and experts. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete praise/criticism; avoid vague generalities.")
    @Schema(description = "Community review (novice)")
    private String communityReviewNovice;

    @JsonProperty(required = true, value = "communityReviewIntermediate")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Community review for INTERMEDIATE readers: 3-4 paragraphs, include recurring themes and notable disagreements if sources show them. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: cite [n] after each concrete statement (e.g., reliability complaints, UX praise).")
    @Schema(description = "Community review (intermediate)")
    private String communityReviewIntermediate;

    @JsonProperty(required = true, value = "communityReviewAdvanced")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Community review for ADVANCED readers: 6-8 advanced paragraphs. "
          + "You may discuss evidence strength (reviewer bias, sample size) ONLY if sources provide it. Do not speculate. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: cite [n] after each statement; omit any uncertain claim.")
    @Schema(description = "Community review (advanced)")
    private String communityReviewAdvanced;




    @JsonProperty(required = true, value = "obsolescenceWarning")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Obsolescence warning, checking out possible short / medium term obscolescence issue against last known standards. Check for compatibility, standards support, .... "
          + "You MUST return nothing (empty string), if no detected concern"
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: cite [n] after each statement; omit any uncertain claim.")
    @Schema(description = "Community review (advanced)")
    private String obsolescenceWarning;









    /** A summary of the product review. */
    @JsonProperty(required = true, value = "summary")
    @AiGeneratedField(instruction =
            "Write in FRENCH. Concise overall summary in 2–3 sentences. Include key takeaways and trade-offs. "
          + "No Markdown. Minimal HTML allowed (<strong>, <em>, <br>, <p>, <ul>, <li>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: add [n] after each concrete claim.")
    @Schema(description = "Summary of the product review")
    private String summary;

    /** The pros of the product. */
    @JsonProperty(required = true, value = "pros")
    @AiGeneratedField(instruction =
            "Write in FRENCH. maximum 6 short pros. Each entry is a concise phrase. "
          + "No numbering, no Markdown. Minimal HTML allowed (<strong>, <em>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "SOURCING RULE: only add citations [n] when an entry contains a concrete claim (spec, measured result, explicit praise from a source). "
          + "If the pro is a generic phrasing without a concrete claim, omit citations.")
    @Schema(description = "List of pros")
    private List<String> pros = new ArrayList<>();

    /** The cons of the product. */
    @JsonProperty(required = true, value = "cons")
    @AiGeneratedField(instruction =
            "Write in FRENCH. maximum 6 short cons. Each entry is a concise phrase. "
          + "No numbering, no Markdown. Minimal HTML allowed (<strong>, <em>). "
          + "Never mention prices, dates, or product condition (new/used). "
          + "SOURCING RULE: only add citations [n] when an entry contains a concrete claim (explicit criticism, limitation, defect, measurable downside). "
          + "If the con is generic without a concrete claim, omit citations.")
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
    @AiGeneratedField(instruction =
            "Write in FRENCH. 1–2 sentences describing data coverage, reliability, and key gaps (what you could not confirm). Plain text only. "
          + "Never mention prices, dates, or product condition (new/used). "
          + "MAXIMUM SOURCING: if you mention a specific gap linked to sources (e.g., 'no official datasheet found'), add [n]. "
          + "Otherwise keep it generic without citations.")
    @Schema(description = "Quality of external data used for the review")
    private String dataQuality;

    /** The ratings found in the sources. */
    @JsonProperty(required = false, value = "ratings")
    @Schema(description = "List of ratings found in sources")
    private List<AiRating> ratings = new ArrayList<>();

    /** The pdfs found in the sources. */
    @JsonProperty(required = false, value = "pdfs")
    @AiGeneratedField(instruction =
            "List of PDF URLs only (manuals, datasheets, repairability docs). VALUE-ONLY FIELD: no extra text, no citations.")
    @Schema(description = "List of PDF's url's for this product")
    private List<String> pdfs = new ArrayList<>();

    /** The images found in the sources. */
    @JsonProperty(required = false, value = "images")
    @AiGeneratedField(instruction =
            "List of product image URLs only (prefer high quality). VALUE-ONLY FIELD: no extra text, no citations.")
    @Schema(description = "List of quality images url's for this product")
    private List<String> images = new ArrayList<>();

    /** The videos found in the sources. */
    @JsonProperty(required = false, value = "videos")
    @AiGeneratedField(instruction =
            "List of video URLs only (YouTube, Vimeo, Dailymotion, direct files). VALUE-ONLY FIELD: no extra text, no citations.")
    @Schema(description = "List of product related videos")
    private List<String> videos = new ArrayList<>();

    /** The social network references */
    @JsonProperty(required = false, value = "social")
    @AiGeneratedField(instruction =
            "List of social post URLs only (Facebook, X/Twitter, Instagram, etc.). VALUE-ONLY FIELD: no extra text, no citations.")
    @Schema(description = "List of social networks references")
    private List<String> socialLinks = new ArrayList<>();

    /** No-args constructor (required for deserialization) */
    public AiReview() {}

    // -------------------------
    // Getters and setters
    // -------------------------

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechnicalOneline() { return technicalOneline; }
    public void setTechnicalOneline(String technicalOneline) { this.technicalOneline = technicalOneline; }

    public String getTechnicalShortReview() { return technicalShortReview; }
    public void setTechnicalShortReview(String technicalShortReview) { this.technicalShortReview = technicalShortReview; }

    public String getEcologicalOneline() { return ecologicalOneline; }
    public void setEcologicalOneline(String ecologicalOneline) { this.ecologicalOneline = ecologicalOneline; }

    public String getCommunityOneline() { return communityOneline; }
    public void setCommunityOneline(String communityOneline) { this.communityOneline = communityOneline; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getMediumTitle() { return mediumTitle; }
    public void setMediumTitle(String mediumTitle) { this.mediumTitle = mediumTitle; }

    public String getShortTitle() { return shortTitle; }
    public void setShortTitle(String shortTitle) { this.shortTitle = shortTitle; }

    public String getBaseLine() { return baseLine; }
    public void setBaseLine(String baseLine) { this.baseLine = baseLine; }

    public String getManufacturingCountry() { return manufacturingCountry; }
    public void setManufacturingCountry(String manufacturingCountry) { this.manufacturingCountry = manufacturingCountry; }

    public String getTechnicalReviewNovice() { return technicalReviewNovice; }
    public void setTechnicalReviewNovice(String technicalReviewNovice) { this.technicalReviewNovice = technicalReviewNovice; }

    public String getTechnicalReviewIntermediate() { return technicalReviewIntermediate; }
    public void setTechnicalReviewIntermediate(String technicalReviewIntermediate) { this.technicalReviewIntermediate = technicalReviewIntermediate; }

    public String getTechnicalReviewAdvanced() { return technicalReviewAdvanced; }
    public void setTechnicalReviewAdvanced(String technicalReviewAdvanced) { this.technicalReviewAdvanced = technicalReviewAdvanced; }

    public String getEcologicalReviewNovice() { return ecologicalReviewNovice; }
    public void setEcologicalReviewNovice(String ecologicalReviewNovice) { this.ecologicalReviewNovice = ecologicalReviewNovice; }

    public String getEcologicalReviewIntermediate() { return ecologicalReviewIntermediate; }
    public void setEcologicalReviewIntermediate(String ecologicalReviewIntermediate) { this.ecologicalReviewIntermediate = ecologicalReviewIntermediate; }

    public String getEcologicalReviewAdvanced() { return ecologicalReviewAdvanced; }
    public void setEcologicalReviewAdvanced(String ecologicalReviewAdvanced) { this.ecologicalReviewAdvanced = ecologicalReviewAdvanced; }

    public String getCommunityReviewNovice() { return communityReviewNovice; }
    public void setCommunityReviewNovice(String communityReviewNovice) { this.communityReviewNovice = communityReviewNovice; }

    public String getCommunityReviewIntermediate() { return communityReviewIntermediate; }
    public void setCommunityReviewIntermediate(String communityReviewIntermediate) { this.communityReviewIntermediate = communityReviewIntermediate; }

    public String getCommunityReviewAdvanced() { return communityReviewAdvanced; }
    public void setCommunityReviewAdvanced(String communityReviewAdvanced) { this.communityReviewAdvanced = communityReviewAdvanced; }

    public String getObsolescenceWarning() { return obsolescenceWarning; }
    public void setObsolescenceWarning(String obsolescenceWarning) { this.obsolescenceWarning = obsolescenceWarning; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getPros() { return pros; }
    public void setPros(List<String> pros) { this.pros = pros; }

    public List<String> getCons() { return cons; }
    public void setCons(List<String> cons) { this.cons = cons; }

    public List<AiSource> getSources() { return sources; }
    public void setSources(List<AiSource> sources) { this.sources = sources; }

    public List<AiAttribute> getAttributes() { return attributes; }
    public void setAttributes(List<AiAttribute> attributes) { this.attributes = attributes; }

    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }

    public List<AiRating> getRatings() { return ratings; }
    public void setRatings(List<AiRating> ratings) { this.ratings = ratings; }

    public List<String> getPdfs() { return pdfs; }
    public void setPdfs(List<String> pdfs) { this.pdfs = pdfs; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public List<String> getVideos() { return videos; }
    public void setVideos(List<String> videos) { this.videos = videos; }

    public List<String> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(List<String> socialLinks) { this.socialLinks = socialLinks; }

    // --- Inner static POJO classes (converted from records for better JSON Schema generation) ---

    /**
     * Represents a source of information for an AI-generated review.
     */
    @Schema(description = "Source information for the review", type = "object")
    public static class AiSource {
        
        @JsonProperty(required = true, value = "number")
        @AiGeneratedField(instruction = "Source number (must match provided sources). Plain text only. VALUE-ONLY FIELD: no citations.")
        @Schema(description = "Source number", type = "integer")
        private Integer number;
        
        @JsonProperty(required = true, value = "name")
        @AiGeneratedField(instruction = "Human-readable source name/title. Plain text only. VALUE-ONLY FIELD: no citations.")
        @Schema(description = "Source name", type = "string")
        private String name;
        
        @JsonProperty(required = true, value = "description")
        @AiGeneratedField(instruction = "Very short description of what the source provides (e.g., official specs, review, user forum). Plain text only. VALUE-ONLY FIELD: no citations.")
        @Schema(description = "Source description", type = "string")
        private String description;
        
        @JsonProperty(required = true, value = "url")
        @AiGeneratedField(instruction = "Source URL exactly. VALUE-ONLY FIELD: no extra text, no citations.")
        @Schema(description = "Source URL", type = "string")
        private String url;

        public AiSource() {}

        public AiSource(Integer number, String name, String description, String url) {
            this.number = number;
            this.name = name;
            this.description = description;
            this.url = url;
        }

        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    /**
     * Represents an attribute of the product.
     */
    @JsonDeserialize(using = AiAttributeDeserializer.class)
    @Schema(description = "Product attribute derived from review", type = "object")
    public static class AiAttribute {
        
        @JsonProperty(required = true, value = "name")
        @AiGeneratedField(instruction = "Attribute name. Plain text only.")
        @Schema(description = "Attribute name", type = "string")
        private String name;
        
        @JsonProperty(required = true, value = "value")
        @AiGeneratedField(instruction = "Attribute value. Plain text only (no HTML).")
        @Schema(description = "Attribute value", type = "string")
        private String value;
        
        @JsonProperty(required = true, value = "number")
        @AiGeneratedField(instruction = "Source number that supports this attribute.")
        @Schema(description = "Reference source number", type = "integer")
        private Integer number;

        public AiAttribute() {}

        public AiAttribute(String name, String value, Integer number) {
            this.name = name;
            this.value = value;
            this.number = number;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
    }

    /**
     * Represents a rating found in a source.
     */
    @Schema(description = "Rating found in a source", type = "object")
    public static class AiRating {
        
        @JsonProperty(required = true, value = "source")
        @AiGeneratedField(instruction = "Name of the source that provides the rating. VALUE-ONLY FIELD: no citations.")
        @Schema(description = "Source providing the rating", type = "string")
        private String source;
        
        @JsonProperty(required = true, value = "score")
        @AiGeneratedField(instruction = "Score value exactly as stated by the source (e.g., \"4.5\"). VALUE-ONLY FIELD: no citations.")
        @Schema(description = "The score value", type = "string")
        private String score;
        
        @JsonProperty(required = true, value = "max")
        @AiGeneratedField(instruction = "Maximum possible score exactly as stated by the source (e.g., \"5\", \"10\", \"20\"). VALUE-ONLY FIELD: no citations.")
        @Schema(description = "The maximum possible score", type = "string")
        private String max;
        
        @JsonProperty(required = false, value = "comment")
        @AiGeneratedField(instruction = "Short associated comment if present in the source. Write in FRENCH if you translate; otherwise keep original wording. Plain text only. VALUE-ONLY FIELD: no citations.")
        @Schema(description = "Short comment associated with the rating", type = "string")
        private String comment;
        
        @JsonProperty(required = true, value = "number")
        @AiGeneratedField(instruction = "Source number that supports this rating. VALUE-ONLY FIELD.")
        @Schema(description = "Reference source number", type = "integer")
        private Integer number;

        public AiRating() {}

        public AiRating(String source, String score, String max, String comment, Integer number) {
            this.source = source;
            this.score = score;
            this.max = max;
            this.comment = comment;
            this.number = number;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getScore() { return score; }
        public void setScore(String score) { this.score = score; }
        public String getMax() { return max; }
        public void setMax(String max) { this.max = max; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Integer getNumber() { return number; }
        public void setNumber(Integer number) { this.number = number; }
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
