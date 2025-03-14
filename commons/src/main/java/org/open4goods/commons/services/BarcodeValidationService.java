package org.open4goods.commons.services;


import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;
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


		// EAN13
		if (EAN13CheckDigit.EAN13_CHECK_DIGIT.isValid(barcode)) {

			if (barcode.length() == 8 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_8, barcode);
			} else if (barcode.length() == 13 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_13, barcode);
			} else if (barcode.length() == 12 ) {
				return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_12, barcode);
			}  else if (barcode.length() == 14 ) {
				
				if (barcode.startsWith("0")) {
					return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_13, barcode.substring(1));					
				} else {
					return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.GTIN_14, barcode);
				}
			}
		}

		// Unknown type
		return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.UNKNOWN, barcode);
	}



}