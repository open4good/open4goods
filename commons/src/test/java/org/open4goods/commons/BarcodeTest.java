package org.open4goods.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.model.product.BarcodeType;

public class BarcodeTest {

	@Test
	public void test() {
		BarcodeValidationService service = new BarcodeValidationService();

		assertEquals(BarcodeType.UNKNOWN, service.sanitize("5020570230324").getKey());


		// Testing ean13
		assertEquals(BarcodeType.GTIN_13, service.sanitize("8436542858045").getKey());
		assertEquals(BarcodeType.GTIN_13, service.sanitize("3462117246967").getKey());
		
		assertEquals(BarcodeType.ISBN_13, service.sanitize("9781845924539").getKey());

		
		

		// Testing ean8
		assertEquals(BarcodeType.GTIN_13, service.sanitize("40170725").getKey());




		// Gtin12
		assertEquals(BarcodeType.GTIN_13, service.sanitize("123601057072").getKey());


		// Gtin 14
		assertEquals(BarcodeType.GTIN_14, service.sanitize("70753800008156").getKey());


		// Testing ISBN 13
		assertEquals(BarcodeType.ISBN_13, service.sanitize("9782298135039").getKey());
		assertEquals(BarcodeType.ISBN_13, service.sanitize("978-614-404-018-8").getKey());


		// Testing ISBN 10 (converted to isbn13)
		assertEquals(BarcodeType.ISBN_13, service.sanitize("0198526636").getKey());


		assertEquals(BarcodeType.ISBN_13, service.sanitize("9780786296323").getKey());

	
		// Testing invalid
		assertEquals(BarcodeType.UNKNOWN, service.sanitize("8436542859045").getKey());

		
		// Testing the padded resolution
		assertEquals(BarcodeType.UNKNOWN, service.sanitize("000	88381694858").getKey());
		assertEquals(BarcodeType.GTIN_13, service.sanitize("88381694858").getKey());
		assertEquals(BarcodeType.GTIN_13, service.sanitize("088381694858").getKey());
		assertEquals(BarcodeType.GTIN_13, service.sanitize("0088381694858").getKey());
		// 14-digit barcodes are always GTIN_14 (packaging indicator in first digit)
		assertEquals(BarcodeType.GTIN_14, service.sanitize("00088381694858").getKey());

		
		// Alimentary : 
		assertEquals(BarcodeType.GTIN_13, service.sanitize("11225416712").getKey());
		assertEquals(BarcodeType.GTIN_13, service.sanitize("790310044").getKey());
		

	}

	/**
	 * GOU-1841: every form of a code gives the id the product is indexed under.
	 */
	@Test
	public void toProductIdGivesTheSameIdForEveryFormOfACode() {
		BarcodeValidationService service = new BarcodeValidationService();

		// ISBN-10 and its ISBN-13
		assertEquals(9782726862216L, service.toProductId("2726862217"));
		assertEquals(9782726862216L, service.toProductId("9782726862216"));
		assertEquals(9781609202415L, service.toProductId("1609202414"));
		assertEquals(9781609202415L, service.toProductId("9781609202415"));

		// UPC-A, EAN-13 and GTIN-14 with leading zeros
		assertEquals(36000291452L, service.toProductId("036000291452"));
		assertEquals(36000291452L, service.toProductId("0036000291452"));
		assertEquals(36000291452L, service.toProductId("00036000291452"));
		assertEquals(36000291452L, service.toProductId("36000291452"));
		assertEquals(7333394145372L, service.toProductId("07333394145372"));

		// EAN-8
		assertEquals(40170725L, service.toProductId("40170725"));

		// Invalid
		assertNull(service.toProductId("7333394145373"));
		assertNull(service.toProductId(null));
	}
	//




}
