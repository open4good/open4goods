package org.open4goods.model.data;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class DataFragmentSource {

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String provider;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String configName;
	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long indexationDate;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;

	public DataFragmentSource(DataFragment source) {
		provider = source.getDatasourceName();
		configName = source.getDatasourceConfigName();
		indexationDate = source.getLastIndexationDate();
		url = source.affiliatedUrlIfPossible();
	}



	public DataFragmentSource() {
		super();
	}



	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ":" + configName + ":" + url;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getConfigName() {
		return configName;
	}

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public Long getIndexationDate() {
		return indexationDate;
	}

	public void setIndexationDate(Long indexationDate) {
		this.indexationDate = indexationDate;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
