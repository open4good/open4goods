package org.open4goods.config.yml.ui;



import org.open4goods.model.Localisable;

/**
 *
 * @author goulven
 *
 */
public class NamesConfig {

	private String key;


	private Localisable value ;


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public Localisable getValue() {
		return value;
	}


	public void setValue(Localisable prompts) {
		this.value = prompts;
	}


}