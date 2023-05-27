package org.open4goods.config.yml.ui;

import org.open4goods.model.Localisable;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
public class AttributeMatching {

	@NotBlank
	private String spel;

	@NotNull
	private Localisable title;

	@NotNull
	private Localisable description;

	@NotNull
	private Localisable logo;



	public String getSpel() {
		return spel;
	}

	public void setSpel(final String spel) {
		this.spel = spel;
	}

	public Localisable getTitle() {
		return title;
	}

	public void setTitle(final Localisable title) {
		this.title = title;
	}

	public Localisable getDescription() {
		return description;
	}

	public void setDescription(final Localisable description) {
		this.description = description;
	}

	public Localisable getLogo() {
		return logo;
	}

	public void setLogo(final Localisable logo) {
		this.logo = logo;
	}

}
