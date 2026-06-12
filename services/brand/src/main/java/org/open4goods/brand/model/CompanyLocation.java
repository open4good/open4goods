package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Geographic location (typically a company headquarters) with optional
 * coordinates resolved at enrichment time and its sourcing references.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyLocation {

    private String country;
    private String city;
    private Double lat;
    private Double lon;
    private List<SourcedReference> sources = new ArrayList<>();

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public List<SourcedReference> getSources() {
        return sources;
    }

    public void setSources(List<SourcedReference> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }
}
