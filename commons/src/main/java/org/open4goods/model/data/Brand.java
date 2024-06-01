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

@Document(indexName = "brands", createIndex = true)
// TODO : Store original score value
public class Brand {

	@Id
	private String name;
	@Field(type = FieldType.Date)
	private long lastUpdate;

	@Field(index = true, store = false, type = FieldType.Keyword)
	private Set<String> aka = new HashSet<String>();

	@Field(index = true, store = false, type = FieldType.Object)
	private Map<String, Double> scores = new HashMap<>();

	public Brand(String name) {
		this.name = name;
	}
	
	public Brand() {
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

	public Set<String> getAka() {
		return aka;
	}

	public void setAka(Set<String> aka) {
		this.aka = aka;
	}

	public Map<String, Double> getScores() {
		return scores;
	}

	public void setScores(Map<String, Double> scores) {
		this.scores = scores;
	}

}
