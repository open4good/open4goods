package org.open4goods.model.data;

import java.util.UUID;

import org.open4goods.model.product.AggregatedData;
import org.open4goods.model.product.AggregatedPrice;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "affiliation-links", refreshInterval = "60s", createIndex = true)
public class AffiliationToken {

	@Id
	private String id;
	@Field(index = false, store = false, type = FieldType.Date)
	private long ts;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private Boolean affiliated;
	@Field(index = false, store = false, type=FieldType.Double)
	private Double price;
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String datasourceName;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String brand;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String brandUid;
	@Field(index = false, store = false, type = FieldType.Ip)
	private String ip;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String ua;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String gtin;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;
	
	public AffiliationToken() {
		super();
	}

	
	public AffiliationToken(String datasourceName , String url) {
		brand = "PARTNER";
		brandUid = "PARTNER";
		gtin= "";
		price=0.0;
		affiliated = true;
		this.datasourceName = datasourceName;
		this.url=url;
		ts = System.currentTimeMillis();
		id = UUID.randomUUID().toString();
	}

	
	
	public AffiliationToken(final DataFragment e, final AggregatedData data) {
		brand = data.brand();
		brandUid = data.model();
		gtin= data.gtin();
		price=e.getPrice().getPrice();
		affiliated = e.affiliated();
		datasourceName = e.getDatasourceConfigName();
		url=e.affiliatedUrlIfPossible();
		ts = System.currentTimeMillis();
		id = UUID.randomUUID().toString();

	}
	public AffiliationToken(AggregatedPrice e, AggregatedData data) {
		brand = data.brand();
		brandUid = data.model();
		gtin= data.gtin();
		price=e.getPrice();
		affiliated = e.isAffiliated();
		datasourceName = e.getDatasourceConfigName();
		url=e.getUrl();
		ts = System.currentTimeMillis();
	}

	public String getBrand() {
		return brand;
	}
	public void setBrand(final String brand) {
		this.brand = brand;
	}
	public String getBrandUid() {
		return brandUid;
	}
	public void setBrandUid(final String brandUid) {
		this.brandUid = brandUid;
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
	public Boolean getAffiliated() {
		return affiliated;
	}
	public void setAffiliated(final Boolean affiliated) {
		this.affiliated = affiliated;
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
	public long getTs() {
		return ts;
	}
	public void setTs(final long ts) {
		this.ts = ts;
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



}
