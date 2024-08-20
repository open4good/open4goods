package org.open4goods.model;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerticalIndexDefinition {

    private static final Logger logger = LoggerFactory.getLogger(VerticalIndexDefinition.class);

    private String indexName;
    private int numberOfShards;
    private int numberOfReplicas;
    private Map<String, VerticalIndexField> fields = new HashMap<>();

    public VerticalIndexDefinition(String indexName, int numberOfShards, int numberOfReplicas) {
        this.indexName = indexName;
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
    }

    public String getIndexName() {
        return indexName;
    }

    public int getNumberOfShards() {
        return numberOfShards;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public Map<String, VerticalIndexField> getFields() {
        return fields;
    }



}
