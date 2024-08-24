package org.open4goods.commons.config.yml.datasource;

import java.util.ArrayList;
import java.util.List;

public class CommentsProperties {
	private String title;
	private String description;
	private String date;
	private String author;
	private String seller;
	private String url;

	private String usefull;

	private String useless;

	/**
	 * Removals that will be used for answerUseless / useFull
	 */
	private List<String> useRemovals = new ArrayList<>();



	private RatingConfig rating;

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	public String getSeller() {
		return seller;
	}

	public void setSeller(final String seller) {
		this.seller = seller;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public RatingConfig getRating() {
		return rating;
	}

	public void setRating(final RatingConfig rating) {
		this.rating = rating;
	}

	public String getUsefull() {
		return usefull;
	}

	public void setUsefull(final String usefull) {
		this.usefull = usefull;
	}

	public String getUseless() {
		return useless;
	}

	public void setUseless(final String useless) {
		this.useless = useless;
	}

	public List<String> getUseRemovals() {
		return useRemovals;
	}

	public void setUseRemovals(final List<String> useRemovals) {
		this.useRemovals = useRemovals;
	}


}
