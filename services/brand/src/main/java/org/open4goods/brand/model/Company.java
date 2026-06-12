package org.open4goods.brand.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The owning company of a brand, enriched with sourced intelligence: HQ and
 * manufacturing locations (category-aware), per-provider ESG/ethics scores, and
 * generic sourced "x-metas" (certifications, controversies, facts, news).
 *
 * <p>This is the v3 model. The legacy flat {@code factoryLocations} /
 * {@code scorings} maps were removed in favour of the structured fields below.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Company {

    private int schemaVersion = 3;
    private String id;
    private String name;
    private List<String> aliases = new ArrayList<>();
    private String parentCompanyId;
    private Map<String, String> identifiers = new HashMap<>();
    private CompanyLocation hq;
    private List<ManufacturingSite> manufacturing = new ArrayList<>();
    private Map<String, CompanyScore> scores = new HashMap<>();
    private List<XMeta> xmetas = new ArrayList<>();
    private Provenance provenance;

    public Company() {
    }

    public Company(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @param verticalId an open4goods vertical id (may be {@code null})
     * @return the manufacturing sites that apply to the given category
     */
    public List<ManufacturingSite> manufacturingFor(String verticalId) {
        return manufacturing.stream().filter(site -> site.appliesTo(verticalId)).toList();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
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

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases == null ? new ArrayList<>() : aliases;
    }

    public String getParentCompanyId() {
        return parentCompanyId;
    }

    public void setParentCompanyId(String parentCompanyId) {
        this.parentCompanyId = parentCompanyId;
    }

    public Map<String, String> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, String> identifiers) {
        this.identifiers = identifiers == null ? new HashMap<>() : identifiers;
    }

    public CompanyLocation getHq() {
        return hq;
    }

    public void setHq(CompanyLocation hq) {
        this.hq = hq;
    }

    public List<ManufacturingSite> getManufacturing() {
        return manufacturing;
    }

    public void setManufacturing(List<ManufacturingSite> manufacturing) {
        this.manufacturing = manufacturing == null ? new ArrayList<>() : manufacturing;
    }

    public Map<String, CompanyScore> getScores() {
        return scores;
    }

    public void setScores(Map<String, CompanyScore> scores) {
        this.scores = scores == null ? new HashMap<>() : scores;
    }

    public List<XMeta> getXmetas() {
        return xmetas;
    }

    public void setXmetas(List<XMeta> xmetas) {
        this.xmetas = xmetas == null ? new ArrayList<>() : xmetas;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
