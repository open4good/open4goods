
package org.open4goods.commons.model.crawlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "indexations", createIndex = true)
public class WebIndexationStats {

	@Id
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String id; 
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String name;

	@Field(index = true, store = false, type = FieldType.Date)
	private long startDate;

	@Field(index = true, store = false, type = FieldType.Long)
	private long queueLength = 0;

	@Field(index = true, store = false, type = FieldType.Long)
	private long numberOfProcessedDatas = 0;

	@Field(index = true, store = false, type = FieldType.Long)
	private long numberOfIndexedDatas = 0;

	@Field(index = true, store = false, type = FieldType.Boolean)
	private boolean finished = false;

	@Field(index = true, store = false, type = FieldType.Boolean)
	private boolean shuttingDown = false;
	
	private Map<String,Long> filesCounters = new HashMap();


	public void incrementProcessed(final int size) {
		numberOfProcessedDatas += size;
	}
	
	
	public void incrementProcessed(String url) {
		
		Long count = filesCounters.get(url);
		if (null == count) {
			filesCounters.put(url, 1L);
		} else {
			filesCounters.put(url, count+1);
		}
		numberOfProcessedDatas++;
	}

	public void decrementQueue() {
		queueLength--;
	}

	public WebIndexationStats() {
		super();
	}

	public WebIndexationStats(final String name, final long startDate) {
		super();
		this.id = UUID.randomUUID().toString();
		this.name = name;
		this.startDate = startDate;
	}


	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(final boolean finished) {
		this.finished = finished;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public void setShuttingDown(final boolean shuttingDown) {
		this.shuttingDown = shuttingDown;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(final long startDate) {
		this.startDate = startDate;
	}

	public long getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(final long queueLength) {
		this.queueLength = queueLength;
	}

	public long getNumberOfProcessedDatas() {
		return numberOfProcessedDatas;
	}

	public void setNumberOfProcessedDatas(final long numberOfProcessedPages) {
		numberOfProcessedDatas = numberOfProcessedPages;
	}


	public long getNumberOfIndexedDatas() {
		return numberOfIndexedDatas;
	}

	public void setNumberOfIndexedDatas(final long numberOfIndexedPages) {
		numberOfIndexedDatas = numberOfIndexedPages;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public Map<String, Long> getFilesCounters() {
		return filesCounters;
	}


	public void setFilesCounters(Map<String, Long> filesCounters) {
		this.filesCounters = filesCounters;
	}








}
