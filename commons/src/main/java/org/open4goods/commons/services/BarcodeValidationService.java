package org.open4goods.commons.services;


import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Gs1Class;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is in charge of testing barcodes, in order to detect validity and type
 * @author Goulven.Furet
 *
 */
public class BarcodeValidationService {

	private static final Logger logger = LoggerFactory.getLogger(BarcodeValidationService.class);

	/** Minimum count of significant digits (leading zeros removed) of a code, see GOU-2293 measure. */
	public static final int MIN_SIGNIFICANT_DIGITS = 8;

	/** Well-known placeholder codes, compared without their leading zeros. */
	public static final Set<String> DEFAULT_FAKE_CODES = Set.of("1234567890128", "123456789012");

	/**
	 * Two-level verdict of a barcode.
	 */
	public enum Verdict {
		/** Wrong check digit, degenerate or fake code: rejected everywhere. */
		INVALID,
		/** Valid code of a restricted class (in-store, coupon): never creates a product. */
		RESTRICTED,
		/** Valid code, usable as a product key. */
		ACCEPTED
	}

	private final Set<String> fakeCodes;

	/** Builds a service with {@link #DEFAULT_FAKE_CODES}. */
	public BarcodeValidationService() {
		this(DEFAULT_FAKE_CODES);
	}

	/**
	 * @param fakeCodes known placeholder codes, rejected whatever their leading zeros
	 */
	public BarcodeValidationService(Collection<String> fakeCodes) {
		this.fakeCodes = new HashSet<>();
		for (String code : fakeCodes) {
			this.fakeCodes.add(StringUtils.stripStart(code, "0"));
		}
	}

	/**
	 * Sanitises a barcode: rejects degenerate codes, then pads to 13 digits and checks it
	 * as ISBN or GTIN, falling back to the original form (ISBN-10).
	 *
	 * @param barcode raw barcode
	 * @return the type and normalised value; {@link BarcodeType#UNKNOWN} when the check digit is wrong
	 *         or when the code is {@link #isDegenerate(String) degenerate}
	 */
	public  SimpleEntry<BarcodeType, String> sanitize(String barcode) {

		if (isDegenerate(barcode)) {
			return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.UNKNOWN, barcode);
		}

		// Making it a 13 digits long, to enforce EAN13. This allow to avoid duplicates between GTIN 13 / 12 duplicates
		String paddedBarcode = StringUtils.leftPad(barcode,13,'0');
		
		// But if we fail, trying a classical resolution (for EAN 10 for example)
		SimpleEntry<BarcodeType, String> ret = test(paddedBarcode);
		
		
		if (null == ret || ret.getKey().equals(BarcodeType.UNKNOWN)) {
			// Trying with the original barcode
			ret = test(barcode);
		}
		
		return ret;


	}

	/**
	 * Tells whether a code is degenerate, whatever its check digit: fewer than
	 * {@value #MIN_SIGNIFICANT_DIGITS} significant digits once leading zeros are removed
	 * (the padding to 13 digits would otherwise hide them), a single repeated digit, or a
	 * known fake code.
	 *
	 * @param barcode raw barcode, may be null
	 * @return {@code true} when the code cannot be a real product code
	 */
	public boolean isDegenerate(String barcode) {
		if (barcode == null) {
			return true;
		}
		String significant = StringUtils.stripStart(barcode.replaceAll("[^0-9]", ""), "0");
		if (significant.length() < MIN_SIGNIFICANT_DIGITS) {
			return true;
		}
		return significant.chars().distinct().count() == 1 || fakeCodes.contains(significant);
	}

	/**
	 * Classifies a normalised barcode by GS1 prefix arithmetic.
	 *
	 * @param normalized sanitised barcode, as returned by {@link #sanitize(String)}
	 * @return the class, {@link Gs1Class#UNKNOWN} when the code has no 3-digit prefix
	 */
	public Gs1Class classifyGs1Class(String normalized) {
		if (normalized == null || normalized.length() < 3) {
			return Gs1Class.UNKNOWN;
		}
		String gs1Prefix = normalized.substring(0, 3);

		// ISSN periodicals: prefix 977
		if ("977".equals(gs1Prefix)) {
			return Gs1Class.ISSN_PERIODICAL;
		}
		// ISBN / ISMN: prefixes 978 and 979
		if ("978".equals(gs1Prefix)) {
			return Gs1Class.ISBN_BOOKLAND;
		}
		if ("979".equals(gs1Prefix)) {
			// 979-0 is ISMN (music); everything else in 979 is ISBN
			if (normalized.length() >= 4 && normalized.charAt(3) == '0') {
				return Gs1Class.ISMN_MUSIC;
			}
			return Gs1Class.ISBN_BOOKLAND;
		}
		try {
			int prefixInt = Integer.parseInt(gs1Prefix);
			// Coupon: prefixes 980-999
			if (prefixInt >= 980) {
				return Gs1Class.COUPON;
			}
			// Restricted / in-store / variable-weight
			if ((prefixInt >= 20 && prefixInt <= 29)
					|| (prefixInt >= 40 && prefixInt <= 49)
					|| (prefixInt >= 200 && prefixInt <= 299)) {
				return Gs1Class.RESTRICTED_INTERNAL;
			}
		} catch (NumberFormatException e) {
			return Gs1Class.UNKNOWN;
		}
		return Gs1Class.GTIN;
	}

	/**
	 * Two-level verdict of a raw barcode.
	 *
	 * @param barcode raw barcode
	 * @return {@link Verdict#INVALID} when {@link #sanitize(String)} rejects it,
	 *         {@link Verdict#RESTRICTED} for {@link Gs1Class#RESTRICTED_INTERNAL} and
	 *         {@link Gs1Class#COUPON}, {@link Verdict#ACCEPTED} otherwise
	 */
	public Verdict verdict(String barcode) {
		SimpleEntry<BarcodeType, String> sanitized = sanitize(barcode);
		if (sanitized == null || BarcodeType.UNKNOWN.equals(sanitized.getKey())) {
			return Verdict.INVALID;
		}
		Gs1Class gs1Class = classifyGs1Class(sanitized.getValue());
		if (Gs1Class.RESTRICTED_INTERNAL.equals(gs1Class) || Gs1Class.COUPON.equals(gs1Class)) {
			return Verdict.RESTRICTED;
		}
		return Verdict.ACCEPTED;
	}

	/**
	 * Returns the numeric product id of a barcode, i.e. {@code Long.valueOf} of its
	 * {@link #sanitize(String) sanitised} form. It is the single key used both to look
	 * up an existing product and to index it, so that every form of the same code
	 * designates the same document: an ISBN-10 and its ISBN-13 ({@code 2726862217} and
	 * {@code 9782726862216}), a UPC-A, EAN-13 or GTIN-14 with or without leading zeros
	 * ({@code 07333394145372} and {@code 7333394145372}).
	 *
	 * @param barcode raw barcode, as published by a datasource
	 * @return the product id, or {@code null} when the barcode is not a valid GTIN or ISBN
	 */
	public Long toProductId(String barcode) {
		SimpleEntry<BarcodeType, String> sanitized = sanitize(barcode);
		if (sanitized == null || BarcodeType.UNKNOWN.equals(sanitized.getKey()) || !StringUtils.isNumeric(sanitized.getValue())) {
			return null;
		}
		try {
			return Long.valueOf(sanitized.getValue());
		} catch (NumberFormatException e) {
			logger.debug("Sanitised barcode {} of {} is not a product id", sanitized.getValue(), barcode);
			return null;
		}
	}


	private SimpleEntry<BarcodeType, String> test(String barcode) {
		//		String barcode = Long.valueOf(code).toString();
		ISBNValidator isbnValidator = ISBNValidator.getInstance();

		// Checking ISBN 13
		String formatedbarCode = isbnValidator.validateISBN13(barcode);
		if (null != formatedbarCode) {
			return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.ISBN_13, formatedbarCode);
		}

		// Checking ISBN 10
		formatedbarCode = isbnValidator.validateISBN10(barcode);
		if (null != formatedbarCode) {
			// Converting to ISBN 13
			return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.ISBN_13, isbnValidator.convertToISBN13(formatedbarCode));
		}


		// GTIN (EAN/UPC family). The GS1 check-digit algorithm is the same weighted
		// modulus-10 sum regardless of length (8/12/13/14 digits), so it is computed
		// directly here rather than delegated to a length-specific validator class.
		if (isValidGtinCheckDigit(barcode)) {

			if (barcode.length() == 8 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_8, barcode);
			} else if (barcode.length() == 13 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_13, barcode);
			} else if (barcode.length() == 12 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_12, barcode);
			}  else if (barcode.length() == 14 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_14, barcode);
			}
		}

		// Unknown type
		return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.UNKNOWN, barcode);
	}

	/**
	 * Validates the GS1 check digit (used by EAN-8/12/13 and GTIN-14 barcodes).
	 * <p>
	 * Working from the digit immediately left of the check digit, weights alternate
	 * 3/1; this is independent of the total barcode length, unlike
	 * {@code org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit},
	 * which as of commons-validator 1.11.0 only accepts 13-digit input.
	 *
	 * @param barcode digits-only barcode, including its trailing check digit
	 * @return {@code true} when the trailing digit matches the computed check digit
	 */
	private static boolean isValidGtinCheckDigit(String barcode) {
		if (!StringUtils.isNumeric(barcode) || barcode.length() < 8) {
			return false;
		}

		int sum = 0;
		boolean weightThree = true;
		for (int i = barcode.length() - 2; i >= 0; i--) {
			sum += Character.digit(barcode.charAt(i), 10) * (weightThree ? 3 : 1);
			weightThree = !weightThree;
		}

		int checkDigit = Character.digit(barcode.charAt(barcode.length() - 1), 10);
		int computedCheckDigit = (10 - (sum % 10)) % 10;
		return computedCheckDigit == checkDigit;
	}

}