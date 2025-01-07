package org.open4goods.commons.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	@Field(index = true, store = false, type = FieldType.Keyword)
	/**
	 * The url pattern is :  /{vertical}/{url}
	 */
	private String url;

	@Field(index = true, store = false, type = FieldType.Keyword)
	/**
	 * ID of the vertical this SourcedPAge is related to
	 */
	private String vertical;

	
	/***
	 * meta description of the article
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String layout;
	
	
	
	/////////////////////////////////
	///// Text contents
	/////////////////////////////////
	/***
	 * Title (h1) of the article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String title;
	
	
	/***
	 * Hero description of this article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String hero;
	
	
	/**
	 * The bloc1 of text . Thinked initialy to be for example "Bien choisir une TV...., les points importants"
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String bloc1Title;
	@Field(index = true, store = false, type = FieldType.Text)
	private String bloc1Content;
	
	/**
	 * The bloc2 of text . Thinked initialy to be the description of our methodology. For example "Notre approche"
	 */

	@Field(index = true, store = false, type = FieldType.Text)
	private String bloc2Title;
	@Field(index = true, store = false, type = FieldType.Text)
	private String bloc2Content;
	
	
	//////////////////////////////////
	/// Metas
	//////////////////////////////////
	/***
	 * meta title of the article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String metaTitle;
	
	
	/***
	 * meta description of the article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String metaDescription;
	
	/***
	 * meta description of the article
	 */
	@Field(index = true, store = false, type = FieldType.Text)
	private String metaKeywords;
	
	
	
	/////////////////////////////////////////////
	///// Prompt config
	////////////////////////////////////////////
	@Field(index = true, store = false, type = FieldType.Text)
	/**
	 * The config, as YAML raw promptConfig object
	 */
	private String promptConfig;
	
	
	/**
	 * The context to the prompt, as a map of objects
	 */
	private Map<String,Object> context = new HashMap<String, Object>();
	

	
	////////////////////////////////////////////////
	// Matching products
	////////////////////////////////////////////////
	
	@Field(store = false, type = FieldType.Object)
	/**
	 * The matched products id
	 */
	private List<Long> products = new ArrayList<Long>();

	
	@Field(store = false, type = FieldType.Object)
	private Map<Long,String> productsDescriptions = new HashMap<Long, String>();

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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getVertical() {
		return vertical;
	}

	public void setVertical(String vertical) {
		this.vertical = vertical;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public String getHero() {
		return hero;
	}

	public void setHero(String hero) {
		this.hero = hero;
	}

	public String getBloc1Title() {
		return bloc1Title;
	}

	public void setBloc1Title(String bloc1Title) {
		this.bloc1Title = bloc1Title;
	}

	public String getBloc1Content() {
		return bloc1Content;
	}

	public void setBloc1Content(String bloc1Content) {
		this.bloc1Content = bloc1Content;
	}

	public String getBloc2Title() {
		return bloc2Title;
	}

	public void setBloc2Title(String bloc2Title) {
		this.bloc2Title = bloc2Title;
	}

	public String getBloc2Content() {
		return bloc2Content;
	}

	public void setBloc2Content(String bloc2Content) {
		this.bloc2Content = bloc2Content;
	}

	public String getMetaTitle() {
		return metaTitle;
	}

	public void setMetaTitle(String metaTitle) {
		this.metaTitle = metaTitle;
	}

	public String getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}

	public String getMetaKeywords() {
		return metaKeywords;
	}

	public void setMetaKeywords(String metaKeywords) {
		this.metaKeywords = metaKeywords;
	}

	public String getPromptConfig() {
		return promptConfig;
	}

	public void setPromptConfig(String promptConfig) {
		this.promptConfig = promptConfig;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	public Map<Long, String> getProductsDescriptions() {
		return productsDescriptions;
	}

	public void setProductsDescriptions(Map<Long, String> productsDescriptions) {
		this.productsDescriptions = productsDescriptions;
	}

	
	
}
