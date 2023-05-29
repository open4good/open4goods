package org.open4goods.model.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.Validable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class Question  implements Validable {

	@Field(index = false, store = false, type = FieldType.Object)
	private Localised title;

	@Field(index = false, store = false, type = FieldType.Object)
	private Localised description;

	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long date;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String author;

	@Field(index = false, store = false, type = FieldType.Object)
	private List<Answer> answers = new ArrayList<>();

	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////

	@Override
	public void validate() throws ValidationException {
		title.validate();

		if (null != description) {
			description.validate();
		}

		for (final Answer answer : answers) {
			answer.validate();
		}

	}
	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		return title.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description,  author, answers);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Question) {
			final Question o = (Question) obj;
			return  Objects.equals(title, o.getTitle())
					&& Objects.equals(description, o.getDescription()) && Objects.equals(author, o.getAuthor())
					&& Objects.equals(answers, o.getAnswers());
		}

		return false;
	}

	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////

	public Long getDate() {
		return date;
	}

	public Localised getTitle() {
		return title;
	}

	public void setTitle(final Localised title) {
		this.title = title;
	}

	public Localised getDescription() {
		return description;
	}

	public void setDescription(final Localised description) {
		this.description = description;
	}

	public void setDate(final Long date) {
		this.date = date;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(final List<Answer> answers) {
		this.answers = answers;
	}
}
