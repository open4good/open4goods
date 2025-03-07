package org.open4goods.commons.model.dto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalFilterTerm {
	String id;
	String text;
	Long count;

	public VerticalFilterTerm(String text, Long count) {
		super();
		this.text = text;
		this.count = count;
		id=text.replaceAll("[^A-Za-z0]", "");

	}


	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * ui helper
	 * @return
	 */
	public String urlEncodedText() {
		try {
			return URLEncoder.encode(text.toLowerCase(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return text;
		}
	}


	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}



}
