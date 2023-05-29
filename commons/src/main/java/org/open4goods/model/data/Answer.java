package org.open4goods.model.data;

import java.util.Objects;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.Validable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class Answer  implements Validable {

	@Field(index = false, store = false, type = FieldType.Object)
	private Localised answer = new Localised();

	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long date;

	@Field(index = false, store = false, type = FieldType.Integer)
	private Integer usefull;

	@Field(index = false, store = false, type = FieldType.Integer)
	private Integer useless;

	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////

	@Override
	public void validate() throws ValidationException {
		answer.validate();

	}

	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		return answer.toString();
	}

	@Override
	public int hashCode() {
		return answer.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Answer) {
			final Answer o = (Answer) obj;
			return Objects.equals(answer, o.getAnswer());
		}

		return false;
	}

	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////

	public String getAuthor() {
		return author;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(final Long date) {
		this.date = date;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	private String author;

	public Localised getAnswer() {
		return answer;
	}

	public void setAnswer(final Localised answer) {
		this.answer = answer;
	}

	public Integer getUsefull() {
		return usefull;
	}

	public void setUsefull(final Integer usefull) {
		this.usefull = usefull;
	}

	public Integer getUseless() {
		return useless;
	}

	public void setUseless(final Integer useless) {
		this.useless = useless;
	}

}
