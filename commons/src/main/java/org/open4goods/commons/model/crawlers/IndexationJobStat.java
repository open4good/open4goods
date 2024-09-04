
package org.open4goods.commons.model.crawlers;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "datas-indexations", createIndex = true)
public class IndexationJobStat {

	@Id
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String id; 
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String datasource="";
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String url="";

	@Field(index = true, store = false, type = FieldType.Date)
	private long startDate = 0;

	@Field(index = true, store = false, type = FieldType.Long)
	private long duration = 0;
	
	@Field(index = true, store = false, type = FieldType.Long)
	private long processed = 0;
	
	@Field(index = true, store = false, type = FieldType.Long)
	private long indexed = 0;
	
	@Field(index = true, store = false, type = FieldType.Long)
	private long validationFail = 0;
	
	@Field(index = true, store = false, type = FieldType.Long)
	private long exceptions = 0;

	@Field(index = true, store = false, type = FieldType.Boolean)
	private Boolean fail = false;
	
	public IndexationJobStat() {
	}	
	
	public IndexationJobStat(String url, String dsName) {
		this.startDate=System.currentTimeMillis();
		this.datasource=dsName;
		this.url = url;
		this.fail = false;
		this.id=UUID.randomUUID().toString();
	}

	
	public void terminate() {
		this.duration = System.currentTimeMillis() - startDate;		
	}

	public void incrementLines() {
		processed++;		
	}

	public void incrementErrors() {
		exceptions++;		
	}
	
	public void incrementValidationFail() {
		validationFail++;
	}

	public void incrementIndexed() {
		indexed++;
	}


	
	
	
	
	
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDatasource() {
		return datasource;
	}

	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	public long getProcessed() {
		return processed;
	}

	public void setProcessed(long lines) {
		this.processed = lines;
	}

	public long getIndexed() {
		return indexed;
	}

	public void setIndexed(long indexed) {
		this.indexed = indexed;
	}

	public long getValidationFail() {
		return validationFail;
	}

	public void setValidationFail(long validationFail) {
		this.validationFail = validationFail;
	}

	public long getExceptions() {
		return exceptions;
	}

	public void setExceptions(long exceptions) {
		this.exceptions = exceptions;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Boolean getFail() {
		return fail;
	}

	public void setFail(Boolean fail) {
		this.fail = fail;
	}






}
