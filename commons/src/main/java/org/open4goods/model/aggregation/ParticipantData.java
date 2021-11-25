package org.open4goods.model.aggregation;

import org.open4goods.model.constants.ProviderType;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Designates a DataFragment that is participant in an AggregatedData
 * 
 * @author Goulven.Furet
 *
 */
public class ParticipantData {

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String providerName;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private ProviderType providerType;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String dataUrl;


	@Override
	public int hashCode() {		
		return dataUrl.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ParticipantData) {
			final ParticipantData o = (ParticipantData) obj;
			return o.getDataUrl().equals(this.getDataUrl());
		}

		return false;
	}
	
	
	@Override
	public String toString() {
		return providerName + ":" + dataUrl;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(final String providerName) {
		this.providerName = providerName;
	}

	public ProviderType getProviderType() {
		return providerType;
	}

	public void setProviderType(final ProviderType providerType) {
		this.providerType = providerType;
	}

	public String getDataUrl() {
		return dataUrl;
	}

	public void setDataUrl(final String dataUrl) {
		this.dataUrl = dataUrl;
	}


}
