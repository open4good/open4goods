package org.open4goods.model.data;

import java.util.Objects;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.Validable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class ProsOrCons  implements Validable {

	@Field(index = false, store = false, type = FieldType.Object)
	private Localised label = new Localised();

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String author;

	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////

	@Override
	public void validate() throws ValidationException {
		label.validate();

	}
	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		return label.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, author);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ProsOrCons o) {
			return Objects.equals(label, o.getLabel()) && Objects.equals(author, o.getAuthor());
		}

		return false;
	}

	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////
	public Localised getLabel() {
		return label;
	}

	public void setLabel(final Localised label) {
		this.label = label;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

}
