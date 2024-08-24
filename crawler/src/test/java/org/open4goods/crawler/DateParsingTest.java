package org.open4goods.crawler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.crawler.extractors.Extractor;

public class DateParsingTest {

	@Test
	public void test() {

		try {
			final Long result = Extractor.parseDate("2018-02-25T19:45:22.000+00:00", "yyyy-MM-dd'T'HH:mm:ss.SSSz", null,
					null, Locale.FRANCE);
			assertNotNull(result);
		} catch (final ValidationException e) {
			fail(e.getMessage());
		}

		try {
			final Long result = Extractor.parseDate("21/06/2018", "dd/MM/yyyy", null, null, Locale.FRANCE);
			assertNotNull(result);
		} catch (final ValidationException e) {
			fail(e.getMessage());
		}

		try {
			final Long result = Extractor.parseDate("04 avr. 2018", "dd MMM yyyy", null, null, Locale.FRANCE);
			assertNotNull(result);
		} catch (final ValidationException e) {
			fail(e.getMessage());
		}

		try {
			final Long result = Extractor.parseDate("04 avr. 2018", "dd MMM yyyy", null, null, Locale.FRANCE);
			assertNotNull(result);
		} catch (final ValidationException e) {
			fail(e.getMessage());
		}

		try {
			final Long result = Extractor.parseDate("25 janvier 2019", "d MMMM yyyy", null, null, Locale.FRANCE);
			assertNotNull(result);
		} catch (final ValidationException e) {
			fail(e.getMessage());
		}

		try {
			final Long result = Extractor.parseDate("4 février 2017", "d MMMM yyyy", null, null, Locale.FRANCE);
			assertNotNull(result);
		} catch (final ValidationException e) {
			fail(e.getMessage());
		}

	}

}
