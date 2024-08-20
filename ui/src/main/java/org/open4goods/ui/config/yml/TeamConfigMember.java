package org.open4goods.ui.config.yml;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
public class TeamConfigMember {

	/**
	 * Name o be displaid
	 */
	@NotEmpty
	private String name;
	/**
	 * Title to be displaid
	 */
	@NotEmpty
	private String title;
	
	/**
	 * Link to the LinkedIn profile
	 */
	@NotEmpty
	private String linkedInUrl;
	
	/**
	 * Link to external -or better internal- image URL
	 */
	@NotEmpty
	private String imageUrl;
	
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getLinkedInUrl() {
		return linkedInUrl;
	}
	public void setLinkedInUrl(String linkedInUrl) {
		this.linkedInUrl = linkedInUrl;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	
}
