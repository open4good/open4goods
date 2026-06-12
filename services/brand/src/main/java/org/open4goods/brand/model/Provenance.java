package org.open4goods.brand.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maintenance provenance stamp for a company referential entry. Replaces the
 * "# Maintained:" comment convention used in YAML corpora, since JSON has no
 * comments.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Provenance {

    private String maintainedBy;
    private String batch;
    private String updatedAt;

    public String getMaintainedBy() {
        return maintainedBy;
    }

    public void setMaintainedBy(String maintainedBy) {
        this.maintainedBy = maintainedBy;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
