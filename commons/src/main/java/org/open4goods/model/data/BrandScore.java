package org.open4goods.model.data;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.helper.IdHelper;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "brand-scores", createIndex = true)
public class BrandScore {

	@Id
	private String id;
	
	
	@Field(type = FieldType.Date)
	private long lastUpdate;

	@Field(index = true, store = false, type = FieldType.Keyword)
	private String datasourceName;
	
	@Field(index = true, store = false, type = FieldType.Text)
	// As text to allow prefix / wildcards search
	private String brandName;
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String scoreValue;
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private Set<String> tags = new HashSet<>();
	
	public BrandScore(String datasourceName, String brandName, String scoreValue) {
		super();
		this.datasourceName = datasourceName;
		this.brandName = brandName;
		this.id=id(datasourceName, brandName);
		this.lastUpdate = System.currentTimeMillis();
		this.scoreValue = scoreValue;
	}

	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return datasourceName+"-"+brandName+"="+scoreValue;
	}
	
	public BrandScore(String id) {
		super();
		this.id = id;
	}

	public BrandScore() {
		super();
	}
	
	public static String id(String datasourceName, String brandName) {
		return IdHelper.azCharAndDigits(datasourceName)+"-"+IdHelper.azCharAndDigits(brandName).toLowerCase();
	}
	
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	public String getBrandName() {
		return brandName;
	}

	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	public String getScoreValue() {
		return scoreValue;
	}

	public void setScoreValue(String scoreValue) {
		this.scoreValue = scoreValue;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}
	
	
	
	
	

}
