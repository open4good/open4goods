package org.open4goods.commons.model.data;

import java.util.UUID;

import org.open4goods.commons.model.product.AggregatedPrice;
import org.open4goods.commons.model.product.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;


@Document(indexName = ContributionVote.INDEX_NAME , createIndex = true)
public class ContributionVote {
	public static final String INDEX_NAME = "contribution-votes";

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
	private String vote;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String datasourceName;
	
	@Field(index = false, store = false, type = FieldType.Ip)
	private String ip;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String ua;
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;


	public ContributionVote() {
		super();
	}



	public ContributionVote(AggregatedPrice e, Product data) {
		gtin= data.gtin();
		price=e.getPrice();
		datasourceName = e.getDatasourceName();
		url=e.getUrl();
		ts = System.currentTimeMillis();
		this.id=UUID.randomUUID().toString();
	}

	
	
	public ContributionVote(String datasourcename, String url) {

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




	public String getVote() {
		return vote;
	}



	public void setVote(String cashback) {
		this.vote = cashback;
	}



	public long getTs() {
		return ts;
	}



	public void setTs(long ts) {
		this.ts = ts;
	}

}
