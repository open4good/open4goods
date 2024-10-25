package org.open4goods.commons.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.commons.model.constants.ReferentielKey;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductAttributes  {

	/**
	 * The referentiel attributes
	 */
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();
	
	private Map<String,IndexedAttribute> indexed = new HashMap<>();	
	
	private Map<String, ProductAttribute> all = new HashMap<>();

	@Transient
	// Instance life cache for fastenning features
	private Set<ProductAttribute> featuresCache;

	
	
	/**
	 * Best effort method to return the string val of an attribute, looking up, by priority in :
	 * > ReferentielAttributes
	 * > IndexedAttributes
	 * > All 
	 */
	public String val(String key) {
		
		String value = null;
		// Checking in referentielkey
		boolean rKey = ReferentielKey.isValid(key);
		if (rKey) {
			value = referentielAttributes.get(ReferentielKey.valueOf(key));
		}

		if (null == value) {
			IndexedAttribute ia = indexed.get(key);
			if (null != ia) {
				value = ia.getValue();
			}
		}

		if (null == value) {
			ProductAttribute ia = all.get(key);
			if (null != ia) {
				value = ia.getValue();
			}
		}
		
		return value;
	}

	@Override
	public String toString() {
		return "ref:"+referentielAttributes.size()+ " , indexed:"+indexed.size() +" , all:"+all.size();
	}
	
	

	public int count() {
		
		return referentielAttributes.size() + indexed.size() + all.size();
	}
	
	public Set<ProductAttribute> features() {
		
		if (this.featuresCache == null) {
			featuresCache = all.values().stream().filter(e -> Boolean.TRUE.equals(IndexedAttribute.getBool(e.getValue()))  ).collect(Collectors.toSet());
		}
		return featuresCache;
	}
	
	

	// TODO(p3,perf) : performance
	public ProductAttribute attributeByFeatureId(Integer featureId) {
		if (null == featureId) {
			return null;
		}
		
		ProductAttribute ret = all.values().stream().filter(a -> a.getIcecatTaxonomyIds().contains(featureId)).findFirst()
				.orElse(null);
				return ret;
	}
	
	public void addReferentielAttribute(ReferentielKey key, String value) {
		referentielAttributes.put(key, value);
	}

	public Map<String, String> referentielAttributesAsStringKeys() {
		return referentielAttributes.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey().toString(),
				Map.Entry::getValue
					
				));
	}
	

	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}



	public Map<String, IndexedAttribute> getIndexed() {
		return indexed;
	}



	public void setIndexed(Map<String, IndexedAttribute> indexed) {
		this.indexed = indexed;
	}



	public Map<String, ProductAttribute> getAll() {
		return all;
	}



	public void setAll(Map<String, ProductAttribute> all) {
		this.all = all;
	}


	
}
