package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration for resource completion downloads, analysis and clustering.
 */
public class ResourceCompletionConfig {

    private List<ResourceCompletionUrlTemplate> urlTemplates = new ArrayList<>();

    /**
     * Regenerate file names even when a resource already has one.
     */
    private boolean forceEraseFileName = false;

    /**
     * User-agent sent when downloading remote resources.
     */
    @NotBlank
    private String downloadUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) open4goods-resource-completion";

    /**
     * Connection timeout for remote resource downloads.
     */
    @Min(1)
    private int connectTimeoutMs = 1_000;

    /**
     * Socket timeout for remote resource downloads.
     */
    @Min(1)
    private int socketTimeoutMs = 1_000;

    /**
     * Similarity threshold used to group images with embeddings.
     */
    @DecimalMin("0.0")
    @Max(1)
    private double embeddingSimilarityThreshold = 0.80;

    /**
     * Perceptive hash size used by jImageHash.
     */
    @Min(1)
    private int perceptiveHashSize = 32;

    /**
     * Alpha value above which transparent pixels are treated as opaque.
     */
    @Min(0)
    @Max(255)
    private int perceptiveHashAlphaThreshold = 243;

    /**
     * Minimum raw score for a language to count as present in a PDF.
     */
    @DecimalMin("0.0")
    @Max(1)
    private double pdfLanguageMinConfidence = 0.5;

    /**
     * Maximum number of pages read for PDF language detection.
     */
    @Min(1)
    private int pdfLanguageMaxPages = 5;

    /**
     * Maximum number of extracted characters used for PDF language detection.
     */
    @Min(1)
    private int pdfLanguageMaxChars = 20_000;

    /**
     * Maximum number of first-page text lines inspected for PDF title detection.
     */
    @Min(1)
    private int pdfTitleMaxLines = 10;

    /**
     * Font-size tolerance used when grouping title candidate lines.
     */
    @DecimalMin("0.0")
    private float pdfTitleFontSizeTolerance = 0.8f;

    /**
     * Maximum file size in bytes for an image to be processed. Larger images will be skipped.
     */
    @Min(1)
    private long maxImageSizeToProcessBytes = 10 * 1024 * 1024L;

    /**
     * Maximum file size in bytes for a PDF to be processed. Larger PDFs will be skipped.
     */
    @Min(1)
    private long maxPdfSizeToProcessBytes = 50 * 1024 * 1024L;

    public List<ResourceCompletionUrlTemplate> getUrlTemplates() {
        return urlTemplates;
    }

    public void setUrlTemplates(List<ResourceCompletionUrlTemplate> urlTemplates) {
        this.urlTemplates = urlTemplates == null ? new ArrayList<>() : urlTemplates;
    }

    public boolean isForceEraseFileName() {
        return forceEraseFileName;
    }

    public void setForceEraseFileName(boolean forceEraseFileName) {
        this.forceEraseFileName = forceEraseFileName;
    }

    public String getDownloadUserAgent() {
        return downloadUserAgent;
    }

    public void setDownloadUserAgent(String downloadUserAgent) {
        this.downloadUserAgent = downloadUserAgent;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }

    public void setSocketTimeoutMs(int socketTimeoutMs) {
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public double getEmbeddingSimilarityThreshold() {
        return embeddingSimilarityThreshold;
    }

    public void setEmbeddingSimilarityThreshold(double embeddingSimilarityThreshold) {
        this.embeddingSimilarityThreshold = embeddingSimilarityThreshold;
    }

    public int getPerceptiveHashSize() {
        return perceptiveHashSize;
    }

    public void setPerceptiveHashSize(int perceptiveHashSize) {
        this.perceptiveHashSize = perceptiveHashSize;
    }

    public int getPerceptiveHashAlphaThreshold() {
        return perceptiveHashAlphaThreshold;
    }

    public void setPerceptiveHashAlphaThreshold(int perceptiveHashAlphaThreshold) {
        this.perceptiveHashAlphaThreshold = perceptiveHashAlphaThreshold;
    }

    public double getPdfLanguageMinConfidence() {
        return pdfLanguageMinConfidence;
    }

    public void setPdfLanguageMinConfidence(double pdfLanguageMinConfidence) {
        this.pdfLanguageMinConfidence = pdfLanguageMinConfidence;
    }

    public int getPdfLanguageMaxPages() {
        return pdfLanguageMaxPages;
    }

    public void setPdfLanguageMaxPages(int pdfLanguageMaxPages) {
        this.pdfLanguageMaxPages = pdfLanguageMaxPages;
    }

    public int getPdfLanguageMaxChars() {
        return pdfLanguageMaxChars;
    }

    public void setPdfLanguageMaxChars(int pdfLanguageMaxChars) {
        this.pdfLanguageMaxChars = pdfLanguageMaxChars;
    }

    public int getPdfTitleMaxLines() {
        return pdfTitleMaxLines;
    }

    public void setPdfTitleMaxLines(int pdfTitleMaxLines) {
        this.pdfTitleMaxLines = pdfTitleMaxLines;
    }

    public float getPdfTitleFontSizeTolerance() {
        return pdfTitleFontSizeTolerance;
    }

    public void setPdfTitleFontSizeTolerance(float pdfTitleFontSizeTolerance) {
        this.pdfTitleFontSizeTolerance = pdfTitleFontSizeTolerance;
    }

    public long getMaxImageSizeToProcessBytes() {
        return maxImageSizeToProcessBytes;
    }

    public void setMaxImageSizeToProcessBytes(long maxImageSizeToProcessBytes) {
        this.maxImageSizeToProcessBytes = maxImageSizeToProcessBytes;
    }

    public long getMaxPdfSizeToProcessBytes() {
        return maxPdfSizeToProcessBytes;
    }

    public void setMaxPdfSizeToProcessBytes(long maxPdfSizeToProcessBytes) {
        this.maxPdfSizeToProcessBytes = maxPdfSizeToProcessBytes;
    }
}
