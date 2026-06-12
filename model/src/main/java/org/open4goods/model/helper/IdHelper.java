package org.open4goods.model.helper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.util.ProductModelCandidateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author goulven
 *
 */
public class IdHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdHelper.class);

	/** Legacy token pattern used by {@link #extractModelTokens}. */
	private static final Pattern LEGACY_BRAND_UID_PATTERN = Pattern.compile("\\w*\\-?(\\d[A-Za-z])+|([A-Za-z]\\d)+\\-?\\w*");

	private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

	private static final int TARGET_DIMS = 512;



	/***
	 * The key used for simple encryptions. Mabe a need one day to handle it from
	 * conf
	 */
	private static final String XOR_KEY = "that'sawonderfulkeyno??";


	/**
	 * Generates a resource hash
	 *
	 * @param url
	 * @return
	 */
	public static String generateResourceId(final String url) {
		return FNV.hash64(url);
	}
	// ISSUE : test2

	/**
	 * Sanitize (html unescape, space normalisations)
	 *
	 * @param input
	 * @return
	 */
	public static String sanitize(final String input)
	{
		if (StringUtils.isEmpty(input))
		{
			return null;
		}
		String unescaped = input.indexOf('&') != -1 ? StringEscapeUtils.unescapeHtml4(input) : input;
		final String ret = unescaped.trim();

		if (StringUtils.isEmpty(ret))
		{
			return null;
		}

		return ret;
	}

	public static String sanitizeAndNormalize(final String input)
	{
		if (input == null)
		{
			return null;
		}

		int len = input.length();
		if (len == 0)
		{
			return input;
		}

		boolean hasAmp = false;
		boolean needsNormalize = false;

		if (Character.isWhitespace(input.charAt(0)) || Character.isWhitespace(input.charAt(len - 1))) {
			needsNormalize = true;
		}

		if (!needsNormalize) {
			boolean lastWasSpace = false;
			for (int i = 0; i < len; i++) {
				char c = input.charAt(i);
				if (c == '&') {
					hasAmp = true;
				}
				if (Character.isWhitespace(c)) {
					if (c != ' ' || lastWasSpace) {
						needsNormalize = true;
						break;
					}
					lastWasSpace = true;
				} else {
					lastWasSpace = false;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				if (input.charAt(i) == '&') {
					hasAmp = true;
					break;
				}
			}
		}

		if (!hasAmp && !needsNormalize) {
			return input;
		}

		String unescaped = hasAmp ? StringEscapeUtils.unescapeHtml4(input) : input;
		return needsNormalize ? StringUtils.normalizeSpace(unescaped) : unescaped;
	}

	/**
	 * Clean diacritics using a precompiled pattern, skipping entirely for ASCII-only strings.
	 */
	public static String stripAccents(final String input) {
		if (input == null) {
			return null;
		}
		boolean hasNonAscii = false;
		int len = input.length();
		for (int i = 0; i < len; i++) {
			if (input.charAt(i) > 127) {
				hasNonAscii = true;
				break;
			}
		}
		if (!hasNonAscii) {
			return input;
		}
		final String decomposed = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
		return DIACRITICS_PATTERN.matcher(decomposed).replaceAll("");
	}

	/**
	 * Normalize
	 * @param string
	 * @return
	 */
	public static String normalizeAttributeName(String string) {
		if (string == null) {
			return null;
		} else {
			return stripAccents(string).trim().toUpperCase();
		}
	}

	/**
	 * Extracts the most likely manufacturer model code from a data fragment's offer
	 * names by delegating to
	 * {@link ProductModelCandidateHelper#extractModelsFromTitles}.
	 *
	 * <p>The best (highest-frequency, then shortest, then lexical) candidate is
	 * returned. When no qualifying token is found, {@code null} is returned.
	 * Ambiguous multi-candidate cases are resolved by the central helper's ranking
	 * instead of throwing.
	 *
	 * @param dataFragment source fragment whose names are scanned
	 * @return best model candidate, or {@code null} when none qualifies
	 */
	public static String extractModelFromNames(final DataFragment dataFragment) {
		return ProductModelCandidateHelper.extractModelsFromTitles(dataFragment.getNames()).best();
	}

	/**
	 * Extracts manufacturer-like model tokens from a single string using a
	 * legacy token-boundary pattern. Used by Amazon model string shortening; prefer
	 * {@link ProductModelCandidateHelper#extractModelsFromTitles} for new callers.
	 *
	 * @param name raw string to scan
	 * @return set of uppercased model-like tokens (may be empty)
	 */
	public static Set<String> extractModelTokens(final String name) {
		final Set<String> tokens = new HashSet<>();
		for (final String fragment : name.split(" ")) {
			if (fragment.length() > 3 && LEGACY_BRAND_UID_PATTERN.matcher(fragment).find()) {
				tokens.add(fragment.toUpperCase());
				LOGGER.debug("Extracted model token ({}) from ({})", fragment, name);
			}
		}
		return tokens;
	}

	public static String getCategoryName(final String name) {

		if (StringUtils.isBlank(name)) {
			return null;
		}

		String [] frags = name.split("\n");

		List<String> frs = Arrays.stream(frags).filter(e ->  ! StringUtils.isBlank(StringUtils.normalizeSpace( e))).collect(Collectors.toList());

		String ret = StringUtils.join(frs," > ");
		ret = stripAccents(ret);
		ret = ret.toUpperCase();
		ret = ret.replace("\n", " > ");
		ret = StringUtils.normalizeSpace(ret);
		return ret;

	}

	public static String azCharAndDigits(final String input) {
		return stripAccents(input).replaceAll("[^a-zA-Z0-9]", "");
	}

	public static String azCharAndDigits(final String input, String replacement) {
		return stripAccents(input).replaceAll("[^a-zA-Z0-9]", replacement);
	}

	public static String azCharAndDigitsPointsDash(String input) {
		return azCharAndDigits(input);
	}


	public static String azCharAndDigitsPointsDash(String input, String string) {
		return stripAccents(input).replaceAll("[^a-zA-Z0-9_-]",string);
	}

	/**
	 * Creates a datasource ID by keeping only alphanumeric characters, dots, dashes and underscores.
	 * @param input the input string
	 * @return cleaned string
	 */
	public static String toDatasourceId(String input) {
		if (input == null) return null;
		return stripAccents(input).replaceAll("[^a-zA-Z0-9_.-]", "");
	}


	/**
	 * Return a clean hashed name
	 *
	 * @param name
	 * @return
	 */
	public static String getHashedName(final String name){

		final StringBuilder ret = new StringBuilder();
		for (final char c : name.toCharArray()) {
			if (c != ')' && c != '(' && c != '`' && c != ' ' && c != '/' && c != '!' && c != ':' && c != '.' && c != '_' && c != '-' && c != '?'
					&& c != '.' && c != '\'' && c != ',' && c != '"' && c != ',' && c != '"') {
				ret.append(c);
			}
		}
		return ret.toString();
	}

	/**
	 * Return a clean hashed name
	 *
	 * @param name
	 * @return
	 */
	public static String getUrlName(final String name){

		final StringBuilder ret = new StringBuilder();
		for (final char c : name.toCharArray()) {
			if (c != ')' && c != '(' && c != '`' && c != ' ' && c != '/' && c != '!' && c != ':' && c != '.' && c != '_'  && c != '?'
					&& c != '.' && c != '\'' && c != ',' && c != '"' && c != ',' && c != '"') {
				ret.append(c);
			} else {
				ret.append("-");
			}
		}
		return ret.toString();
	}


	public static String encrypt(final String text) {
		return Base64.encodeBase64String(xor(text.getBytes()));
	}

	public static String decrypt(final String hash) {
		return new String(xor(Base64.decodeBase64(hash.getBytes())), StandardCharsets.UTF_8);
	}

	private static byte[] xor(final byte[] input) {
		final byte[] output = new byte[input.length];
		final byte[] secret = XOR_KEY.getBytes();
		int spos = 0;
		for (int pos = 0; pos < input.length; ++pos) {
			output[pos] = (byte) (input[pos] ^ secret[spos]);
			spos += 1;
			if (spos >= secret.length) {
				spos = 0;
			}
		}
		return output;
	}

	public static String normalizeFileName(String name) {
		String ret =  azCharAndDigitsPointsDash(name.toLowerCase(),"-");
		ret = ret.replaceAll("[-]+", "-");
		ret = StringUtils.removeStart(ret, '-');
		ret = StringUtils.removeEnd(ret, "-");
		return ret;
	}

	public static String brandName(String name) {
		return (StringUtils.normalizeSpace(stripAccents(name))).toUpperCase();
	}




	public static float[] to512(float[] embedding) {
	    if (embedding.length > TARGET_DIMS) {
	        throw new IllegalArgumentException(
	            "Embedding has more than 512 dims: " + embedding.length
	        );
	    }

	    if (embedding.length == TARGET_DIMS) {
	        return embedding;
	    }

	    float[] padded = new float[TARGET_DIMS];
	    System.arraycopy(embedding, 0, padded, 0, embedding.length);
	    // remaining values default to 0.0f
	    return padded;
	}

	/**
	 * Checks if a string represents a pure, parsable double.
	 * Avoids triggering expensive NumberFormatException when parsing fails.
	 *
	 * @param s the string to check
	 * @return true if parsable as a double, false otherwise
	 */
	public static boolean isPureDouble(final String s) {
		if (s == null) {
			return false;
		}
		final int len = s.length();
		if (len == 0) {
			return false;
		}
		int start = 0;
		final char first = s.charAt(0);
		if (first == '-' || first == '+') {
			start = 1;
			if (len == 1) {
				return false;
			}
		}
		boolean hasDot = false;
		boolean hasExponent = false;
		for (int i = start; i < len; i++) {
			final char c = s.charAt(i);
			if (c >= '0' && c <= '9') {
				continue;
			} else if (c == '.') {
				if (hasDot || hasExponent) {
					return false;
				}
				hasDot = true;
			} else if (c == 'e' || c == 'E') {
				if (hasExponent || i == len - 1) {
					return false;
				}
				hasExponent = true;
				final char next = s.charAt(i + 1);
				if (next == '-' || next == '+') {
					i++;
				}
			} else {
				return false;
			}
		}
		return true;
	}

}
