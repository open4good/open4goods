package org.open4goods.commons.model.data;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

@Document(indexName = "user-searches", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class GlobalUserSearch {

	@Id
	private String id;
	
	
	@Field(type = FieldType.Date)
	private long ts;
	
	@Field(index = false, store = false, type = FieldType.Ip)
	private String ip;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String ua;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String search;


	public GlobalUserSearch( String ip, String ua, String search) {
		super();
		this.id=UUID.randomUUID().toString();
		this.ts = System.currentTimeMillis();
		this.ip = ip;
		this.ua = ua;
		this.search = search;
	}


	public GlobalUserSearch() {
		super();
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public long getTs() {
		return ts;
	}


	public void setTs(long ts) {
		this.ts = ts;
	}


	public String getIp() {
		return ip;
	}


	public void setIp(String ip) {
		this.ip = ip;
	}


	public String getUa() {
		return ua;
	}


	public void setUa(String ua) {
		this.ua = ua;
	}


	public String getSearch() {
		return search;
	}


	public void setSearch(String search) {
		this.search = search;
	}




}
