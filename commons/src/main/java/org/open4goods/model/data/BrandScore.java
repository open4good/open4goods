package org.open4goods.model.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "brand-scores", createIndex = true)
// TODO : Store original score value
public class BrandScore {

	@Id
	private String name;
	@Field(type = FieldType.Date)
	private long lastUpdate;

	@Field(index = true, store = false, type = FieldType.Object)
	private Map<String, Double> scores = new HashMap<>();

	
	public BrandScore(String name) {
		this.name = name;
	}
	
	public BrandScore() {
		super();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}


	public Map<String, Double> getScores() {
		return scores;
	}

	public void setScores(Map<String, Double> scores) {
		this.scores = scores;
	}


}
