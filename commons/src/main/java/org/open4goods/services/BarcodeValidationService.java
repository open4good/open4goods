package org.open4goods.services;


import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;
import org.open4goods.model.BarcodeType;
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
			} 
		}

		// Unknown type
		return new AbstractMap.SimpleEntry<BarcodeType, String>(BarcodeType.UNKNOWN, barcode);			
		
		
	}



}