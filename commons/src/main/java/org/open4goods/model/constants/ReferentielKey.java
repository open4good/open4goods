package org.open4goods.model.constants;

public enum ReferentielKey {
	BRAND,
	GTIN,
	MODEL
	;

	/**
	 *
	 * @param attrToCheck
	 * @return true if this attr name is a valid ReferentielKey
	 */
	public static boolean isValid(final String attrToCheck) {
		try {
			return null != valueOf(attrToCheck);
		} catch (final Exception e) {
			return false;
		}
	}


}
