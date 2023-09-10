package org.open4goods.model.product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedAttribute implements IAttribute {

	/**
	 * The name of this aggregated attribute
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String name;
	
	/**
	 * The value of this aggregated attribute
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String value;


//	/** Type of the attribute **/
//	@Field(index = false, store = false, type = FieldType.Keyword)
//	private AttributeType type;


	/**
	 * The collections of conflicts for this attribute
	 */
	@Field(index = false, store = false, type = FieldType.Object)
	private Set<UnindexedKeyValTimestamp> sources = new HashSet<>();



	// TODO : Simple, but does not allow to handle conflicts, and so on
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof AggregatedAttribute) {
			return name.equals(((AggregatedAttribute)obj).name);
		}
		return false;
	}


	

	/**
	 * Add an attribute
	 * @param parsed
	 * Should handle language ?
	 */
	public void addAttribute(String name, UnindexedKeyValTimestamp sourcedValue) {

		// Guard
		if (this.name != null && !name.equals(this.name)) {
			System.out.println("ERROR : Name mismatch in add attribute");
		}
		
		this.name = name;		
		sources.add(sourcedValue);		
		value = bestValue();
		
	}


/**
 * 
 * @return the best value
 */
	public String bestValue() {

		// Count values by unique keys... NOTE : Should have a java8+ nice solution here !
		Map<String, Integer> valueCounter = new HashMap<>();
		
		for (UnindexedKeyValTimestamp source : sources) {
			Integer existing = valueCounter.get(source.getValue());
			
			if (null == existing) {
				valueCounter.put(source.getValue(),1);
			} else {
				valueCounter.put(source.getValue(),valueCounter.get(source.getValue())+ 1);
			}
		}
				
	
		// sort this map by values

	    
	    Map<String,Integer> result = valueCounter.entrySet().stream()
	    		.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) 		
	    	
	    		.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
	    		(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	    
	    
	    // Take the first one : will be the "most recommanded" if discriminant number of datasources, a random value otherwise
	    
		return result.entrySet().stream().findFirst().get().getKey();
	}

	/**
	 * Return the number of distinct values
	 * @return
	 */
	public long getPonderedvalues() {
		return sources.stream().map(e->e.getValue()).distinct().count();
	}
	
	@Override
	public String toString() {

		return name + " : " +value+ " -> "+  sources.size() + " source(s), " + getPonderedvalues() + " conflict(s)";


	}

	///////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////


	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}



	@Override
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}


//	public AttributeType getType() {
//		return type;
//	}
//
//
//	public void setType(AttributeType type) {
//		this.type = type;
//	}

	public Set<UnindexedKeyValTimestamp> getSources() {
		return sources;
	}

	public void setSources(Set<UnindexedKeyValTimestamp> sources) {
		this.sources = sources;
	}



	@Override
	public String getLanguage() {
		return null;
	}








}
