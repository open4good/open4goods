package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;

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



}
