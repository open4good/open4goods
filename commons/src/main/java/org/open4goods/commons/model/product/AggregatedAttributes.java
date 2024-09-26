package org.open4goods.commons.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.ui.ProductI18nElements;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
//@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatedAttributes  {

	
	transient Map<String,AggregatedAttribute> map;
	
	/**
	 * The referentiel attributes
	 */
	@Field(enabled = false, store = false, type = FieldType.Object)
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();

	//TODO(p3,naming) : rename
	/**
	 * The key is the english name, such as classified by icecat
	 */
	
//	@Field( store = false, type = FieldType.Object)
//	private Map<String,AggregatedAttribute> attributes = new HashMap<>();
//
//	@Field(enabled = false, store = false, type = FieldType.Object)
//	private Set<AggregatedAttribute> unmatchedAttributes = new HashSet<>();

	@Field(type = FieldType.Nested)
	private Set<AggregatedAttribute> attrs = new HashSet<>();
	
	@Override
	public String toString() {
		return "ref:"+referentielAttributes.size()+ " , agg:"+attrs.size();
	}
	
	

	/**
	 * 
	 * @return
	 */
	public long countMatched() {
		
		return attrs.stream().filter(e->e.getIcecatTaxonomyIds().size() >0).count();
	}
	


	/**
	 * 
	 * @return a map version of attributes, with attribute name as key. The map is internally cached for optimisation
	 */
	public Map<String, AggregatedAttribute> toMap() {
		if (null == map) {
			map = attrs.stream().collect(Collectors.toMap(AggregatedAttribute::getName, Function.identity()));
		}
		return map;
	}

	
	/**
	 * Special trick on the setter : We invalidate the local cache
	 * @param attrs
	 */
	public void setAttrs(Set<AggregatedAttribute> attrs) {
		this.map = null;
		this.attrs = attrs;
	}

	
	
	// TODO : performance
	public AggregatedAttribute attributeByFeatureId(Integer featureId) {
		if (null == featureId) {
			return null;
		}
		
		AggregatedAttribute ret = attrs.stream().filter(a -> a.getIcecatTaxonomyIds().contains(featureId)).findFirst()
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

//
//	public Map<String, AggregatedAttribute> getAttributes() {
//		return attributes;
//	}
//	public void setAttributes(Map<String, AggregatedAttribute> aggregatedAttributes) {
//		this.attributes = aggregatedAttributes;
//	}
//
//
//	public Set<AggregatedAttribute> getUnmatchedAttributes() {
//		return unmatchedAttributes;
//	}
//
//	public void setUnmatchedAttributes(Set<AggregatedAttribute> unmapedAttributes) {
//		this.unmatchedAttributes = unmapedAttributes;
//	}

	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}

	public Set<AggregatedAttribute> getAttrs() {
		return attrs;
	}






	

}
