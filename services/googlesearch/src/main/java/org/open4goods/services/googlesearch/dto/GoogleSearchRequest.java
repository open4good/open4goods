package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing a Google Search Request.
 */
public class GoogleSearchRequest {
    
    private static final int DEFAULT_SEARCH_RESULTS = 10;
	private final String query;
    private final int numResults;

    // New optional search parameters corresponding to Google API options.
    private String lr;   // Language restriction (e.g., lang_en)
    private String cr;   // Country restriction (e.g., countryUS)
    private String safe = "off"; // Safe search parameter (e.g., active, off)
    private String sort; // Sort option
    private String gl;   // Geolocation (e.g., us)
    private String hl;   // Interface language (e.g., en)

  
    /**
     * Constructs a new GoogleSearchRequest with all options.
     *
     * @param query      the search query (must not be null or empty)
     * @param numResults the number of results to retrieve
     * @param lr         language restriction parameter (optional)
     * @param cr         country restriction parameter (optional)
     * @param safe       safe search option (optional)
     * @param sort       sort option (optional)
     * @param gl         geolocation parameter (optional)
     * @param hl         interface language parameter (optional)
     */
    public GoogleSearchRequest(String query, int numResults, String lr, String cr, String safe, String sort, String gl, String hl) {
        this.lr = lr;
        this.cr = cr;
        this.safe = safe;
        this.sort = sort;
        this.gl = gl;
        this.hl = hl;
		this.query = query;
		this.numResults = numResults;
    }

    public GoogleSearchRequest(String query, String lr, String cr) {
        this.lr = lr;
        this.cr = cr;
		this.query = query;
		this.numResults = DEFAULT_SEARCH_RESULTS;
    }
	public String getQuery() {
        return query;
    }

    public int getNumResults() {
        return numResults;
    }
    
    public String getLr() {
        return lr;
    }
    
    public void setLr(String lr) {
        this.lr = lr;
    }
    
    public String getCr() {
        return cr;
    }
    
    public void setCr(String cr) {
        this.cr = cr;
    }
    
    public String getSafe() {
        return safe;
    }
    
    public void setSafe(String safe) {
        this.safe = safe;
    }
    
    public String getSort() {
        return sort;
    }
    
    public void setSort(String sort) {
        this.sort = sort;
    }
    
    public String getGl() {
        return gl;
    }
    
    public void setGl(String gl) {
        this.gl = gl;
    }
    
    public String getHl() {
        return hl;
    }
    
    public void setHl(String hl) {
        this.hl = hl;
    }

    @Override
    public String toString() {
        return "GoogleSearchRequest{" +
                "query='" + query + '\'' +
                ", numResults=" + numResults +
                ", lr='" + lr + '\'' +
                ", cr='" + cr + '\'' +
                ", safe='" + safe + '\'' +
                ", sort='" + sort + '\'' +
                ", gl='" + gl + '\'' +
                ", hl='" + hl + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, numResults, lr, cr, safe, sort, gl, hl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchRequest)) return false;
        GoogleSearchRequest that = (GoogleSearchRequest) o;
        return numResults == that.numResults &&
               Objects.equals(query, that.query) &&
               Objects.equals(lr, that.lr) &&
               Objects.equals(cr, that.cr) &&
               Objects.equals(safe, that.safe) &&
               Objects.equals(sort, that.sort) &&
               Objects.equals(gl, that.gl) &&
               Objects.equals(hl, that.hl);
    }
}
