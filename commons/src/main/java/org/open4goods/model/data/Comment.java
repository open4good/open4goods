package org.open4goods.model.data;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.Standardisable;
import org.open4goods.model.Validable;
import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.google.common.collect.Sets;


public class Comment  implements Validable, Standardisable {

	@Field(index = false, store = false, type = FieldType.Object)
	private Localised title;

	@Field(index = false, store = false, type = FieldType.Object)
	private Localised description;

	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long date;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String author;

	@Field(index = false, store = false, type = FieldType.Object)
	private Rating rating;

	@Field(index = false, store = false, type = FieldType.Integer)
	private Integer usefull;

	@Field(index = false, store = false, type = FieldType.Integer)
	private Integer useless;

	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////


	@Override
	public Set<Standardisable> standardisableChildren() {
		if (null != rating) {
			return Sets.newHashSet(rating);
		} else {
			return Sets.newHashSet();
		}
	}

	@Override
	public void standardize(final StandardiserService standardiser, final Currency c) {
		if (null != rating) {
			rating.standardize(standardiser,c);
		}

	}

	@Override
	public void validate() throws ValidationException {
		title.validate();

		if (null != description) {
			description.validate();
		}
		if (null != rating) {
			rating.validate();
		}
		if (null == date) {
			throw new ValidationException("Empty date");
		}

		if (StringUtils.isEmpty(author)) {
			throw new ValidationException("Empty author");
		}

	}

	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		return author + ">" + title;
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description, date, author, rating);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Comment o) {
			return Objects.equals(title, o.getTitle()) && Objects.equals(description, o.getDescription())
					&& Objects.equals(date, o.getDate()) && Objects.equals(author, o.getAuthor());
		}

		return false;
	}

	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////
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

	public Long getDate() {
		return date;
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

	public Rating getRating() {
		return rating;
	}

	public void setRating(final Rating rating) {
		this.rating = rating;
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
