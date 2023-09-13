package org.open4goods.model.data;

import java.util.UUID;

import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "affiliation-links", createIndex = true)
public class AffiliationToken {

	@Id
	private String id;
	
	
	@Field(type = FieldType.Date)
	private long ts;
//	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String gtin;

	@Field(index = false, store = false, type=FieldType.Double)
	private Double price;
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String cashback;
	
	
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String datasourceName;
	
	@Field(index = false, store = false, type = FieldType.Ip)
	private String ip;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String ua;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;


	public AffiliationToken() {
		super();
	}



	public AffiliationToken(AggregatedPrice e, Product data) {
		gtin= data.gtin();
		price=e.getPrice();
		datasourceName = e.getDatasourceName();
		url=e.getUrl();
		ts = System.currentTimeMillis();
		this.id=UUID.randomUUID().toString();
	}

	
	
	public AffiliationToken(String datasourcename, String url) {

		datasourceName = datasourcename;
		this.url=url;
		ts = System.currentTimeMillis();
		this.id=UUID.randomUUID().toString();
	}
	
	
	public String getGtin() {
		return gtin;
	}
	public void setGtin(final String gtin) {
		this.gtin = gtin;
	}
	public Double getPrice() {
		return price;
	}
	public void setPrice(final Double price) {
		this.price = price;
	}

	
	public String getDatasourceName() {
		return datasourceName;
	}
	public void setDatasourceName(final String datasourceName) {
		this.datasourceName = datasourceName;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(final String url) {
		this.url = url;
	}


	public String getIp() {
		return ip;
	}

	public void setIp(final String ip) {
		this.ip = ip;
	}

	public String getUa() {
		return ua;
	}

	public void setUa(final String ua) {
		this.ua = ua;
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



	public String getCashback() {
		return cashback;
	}


	public void setCashback(String cashback) {
		this.cashback = cashback;
	}



}
