package org.open4goods.config.yml.ui;

import org.open4goods.model.Localisable;

public class PageLink {
	private Localisable url = new Localisable();
	private String faIco;
	private Localisable label = new Localisable();
	private Localisable hint = new Localisable();

	public Localisable getUrl() {
		return url;
	}

	public void setUrl(final Localisable url) {
		this.url = url;
	}

	public String getFaIco() {
		return faIco;
	}

	public void setFaIco(final String faIco) {
		this.faIco = faIco;
	}

	public Localisable getLabel() {
		return label;
	}

	public void setLabel(final Localisable label) {
		this.label = label;
	}

	public Localisable getHint() {
		return hint;
	}

	public void setHint(final Localisable hint) {
		this.hint = hint;
	}

}
