package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attribute parsing configuration
 * @author Goulven.Furet
 *
 */
public class AttributeParserConfig {

	/**
	 * If true, attribute value will be normalized (spaces)
	 */
	private Boolean normalize=true;

	/**
	 * If true, attribute value will be trimed (spaces)
	 */
	private Boolean trim=false;

	/**
	 * If true, attribute value will be lowercased
	 */
	private Boolean lowerCase=false;

	/**
	 * If true, attribute value will be uppercased
	 */
	private Boolean upperCase=true;

	/**
	 * If defined, this className will be used to parse the item. Must be a specialisation of AttributeParser, TextAttributeParser or BooleanAttributeParser
	 */
	private String clazz;

	/**
	 * If true all parentheses and inner text will be removed
	 */
	private boolean removeParenthesis = false;

	/**
	 * Will delete those texts from attribute value before handling
	 */
	private List<String> deleteTokens = new ArrayList<>();

	/**
	 * If one of the string is found in the text, then associate the one found
	 */
	private List<String> tokenMatch;

	/**
	 * Physical dimension name used by {@code UnitAwareNumericParser} to look up
	 * conversion factors in the UUDC registry (e.g. "LENGTH", "MASS", "POWER").
	 * Ignored by other parsers.
	 */
	private String dimension;

	/**
	 * Default unit symbol applied when the raw value carries no unit (e.g. "cm" for LENGTH,
	 * "kg" for MASS). Used by {@code UnitAwareNumericParser} as a last-resort hint.
	 */
	private String defaultUnitHint;

	/**
	 * Unit of the values that a raw attribute sends without a unit, by raw attribute name (compared after
	 * {@code IdHelper.normalizeAttributeName}). For example, EPREL sends the hourly consumption of an air conditioner
	 * as "1.1", in kWh/h. Only the watt conversion of the generic parser uses it. Remove an entry to go back to
	 * reading these values as they are.
	 */
	private Map<String, String> sourceUnits = new HashMap<>();

	/**
	 * Lower bound of a plausible numeric value, in the unit of the attribute. A source whose parsed value is below it
	 * does not vote. {@code null} means no bound.
	 */
	private Double minValue;

	/**
	 * Upper bound of a plausible numeric value, in the unit of the attribute (an identifier read as a screen size
	 * is far above any diagonal). A source whose parsed value is above it does not vote. {@code null} means no bound.
	 */
	private Double maxValue;

	/**
	 * Conversion factors of a value written with an explicit unit, by unit token (case-insensitive): with
	 * {@code cm: 0.3937}, "165 cm" is read as 64.96. A value without unit, or with an unlisted unit, is unchanged.
	 */
	private Map<String, Double> unitFactors = new HashMap<>();




	//////////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////////


	public List<String> getDeleteTokens() {
		return deleteTokens;
	}

	public void setDeleteTokens(final List<String> deleteTokens) {
		this.deleteTokens = deleteTokens;
	}

	public Boolean getLowerCase() {
		return lowerCase;
	}

	public void setLowerCase(final Boolean lowerCase) {
		this.lowerCase = lowerCase;
	}

	public Boolean getUpperCase() {
		return upperCase;
	}

	public void setUpperCase(final Boolean upperCase) {
		this.upperCase = upperCase;
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(final String clazz) {
		this.clazz = clazz;
	}

	public boolean isRemoveParenthesis() {
		return removeParenthesis;
	}

	public void setRemoveParenthesis(final boolean removeParenthesis) {
		this.removeParenthesis = removeParenthesis;
	}

	public List<String> getTokenMatch() {
		return tokenMatch;
	}

	public void setTokenMatch(final List<String> tokenMatch) {
		this.tokenMatch = tokenMatch;
	}

	public Boolean getTrim() {
		return trim;
	}

	public void setTrim(final Boolean trim) {
		this.trim = trim;
	}

	public Boolean getNormalize() {
		return normalize;
	}

	public void setNormalize(final Boolean normalize) {
		this.normalize = normalize;
	}

	public String getDimension()
	{
		return dimension;
	}

	public void setDimension(String dimension)
	{
		this.dimension = dimension;
	}

	public String getDefaultUnitHint()
	{
		return defaultUnitHint;
	}

	public void setDefaultUnitHint(String defaultUnitHint)
	{
		this.defaultUnitHint = defaultUnitHint;
	}

	public Double getMinValue() {
		return minValue;
	}

	public void setMinValue(Double minValue) {
		this.minValue = minValue;
	}

	public Double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(Double maxValue) {
		this.maxValue = maxValue;
	}

	public Map<String, Double> getUnitFactors() {
		return unitFactors;
	}

	public void setUnitFactors(Map<String, Double> unitFactors) {
		this.unitFactors = unitFactors == null ? new HashMap<>() : unitFactors;
	}

	public Map<String, String> getSourceUnits() {
		return sourceUnits;
	}

	public void setSourceUnits(Map<String, String> sourceUnits) {
		this.sourceUnits = sourceUnits == null ? new HashMap<>() : sourceUnits;
	}



}
