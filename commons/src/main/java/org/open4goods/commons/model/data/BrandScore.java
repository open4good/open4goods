package org.open4goods.commons.model.data;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Document(indexName = "brand-scores", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class BrandScore {

	private static final Logger logger = LoggerFactory.getLogger(BrandScore.class);
	
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
	
	@Field(index = true, store = false, type = FieldType.Double)
	private Double normalized;
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private Set<String> tags = new HashSet<>();
	
	public BrandScore(DataSourceProperties datasourceProperties, String brandName, String scoreValue) {
		super();
		this.datasourceName = datasourceProperties.getName();
		this.brandName = brandName.toLowerCase().trim();
		this.id=id(datasourceProperties.getName(), brandName);
		this.lastUpdate = System.currentTimeMillis();
		this.scoreValue = scoreValue;
		
		try {
			Double norm;
			if (null != datasourceProperties.getInvertScaleBase()) {
				norm = 	(datasourceProperties.getInvertScaleBase() - Double.valueOf(scoreValue));
			} else {
				norm = Double.valueOf(scoreValue);
			}
			logger.info("Normalized score for brand {} with score {} is {}",brandName,scoreValue,norm);
			this.normalized = norm;
			
		} catch (Exception e) {
			logger.error("Error with score normalization for brand {} with score {}",brandName,scoreValue);
		}
		
		
		
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


	public Double getNormalized() {
		return normalized;
	}


	public void setNormalized(Double normalized) {
		this.normalized = normalized;
	}
	
	
	
	
	

}
