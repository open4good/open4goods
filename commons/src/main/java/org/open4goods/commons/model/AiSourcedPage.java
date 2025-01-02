package org.open4goods.commons.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * POJO Content for a page that presents tops, best, ..  
 */
@Document(indexName = "vertical-pages", createIndex = true, writeTypeHint = WriteTypeHint.FALSE)
public class AiSourcedPage {
	
	
	@Id
	private String id;
	
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String language;
	/***
	 * Title of the article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String title;
	
	/***
	 * question this article must answers
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String question;

	
	/***
	 * description of the article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String description;
	
	
	/** Conseils **/
	
	@Field(index = true, store = false, type = FieldType.Text)
	private String advices;
	
	@Field(store = false, type = FieldType.Object)
	private List<Long> products;

	/**
	 * The sources used to generate that content
	 */
	@Field(index = false, store = false, type = FieldType.Object)
	private List<AiSource> sources = new ArrayList<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getAdvices() {
		return advices;
	}

	public void setAdvices(String advices) {
		this.advices = advices;
	}

	public List<Long> getProducts() {
		return products;
	}

	public void setProducts(List<Long> products) {
		this.products = products;
	}

	public List<AiSource> getSources() {
		return sources;
	}

	public void setSources(List<AiSource> sources) {
		this.sources = sources;
	}

	
	
}
