package org.open4goods.model;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class Localised implements Validable {


	@Field(index = false, store = false, type = FieldType.Text)
	private String text;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String language;

	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	public Localised(final String text, final String language) {
		this.text = text;
		this.language = language;
	}

	public Localised() {

	}

	@Override
	public String toString() {
		return language + ":" + text;
	}

	@Override
	public int hashCode() {
		return Objects.hash(language, text);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Localised) {
			final Localised o = (Localised) obj;
			return Objects.equals(getText(), o.getText()) && Objects.equals(getLanguage(), o.getLanguage());
		}

		return false;
	}

	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////

	@Override
	public void validate() throws ValidationException {
		if (StringUtils.isEmpty(text)) {
			throw new ValidationException("Localised text is empty");
		}
		if (StringUtils.isEmpty(language)) {
			throw new ValidationException("Localised language is empty");
		}
	}

	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

}
