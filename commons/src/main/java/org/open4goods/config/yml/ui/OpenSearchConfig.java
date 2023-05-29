package org.open4goods.config.yml.ui;

import org.open4goods.model.Localisable;

/**
 * Documentation on opensearch here : https://developer.mozilla.org/en-US/docs/Web/OpenSearch
 * @author Goulven.Furet
 *
 */
public class OpenSearchConfig {


	/**
	 * A short name for the search engine. It must be 16 or fewer characters of plain text, with no HTML or other markup.
	 */
	private Localisable shortName;


	/** A brief description of the search engine. It must be 1024 or fewer characters of plain text, with no HTML or other markup. **/
	private Localisable description;

	/**
	 * URI of an 16×16 image of type image/x-icon for the search engine.
	 * The URI may also use the data: URI scheme. (You can generate a data: URI from an icon file at The data: URI kitchen.)
	 *
	 * **/
	private Localisable ico16;

	/**
	 * URI of an 64×64 image of type image/jpeg or image/png for the search engine.
	 * The URI may also use the data: URI scheme. (You can generate a data: URI from an icon file at The data: URI kitchen.)
	 *
	 * **/
	private Localisable ico64;

	public Localisable getShortName() {
		return shortName;
	}

	public void setShortName(Localisable shortName) {
		this.shortName = shortName;
	}

	public Localisable getDescription() {
		return description;
	}

	public void setDescription(Localisable description) {
		this.description = description;
	}

	public Localisable getIco16() {
		return ico16;
	}

	public void setIco16(Localisable ico16) {
		this.ico16 = ico16;
	}

	public Localisable getIco64() {
		return ico64;
	}

	public void setIco64(Localisable ico64) {
		this.ico64 = ico64;
	}





}
