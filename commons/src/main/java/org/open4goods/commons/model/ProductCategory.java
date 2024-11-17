package org.open4goods.commons.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.helper.IdHelper;

/**
 * A ProductCategory is an tree node of ProductCategories. It is built from 
 * GoogleProductCategories, is navigable and contains linked open4goods verticals and extra informations
 */
public class ProductCategory {
	
	/**
	 * The  matching google category
	 */
	private Integer googleCategoryId;
	
	/**
	 * The localised google names
	 */
	private Localisable<String, String> googleNames = new Localisable<String, String>();
	
	/**
	 * The urls 
	 */
	private Localisable<String, String> urls = new Localisable<String, String>();
	
	/**
	 * The hashed names (cleaned version of google names)
	 */
	private Localisable<String, String> hashedNames = new Localisable<String, String>();
	
	
	/**
	 * The associated VerticalConfig if any
	 */
	
	private VerticalConfig vertical;
	
	/**
	 * The children nodes
	 */
	private List<ProductCategory> children = new ArrayList<>();

	/**
	 * The parent category, if any
	 */
	private ProductCategory parent;
	
	public ProductCategory(Integer id, String name, String language) {
		this.googleCategoryId = id;
		googleNames.put(language, name);
		urls.put(language, toUrl(name));
		hashedNames.put(language, toHashedName(name));
	}
	
	private String toHashedName(String name) {
		return IdHelper.normalizeFileName(name);
	}

	private String toUrl(String name) {
		return IdHelper.normalizeFileName(name);
	}

	
	@Override
	public String toString() {
		return googleCategoryId+":"+googleNames.toString();
	}
	/**
	 * 
	 * @param name
	 * @param language
	 * @return
	 */
	public Optional<ProductCategory> getChildByName(String name , String language) {
		String hashName = toHashedName(name);
		return children.stream().filter(e->  hashName.equals(e.hashedNames.i18n(language))).findFirst();
	}
	
	public Optional<ProductCategory> getChildById(Integer id) {
		return children.stream().filter(e->  e.googleCategoryId.equals(id)).findFirst();
	}
	

	/**
	 * Add (if was not already existing) a children category
	 * @param pc
	 */
	public void addChild(ProductCategory pc) {
		if (getChildById(pc.getGoogleCategoryId()).isEmpty()) {
			children.add(pc);
		}
		
	}
	

	/**
	 * Add a language for a category
	 * @param last
	 * @param language
	 */
	public void addLanguage(String name, String language) {
		googleNames.put(language, name);
		urls.put(language, toUrl(name));
		hashedNames.put(language, toHashedName(name));
	}
	
	
	/**
	 * Return the full url path pointing to this category
	 * @param language
	 * @return
	 */
	public String url(String language) {

		StringBuilder sb = new StringBuilder();

		ProductCategory actual = this;

		sb.append(getUrls().get(language));

		while (actual.getParent() != null) {
//			if (!actual.isLeaf()) {
				sb.insert(0, actual.getParent().getUrls().get(language) + "/");
//			}
			actual = actual.getParent();
		}

		return sb.toString();
	}
	
	/**
	 * Return a map of url paths mapped to productCategories, recursivly 
	 * @param language
	 * @return
	 */
	public Map<String, ProductCategory> paths(String language) {
		Map<String, ProductCategory> ret =new HashMap<String, ProductCategory>();
		
		ret.put(url(language), this);
		
		// Recursiv adding children
		
		for (ProductCategory child : getChildren()) {
			ret.putAll(child.paths(language));
		}
		
		return ret;
	}

	

	/**
	 * 
	 * @return true if this is a terminal leaf
	 */
	private boolean isLeaf() {
		return children.size() == 0;
	}

	/**
	 * Return the hierarchy for this category
	 * 
	 * @return
	 */
	public List<ProductCategory> hierarchy() {

		List<ProductCategory> ret = new ArrayList<ProductCategory>();

		ProductCategory actual = this;

		ret.add(actual);

		while (actual.getParent() != null) {
			ret.add(0, actual.getParent());
			actual = actual.getParent();
		}

		return ret;
	}
	
	/////////////////////////////
	// Getters and setters
	/////////////////////////////
	
	
	
	
	
	
	
	

	public Integer getGoogleCategoryId() {
		return googleCategoryId;
	}

	public void setGoogleCategoryId(Integer googleCategoryId) {
		this.googleCategoryId = googleCategoryId;
	}

	public Localisable<String, String> getGoogleNames() {
		return googleNames;
	}

	public void setGoogleNames(Localisable<String, String> googleNames) {
		this.googleNames = googleNames;
	}

	public Localisable<String, String> getUrls() {
		return urls;
	}

	public void setUrls(Localisable<String, String> urls) {
		this.urls = urls;
	}

	public VerticalConfig getVertical() {
		return vertical;
	}

	public void setVertical(VerticalConfig vertical) {
		this.vertical = vertical;
	}

	public List<ProductCategory> getChildren() {
		return children;
	}

	public void setChildren(List<ProductCategory> children) {
		this.children = children;
	}

	public void setParent(ProductCategory parent) {
		this.parent = parent;
	}

	public ProductCategory getParent() {
		return parent;
	}

	public Localisable<String, String> getHashedNames() {
		return hashedNames;
	}

	public void setHashedNames(Localisable<String, String> hashedNames) {
		this.hashedNames = hashedNames;
	}

	
	
	
	
}
