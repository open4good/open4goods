package org.open4goods.commons.config.yml.ui;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.commons.model.Localisable;

public class VerticalSubset {
	
	private String id;
	
	private String group;
	
	private List<SubsetCriteria> criterias = new ArrayList<>();
	
	private String image;
	
	private Localisable<String, String> url;
	
	private Localisable<String, String> title;
	
	private Localisable<String, String> description;

	public List<SubsetCriteria> getCriterias() {
		return criterias;
	}

	public void setCriterias(List<SubsetCriteria> criterias) {
		this.criterias = criterias;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public Localisable<String, String> getTitle() {
		return title;
	}

	public void setTitle(Localisable<String, String> title) {
		this.title = title;
	}

	public Localisable<String, String> getDescription() {
		return description;
	}

	public void setDescription(Localisable<String, String> description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Localisable<String, String> getUrl() {
		return url;
	}

	public void setUrl(Localisable<String, String> url) {
		this.url = url;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	

}
