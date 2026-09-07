package org.open4goods.commons.services;


import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.open4goods.model.product.BarcodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is in charge of testing barcodes, in order to detect validity and type
 * @author Goulven.Furet
 *
 */
public class BarcodeValidationService {

	private static final Logger logger = LoggerFactory.getLogger(BarcodeValidationService.class);


	// Testing for ISBN
	public  SimpleEntry<BarcodeType, String> sanitize(String barcode) {


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