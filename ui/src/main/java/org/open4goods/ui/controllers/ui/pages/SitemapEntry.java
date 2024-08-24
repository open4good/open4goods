package org.open4goods.ui.controllers.ui.pages;

import org.open4goods.commons.model.Localisable;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;

public class SitemapEntry extends Localisable<String,String>{
	
	private static final long serialVersionUID = 1L;
	public static final String LANGUAGE_DEFAULT = "default";
	public static final String LANGUAGE_FR = "fr";

	private double priority;
	
	private ChangeFreq frequency;
	
	
	
	public static SitemapEntry of(String language, String url, double priority, ChangeFreq frequency) {

		SitemapEntry ret = new SitemapEntry();
		ret.setFrequency(frequency);
		ret.setPriority(priority);
		ret.put(language, url);
		return ret;
	}

	public SitemapEntry add (String language, String url) {
		this.put(language, url);
		return this;
	}

	public double getPriority() {
		return priority;
	}



	public void setPriority(double priority) {
		this.priority = priority;
	}



	public ChangeFreq getFrequency() {
		return frequency;
	}



	public void setFrequency(ChangeFreq frequency) {
		this.frequency = frequency;
	}

	
	
}
