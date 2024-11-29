package org.open4goods.commons.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.open4goods.commons.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access to nsted Productegory lists, via first level nodes
 */
public class ProductCategories {
	private List<ProductCategory> nodes = new ArrayList<ProductCategory>();

	protected static final Logger logger = LoggerFactory.getLogger(ProductCategories.class);
	/**
	 * Createor merge a given google product categories path
	 * @param id
	 * @param fragments
	 * @param language
	 * @return 
	 */
	public ProductCategory addGooglecategories(Integer id, List<String> fragments, String language) {
		
		ProductCategory ret = null;
		if (fragments.size() == 1) {
			// Root nodes
			ProductCategory existing = getRootById(id).orElse(null);
			if (null == existing) {
				ProductCategory pc = new ProductCategory(id,fragments.getLast(), language);
				nodes.add(pc);
				ret = pc;
			}
			if (null != existing) {
				// Merging the language
				existing.addLanguage(fragments.getLast(),language);
				ret = existing;
			}
		} else {
			List<String> path = fragments.subList(0, fragments.size() -1);
			
			ProductCategory parent = null;
			for (String fragment : path) {
				if (null != parent) {
					parent = parent.getChildByName(fragment, language).orElse(null);
				} else {
					parent = getRootByName(fragment, language).orElse(null);
				}
				
				if (null == parent) {
					logger.error("empty parent for id {} - {}",fragments);
				}
			}

			
			ProductCategory existing = parent.getChildById(id).orElse(null);
			if (null == existing) {
				ProductCategory pc = new ProductCategory(id,fragments.getLast(), language);
				pc.setParent(parent);
				parent.addChild(pc);	
				ret = pc;
			} else {
//				language update
				existing.addLanguage(fragments.getLast(), language);
				ret = existing;
			}
			
			
//			String leaf = fragments.getLast();		
//			
//			if (path.size() == 0) {
//				// We create a terminal leaf 
//				this.googleCategoryId = id;			
//				urls.put(language, toUrl(leaf));
//				googleNames.put(language, leaf);
//			} else {
//				// We get recursivly assuming the parents have previously been created
//				
//			}
			
			
		}
		return ret;
		
	}


	/**
	 * Return a root PRoductCategory by its google id
	 * @param id
	 * @return
	 */
	public Optional<ProductCategory> getRootById(Integer id) {
		return nodes.stream().filter(e->e.getGoogleCategoryId().equals(id)).findFirst();
	}
	
	/**
	 * 
	 * @param name
	 * @param language
	 * @return
	 */
	public Optional<ProductCategory> getRootByName(String name , String language) {
		String hashName = IdHelper.normalizeFileName(name);
		return nodes.stream().filter(e->  hashName.equals(e.getHashedNames().i18n(language))).findFirst();
	}

	/**
	 * Recursiv list all paths, in a given language
	 * @param language
	 * @return
	 */
	public Map<String, ProductCategory> paths(String language) {
		
		
		Map<String, ProductCategory> ret = new HashMap<String, ProductCategory>();
		
		nodes.forEach(e -> {
			ret.putAll(e.paths(language));
		});
		
		return ret;
	}
	
	
	/**
	 * Get the root categories as an empty ProductCategory, with children 
	 * @return
	 */
	public ProductCategory asRootNode() {
		ProductCategory pc = new ProductCategory(0, "root", "default");
		pc.setChildren(nodes);
		return pc;
	}
	
	
	

	public List<ProductCategory> getNodes() {
		return nodes;
	}


	public void setNodes(List<ProductCategory> nodes) {
		this.nodes = nodes;
	}



	
	
}
