package org.open4goods.model.data;

import java.util.Objects;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.Validable;
import org.open4goods.model.constants.ProviderType;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
public class Description implements Validable {


	@Field(index = false, store = false, type = FieldType.Object)
	private Localised content = new Localised();
	/**
	 * Flagged at the UI level
	 * TODO : remove this, as it is not used anymore
	 */
	@Field(index = false, store = false, type = FieldType.Boolean)
	private Boolean truncated;

	/**
	 * The type of the provider giving this description
	 * TODO : remove this, as it is not used anymore
	 */
	@Field(index = false, store = false, type = FieldType.Keyword)
	private ProviderType providerType;

	public Description() {
	}

	public Description(final Localised content) {

		this.content = content;

	}

	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////

	public Description(final String description, final String language) {
		content = new Localised(description, language);
	}


	@Override
	public void validate() throws ValidationException {
		content.validate();

	}
	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		return content.toString();
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof Description o) {
			return Objects.equals(content, o.getContent());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(content);
	}

	public Localised getContent() {
		return content;
	}

	public void setContent(final Localised content) {
		this.content = content;
	}


	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////

	public Boolean getTruncated() {
		return truncated;
	}

	public void setTruncated(final Boolean truncated) {
		this.truncated = truncated;
	}

	public ProviderType getProviderType() {
		return providerType;
	}

	public void setProviderType(final ProviderType providerType) {
		this.providerType = providerType;
	}


}
