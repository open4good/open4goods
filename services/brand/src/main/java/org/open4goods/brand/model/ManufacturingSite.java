package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A place where a company manufactures or assembles products. Manufacturing is
 * category-aware: a company can build TVs in one country and fridges in another,
 * so each site optionally scopes itself to a set of open4goods vertical ids.
 * An empty {@link #categories} list means the site applies to all categories.
 *
 * <p>Coordinates are resolved at enrichment time (Open Supply Hub coordinates or
 * the geocode service); the runtime never geocodes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManufacturingSite {

    private List<String> categories = new ArrayList<>();
    private String country;
    private String city;
    private Double lat;
    private Double lon;
    private SiteType type = SiteType.FACTORY;
    private String operator;
    private List<SourcedReference> sources = new ArrayList<>();

    /**
     * @param verticalId an open4goods vertical id
     * @return true when this site applies to the given category (or to all
     *         categories when no scope is set)
     */
    public boolean appliesTo(String verticalId) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        return verticalId != null && categories.contains(verticalId);
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories == null ? new ArrayList<>() : categories;
    }

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

    public SiteType getType() {
        return type;
    }

    public void setType(SiteType type) {
        this.type = type == null ? SiteType.FACTORY : type;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<SourcedReference> getSources() {
        return sources;
    }

    public void setSources(List<SourcedReference> sources) {
        this.sources = sources == null ? new ArrayList<>() : sources;
    }
}
