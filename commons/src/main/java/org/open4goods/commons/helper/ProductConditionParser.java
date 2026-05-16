package org.open4goods.commons.helper;

import java.text.Normalizer;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.product.ProductCondition;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * Parses merchant-provided product condition labels into the internal
 * {@link ProductCondition} enum.
 */
public class ProductConditionParser {

	private static final Logger logger = GenericFileLogger.initLogger("product-condition-parser", Level.INFO, "/opt/open4goods/logs/");

	/**
	 * Parses a raw merchant condition.
	 *
	 * @param val raw condition label
	 * @return parsed product condition
	 * @throws InvalidParameterException when the value is empty or unknown
	 */
	public static ProductCondition parse(String val) throws InvalidParameterException {

		if (StringUtils.isEmpty(val)) {
			throw new InvalidParameterException("Cannot evaluate null or empty ProductCondition");
		}
			val = normalize(val);
			return switch (val) {
			    case "NEUF", "NEW", "PRODUIT NEUF", "NOUVEAU", "PRODUIT NEW", "HTTP SCHEMA ORG NEWCONDITION",
						 "COMME NEUF", "ETAT NEUF" -> ProductCondition.NEW;
			    case "PRODUIT RECONDITIONNE", "RECONDITIONNE", "DEPACKAGED", "USED", "OCCASION", "REFURBUSHED",
						 "REFURBISHED", "VERY GOOD", "VERYGOOD", "VERY BON", "TRES BON", "TRES BON ETAT", "BON",
						 "GOOD", "BON ETAT", "ETAT CORRECT", "CORRECT", "ACCEPTABLE", "FAIR", "EXCELLENT",
						 "EXCELLENT ETAT", "COLLECTION", "SECOND HAND", "PRE OWNED", "PREOWNED" -> ProductCondition.OCCASION;
			    default -> {
                    if (isKnownNonConditionValue(val)) {
                        logger.debug("Ignoring non-condition ProductCondition value : {}", val);
                    } else {
                        logger.warn("Unknown ProductCondition value : {}", val);
                    }
			    	throw new InvalidParameterException("Cannot parse ProductCondition value : " + val);
			    }
			};

	}

	/**
	 * Normalizes separators, accents and marketplace enum spellings.
	 *
	 * @param value raw condition label
	 * @return normalized uppercase value
	 */
	private static String normalize(String value) {
		String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKD)
				.replaceAll("\\p{M}", "")
				.replace('_', ' ')
				.replace('-', ' ')
				.replace('/', ' ')
				.replace(':', ' ')
				.replace(".", " ")
				.toUpperCase(Locale.ROOT);
		return normalized.replaceAll("\\s+", " ").trim();
	}

	/**
	 * Detects values that often arrive in the condition field but are clearly from
	 * another feed column.
	 *
	 * @param value normalized condition candidate
	 * @return {@code true} when the value should not be logged as an unknown label
	 */
	private static boolean isKnownNonConditionValue(String value) {
		return value.matches("\\d+(?:[., ]\\d+)*") || switch (value) {
			case "NON", "NO", "FALSE", "N", "OUI", "YES", "TRUE", "Y", "EUR", "EURO", "NULL", "N A", "NA", "NC" -> true;
			default -> false;
		};
	}
}
