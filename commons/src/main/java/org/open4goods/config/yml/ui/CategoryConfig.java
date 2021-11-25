package org.open4goods.config.yml.ui;

import java.util.HashMap;
import java.util.Map;

public class CategoryConfig {

	private PageLink link = new PageLink();

	private String elasticQuery;

	private String presentationTemplateName;

	private Map<String, String> attributesSubAggregations = new HashMap<>();

	public PageLink getLink() {
		return link;
	}

	public void setLink(final PageLink link) {
		this.link = link;
	}

	public String getElasticQuery() {
		return elasticQuery;
	}

	public void setElasticQuery(final String elasticQuery) {
		this.elasticQuery = elasticQuery;
	}

	public String getPresentationTemplateName() {
		return presentationTemplateName;
	}

	public void setPresentationTemplateName(final String presentationTemplateName) {
		this.presentationTemplateName = presentationTemplateName;
	}

	public Map<String, String> getAttributesSubAggregations() {
		return attributesSubAggregations;
	}

	public void setAttributesSubAggregations(final Map<String, String> attributesSubAggregations) {
		this.attributesSubAggregations = attributesSubAggregations;
	}

}
