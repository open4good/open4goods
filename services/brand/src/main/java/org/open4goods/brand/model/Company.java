package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the owning company of a brand, including its metadata and scorings.
 */
public class Company {

    private String id;
    private String name;
    private List<String> factoryLocations = new ArrayList<>();
    private Map<String, Object> scorings = new HashMap<>();

    public Company() {
    }

    public Company(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFactoryLocations() {
        return factoryLocations;
    }

    public void setFactoryLocations(List<String> factoryLocations) {
        this.factoryLocations = factoryLocations != null ? factoryLocations : new ArrayList<>();
    }

    public Map<String, Object> getScorings() {
        return scorings;
    }

    public void setScorings(Map<String, Object> scorings) {
        this.scorings = scorings != null ? scorings : new HashMap<>();
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
