package org.open4goods.commons.model.product;

public class IndexedAttribute extends SourcableAttribute{

	/**
	 * The name of this aggregated attribute
	 */
	private String name;

	/**
	 * The value of this aggregated attribute
	 */
	private String value;

	/**
	 * The numeric value (if any) of this aggregated attribute
	 */
	private Double numericValue;

	/**
	 * The numeric value (if any) of this aggregated attribute
	 */
	private Boolean boolValue;
	

	public IndexedAttribute() {
		super();
	}



	public IndexedAttribute(String key, String cleanedValue) {
		this.name = key; 
		this.value = cleanedValue;
		
		// Trying to specialize as numeric
		String num = cleanedValue.trim().replace(",", ".");
		try {
			final Double dblVal = Double.valueOf(num);
			numericValue = dblVal;
		} catch (final NumberFormatException e) {
		}

		boolValue= getBool(cleanedValue);
	}



	public static Boolean getBool(String cleanedValue) {
		String num;
		// Trying to specialize as boolean
		num = cleanedValue.toLowerCase();
		switch (num) {
		case "true":
		case "yes":
		case "oui":
		case "1":
		case "y":
			return Boolean.TRUE;

		case "no":
		case "non":
		case "false":
		case "n":
		case "0":
			return  Boolean.FALSE;

		default:
			return null;
		}
	}
		
	

	/**
	 * The associated icecat taxonomyIds 
	 */
//	@Field(index = false, store = false, type = FieldType.Keyword)
//	private Set<Integer> icecatTaxonomyIds = new HashSet<>();
	
//	
//	/**
//	 * Number of sources for this attribute
//	 * 
//	 * @return
//	 */
//	public int sourcesCount() {
//		return sources.size();
//	}
//
//	/**
//	 * The number of different values for this item
//	 * 
//	 * @return
//	 */
//	public long distinctValues() {
//		return sources.stream().map(UnindexedKeyVal::getValue).distinct().count();
//	}
//
//	/**
//	 * For UI, a String representation of all providers names
//	 * 
//	 * @return
//	 */
//	public String providersToString() {
//		return StringUtils.join(sources.stream().map(UnindexedKeyVal::getKey).toArray(), ", ");
//	}
//
//	/**
//	 * For UI, a String representation of all providers names and values
//	 * 
//	 * @return
//	 */
//	public String sourcesToString() {
//		return StringUtils.join(sources.stream().map(e -> e.getKey() + ":" + e.getValue()).toArray(), ", ");
//
//	}
//
//	public boolean hasConflicts() {
//		return distinctValues() > 1;
//	}
//
//	public String bgRow() {
//		String ret = "table-default";
//		int sCount = sourcesCount();
//		long dValues = distinctValues();
//
//		if (sCount == 0) {
//			ret = "table-danger";
//		} else if (sCount == 1) {
//			ret = "table-default";
//		} else {
//			ret = "table-info";
//		}
//
//		if (dValues > 1) {
//			ret = "table-danger";
//		}
//
//		return ret;
//	}

	// TODO : Simple, but does not allow to handle conflicts, and so on
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof IndexedAttribute) {
			return name.equals(((IndexedAttribute) obj).name);
		}
		return false;
	}

//	/**
//	 * Add a "matched" attribute, with dynamic type detection
//	 * 
//	 * @param parsed Should handle language ?
//	 */
//	public void addSourceAttribute(Attribute attr, AttributeConfig attrConfig, UnindexedKeyValTimestamp sourcedValue)
//			throws NumberFormatException {
//
//		// Guard
//		if (this.name != null && !name.equals(this.name)) {
//			// TODO
//			System.out.println("ERROR : Name mismatch in add attribute");
//		}
//
//		this.name = attr.getName();
//		sources.add(sourcedValue);
//
//		value = bestValue();
//
//		if (attrConfig.getType().equals(AttributeType.NUMERIC)) {
//			numericValue = numericOrNull(value);
//		}
//	}
//
//	public void addAttribute(Attribute attr, UnindexedKeyValTimestamp sourcedValue) {
//		// Guard
//		if (this.name != null && !name.equals(this.name)) {
//			// TODO
//			System.out.println("ERROR : Name mismatch in add attribute");
//		}
//
//		this.name = attr.getName();
//		sources.add(sourcedValue);
//
//		value = bestValue();
//
//	}
//
//	/**
//	 * 
//	 * @return the best value
//	 */
//	public String bestValue() {
//
//		// Count values by unique keys... NOTE : Should have a java8+ nice solution here
//		// !
//		Map<String, Integer> valueCounter = new HashMap<>();
//
//		for (UnindexedKeyValTimestamp source : sources) {
//
//			valueCounter.merge(source.getValue(), 1, Integer::sum);
//		}
//
//		// sort this map by values
//
//		Map<String, Integer> result = valueCounter.entrySet().stream()
//				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//
//				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue,
//						LinkedHashMap::new));
//
//		// Take the first one : will be the "most recommanded" if discriminant number of
//		// datasources, a random value otherwise
//
//		return result.entrySet().stream().findFirst().get().getKey();
//	}
//
//	/**
//	 * Return the number of distinct values
//	 * 
//	 * @return
//	 */
//	public long ponderedvalues() {
//		return sources.stream().map(UnindexedKeyVal::getValue).distinct().count();
//	}

	@Override
	public String toString() {
		return name + " : " + value;
	}

	public Double numericOrNull(String rawValue) throws NumberFormatException {
		// Trying to specialize as numeric
		final String num = rawValue.trim().replace(",", ".");

		return Double.valueOf(num);
	}

	///////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

//	public Set<UnindexedKeyValTimestamp> getSources() {
//		return sources;
//	}
//
//	public void setSources(Set<UnindexedKeyValTimestamp> sources) {
//		this.sources = sources;
//	}

	public Double getNumericValue() {
		return numericValue;
	}

	public void setNumericValue(Double numericValue) {
		this.numericValue = numericValue;
	}

	public Boolean getBoolValue() {
		return boolValue;
	}

	public void setBoolValue(Boolean boolValue) {
		this.boolValue = boolValue;
	}

	

}
