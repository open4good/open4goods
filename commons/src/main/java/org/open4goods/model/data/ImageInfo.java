package org.open4goods.model.data;

import java.util.HashSet;
import java.util.Set;

public class ImageInfo {

	private Integer height;
	private Integer width;


	private Set<Integer> availlableThumbs = new HashSet<>();




	public ImageInfo() {
		super();
	}
	public Integer getHeight() {
		return height;
	}
	public void setHeight(final Integer height) {
		this.height = height;
	}
	public Integer getWidth() {
		return width;
	}
	public void setWidth(final Integer width) {
		this.width = width;
	}
	public Set<Integer> getAvaillableThumbs() {
		return availlableThumbs;
	}
	public void setAvaillableThumbs(final Set<Integer> availlableThumbs) {
		this.availlableThumbs = availlableThumbs;
	}




}
