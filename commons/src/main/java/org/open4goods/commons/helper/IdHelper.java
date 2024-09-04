package org.open4goods.commons.helper;

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
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author goulven
 *
 */
public class IdHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdHelper.class);

	private static final Pattern brandUid = Pattern.compile("\\w*\\-?(\\d[A-Za-z])+|([A-Za-z]\\d)+\\-?\\w*");

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
	public static String sanitize(final String input) {
		if (StringUtils.isEmpty(input)) {
			return null;
		}
		final String ret = StringEscapeUtils.unescapeHtml4(input).trim();

		if (StringUtils.isEmpty(ret)) {
			return null;
		}

		return ret;
	}
	
	public static String sanitizeAndNormalize(final String input) {
		return StringUtils.normalizeSpace(StringEscapeUtils.unescapeHtml4(input));
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
			return StringUtils.stripAccents(string).trim().toUpperCase();
		}
	}

	/**
	 * Extract brandUid from product name
	 *
	 * @return
	 * @throws InvalidParameterException
	 */
	public static String extractModelFromNames(final DataFragment dataFragment) throws InvalidParameterException {
		final Set<String> brandUids = new HashSet<>();
		for (final String name : dataFragment.getNames()) {
			// TODO(conf,P2,0.25) : Use filter parameters from conf (see cm)
			final Set<String> extracted = extractBrandUids(name).stream().filter(e -> !e.endsWith("CM")).collect(Collectors.toSet());
			brandUids.addAll(extracted);
		}

		if (brandUids.size() == 1) {
			return IdHelper.sanitize(brandUids.iterator().next());

		} else if (brandUids.size() > 1) {
			throw new InvalidParameterException("Multiple brandUids extracted from names : "+StringUtils.join(dataFragment.getNames(), ", ")+" : " + StringUtils.join(brandUids, ", "));
		}
		return null;

	}

	/**
	 * Extract brandUid from a String
	 * TODO : rename extract brand -> extractModel

	 * @return
	 */

	public static Set<String> extractBrandUids(final String name) {
		final Set<String> brandUids = new HashSet<>();
		final String[] frags = name.split(" ");

		for (final String f : frags) {
			//TODO(conf,0.25,P3) : branduid minimum length here) : from conf
			if (f.length() > 3) {
				final boolean isVendorUid = brandUid.matcher(f).find();
				if (isVendorUid) {
					brandUids.add(f.toUpperCase());
					LOGGER.debug("Extracted MODEL  ({}) from ({}) ", f, name);
				}
			}
		}

		return brandUids;

	}

	public static String getCategoryName(final String name) {

		if (StringUtils.isBlank(name)) {
			return null;
		}

		String [] frags = name.split("\n");

		List<String> frs = Arrays.stream(frags).filter(e ->  ! StringUtils.isBlank(StringUtils.normalizeSpace( e))).collect(Collectors.toList());

		String ret = StringUtils.join(frs," > ");
		ret = StringUtils.stripAccents(ret);
		ret = ret.toUpperCase();
		ret = ret.replace("\n", " > ");
		ret = StringUtils.normalizeSpace(ret);
		return ret;

	}

	public static String azCharAndDigits(final String input) {
		return StringUtils.stripAccents(input).replaceAll("[^a-zA-Z0-9]", "");
	}
	
	public static String azCharAndDigits(final String input, String replacement) {
		return StringUtils.stripAccents(input).replaceAll("[^a-zA-Z0-9]", replacement);
	}
	
	public static String azCharAndDigitsPointsDash(String input) {
		return azCharAndDigits(input);
	}


	public static String azCharAndDigitsPointsDash(String input, String string) {
		return StringUtils.stripAccents(input).replaceAll("[^a-zA-Z0-9_-]",string);
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
		//		if (ret.length() > 3) {
		//			return ret.toString();
		//		} else {
		//			throw new InvalidParameterException("Cannot get a clean name for"+name+", it would be "+ret+" and is too short.");
		//		}
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
		//		if (ret.length() > 3) {
		//			return ret.toString();
		//		} else {
		//			throw new InvalidParameterException("Cannot get a clean name for"+name+", it would be "+ret+" and is too short.");
		//		}
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




}
