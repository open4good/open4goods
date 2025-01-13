package org.open4goods.commons.config.yml.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.open4goods.commons.model.Localisable;

public class VerticalSubset {
	
	private String id;
	
	private String group;
	
	private List<SubsetCriteria> criterias = new ArrayList<>();
	
	private String image;
	
	private Localisable<String, String> url;
	
	// For rendering in list in vertical-home
	private Localisable<String, String> caption = new Localisable<String, String>();
	
	private Localisable<String, String> title = new Localisable<String, String>();
	
	private Localisable<String, String> description = new Localisable<String, String>();

	
	
	@Override
	public int hashCode() {
		return Objects.hash(id, group, criterias, image, url, title, description);
	}

	// equals method
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		VerticalSubset other = (VerticalSubset) obj;
		return Objects.equals(id, other.id) && Objects.equals(group, other.group) && Objects.equals(criterias, other.criterias) && Objects.equals(image, other.image) && Objects.equals(url, other.url) && Objects.equals(title, other.title) && Objects.equals(description, other.description);
	}
	    
	    
	    
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

	public Localisable<String, String> getCaption() {
		return caption;
	}

	public void setCaption(Localisable<String, String> caption) {
		this.caption = caption;
	}
	
	

}
