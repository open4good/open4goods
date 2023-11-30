package org.open4goods.model.data.aggregated;

import java.util.Objects;

import org.open4goods.model.data.Comment;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.DataFragmentSource;
import org.open4goods.model.data.Rating;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedComment extends DataFragmentSource {

	@Field(index = false, store = false, type = FieldType.Object)
	private String title;

	@Field(index = false, store = false, type = FieldType.Object)
	private String description;

	@Field(index = false, store = false, type = FieldType.Object)
	private String language;

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



	public AggregatedComment (Comment sourceComment, DataFragment source ) {
		super(source);
		setAuthor(sourceComment.getAuthor());
		setDate(sourceComment.getDate());
		setRating(sourceComment.getRating());
		setUsefull(sourceComment.getUsefull());
		setUseless(sourceComment.getUseless());
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description, date, author, rating);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Comment o) {
			return Objects.equals(title, o.getTitle().getText()) && Objects.equals(description, o.getDescription().getText())
					&& Objects.equals(date, o.getDate()) && Objects.equals(author, o.getAuthor());
		}

		return false;
	}

	public AggregatedComment() {
		super();
	}

	public Long getDate() {
		return date;
	}


	public void setDate(Long date) {
		this.date = date;
	}


	public String getAuthor() {
		return author;
	}


	public void setAuthor(String author) {
		this.author = author;
	}


	public Rating getRating() {
		return rating;
	}


	public void setRating(Rating rating) {
		this.rating = rating;
	}


	public Integer getUsefull() {
		return usefull;
	}


	public void setUsefull(Integer usefull) {
		this.usefull = usefull;
	}


	public Integer getUseless() {
		return useless;
	}


	public void setUseless(Integer useless) {
		this.useless = useless;
	}


	public void setTitle(String title) {
		this.title = title;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public String getLanguage() {
		return language;
	}



	public void setLanguage(String language) {
		this.language = language;
	}



	public String getTitle() {
		return title;
	}



	public String getDescription() {
		return description;
	}





}
