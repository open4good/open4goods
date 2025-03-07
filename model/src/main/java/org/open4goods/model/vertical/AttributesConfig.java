package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;

public class AttributesConfig {


	/**
	 * The specific configs configurations
	 */
	@JsonMerge
	private List<AttributeConfig> configs = new ArrayList<AttributeConfig>();

	/**
	 * If set, the list of configs values making an attribute to be categorized as a "Feature" (must be uppercase)
	 */
	@JsonMerge
	private Set<String> featuredValues = new HashSet<>();


	/**
	 * If set, the list of attributes names to be excluded from attributes. Will not be aggregated in the product data
	 */
	@JsonMerge
	private Set<String> exclusions = new HashSet<>();



	// Internal map
	@JsonIgnore
	private Map<String, AttributeConfig> hashedAttributesByKey;


	// Local cache
	private Map<String, Map<String, String>> cacheHashedSynonyms;

	private Map<String, String> valueKeyMap = new HashMap<String, String>();

	/**
	 * Attributes config by feature groups
	 */
	private Map<String, AttributeConfig> byIcecatFeatureGroup = new HashMap<String, AttributeConfig>();
	

	public AttributesConfig(List<AttributeConfig> configs) {
		this.configs = configs;

	}


	public AttributesConfig() {
	}


	/**
	 * Return the attribute config key name for an attribute name 
	 * @param value
	 * @return
	 */
	public String getKeyForValue(final String value) {
		
		if (cacheHashedSynonyms == null) {
			synonyms();
		}
        return valueKeyMap.get(value);
    }
	
	
	
	
	
	/**
	 * Get all configs synonyms by provider
	 *
	 * @return A map-ProviderKey,Synonym, Translated Key
	 */
	// Spring cache ineffectiv on internal calls
//	@Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	public Map<String, Map<String, String>> synonyms() {

		if (cacheHashedSynonyms == null) {
			
			final Map<String, Map<String, String>> hashedSynonyms = new HashMap<>();
	
			if (null != configs) {
				for (final AttributeConfig ac : configs) {
					
					for (String id : ac.getIcecatFeaturesIds()) {
						byIcecatFeatureGroup.put(id, ac);
					}
					
					for (final Entry<String, Set<String>> entry : ac.getSynonyms().entrySet()) {
						if (!hashedSynonyms.containsKey(entry.getKey())) {
							hashedSynonyms.put(entry.getKey(), new HashMap<>());
							
						}
						valueKeyMap.put(ac.getKey(), ac.getKey());
						for (final String val : entry.getValue()) {
							hashedSynonyms.get(entry.getKey()).put(val, ac.getKey());
							// Also build a reverse map
							valueKeyMap.put(val, ac.getKey());
						}
					}
				}
				
			}
			cacheHashedSynonyms = hashedSynonyms;
		}
		return cacheHashedSynonyms;
	}


	/**
	 * Resolve a product attribute against icecat taxonomy or faalback on attributes config 
	 * @param attr
	 * @return
	 */
	public AttributeConfig resolveFromProductAttribute(ProductAttribute attr) {
	
		AttributeConfig ret = null;
		
		
		// 1 - Checking from icecat forced taxonomy
		for (SourcedAttribute source : attr.getSource()) {

			if (null != source.getIcecatTaxonomyId()) {
				ret = byIcecatFeatureGroup.get(String.valueOf(source.getIcecatTaxonomyId()));				
			}
			
			if (null != ret) {
				break;
			}
		}
		
		
		// 2 - Looking by specific datasource name
		if (null == ret) {
			for (SourcedAttribute source : attr.getSource()) {
				Map<String, String> specSynonyms = synonyms().get(source.getDataSourcename());

				if (null != specSynonyms) {
					ret =  getAttributeConfigByKey(specSynonyms.get(attr.getName()));
					if (null != ret) {
						break;
					}
				}
			}
		}

		// 3 - Looking in the all attributes
		if (null == ret) {
				Map<String, String> specSynonyms = synonyms().get("all");

				if (null != specSynonyms) {
					ret =  getAttributeConfigByKey(specSynonyms.get(attr.getName()));
				}
		}
		
		return ret;
		
		
	}
	
	

	/**
	 * Gets an attribute config by it's id
	 * @param key
	 * @return
	 */
	public AttributeConfig getAttributeConfigByKey(final String key) {
		singletonHashAttrs();
		final AttributeConfig ac = hashedAttributesByKey.get(key);
		return ac;
	}

	/**
	 * Return the attributeConfig for an attribute, if any
	 *
	 * @param indexedName
	 * @return
	 */
	public AttributeConfig getConfigFor(final String indexedName) {
		return getAttributeConfigByKey(indexedName);
	}

	private void singletonHashAttrs() {
		
		// Caching if needed
		if (null == hashedAttributesByKey) {
			hashedAttributesByKey = new HashMap<>();
			if (null == configs) {
				return;
			}
			for (final AttributeConfig a : configs) {
				hashedAttributesByKey.put(a.getKey(), a);

			}
		}
	}





	public List<AttributeConfig> getConfigs() {
		return configs;
	}


	public void setConfigs(List<AttributeConfig> configs) {
		this.configs = configs;
	}


	public Set<String> getFeaturedValues() {
		return featuredValues;
	}


	public void setFeaturedValues(Set<String> featuredValues) {
		this.featuredValues = featuredValues;
	}


	public Set<String> getExclusions() {
		return exclusions;
	}


	public void setExclusions(Set<String> exclusions) {
		this.exclusions = exclusions;
	}



}
