package org.open4goods.model.product;

/**
 * Nature of a product that is not an ordinary product, read from its merchant
 * categories (GOU-2233).
 *
 * <p>An ordinary product has no nature: {@code Product.nature} stays absent.
 * A product with a vertical never has one either (EPREL can keep a real
 * appliance in its vertical while a marketplace categorises it as an accessory).
 * When both apply, {@link #SPARE_PART} wins over {@link #ACCESSORY}.
 */
public enum ProductNature {

	/** Accessory or consumable (cover, cleaning product...). */
	ACCESSORY,

	/** Spare part of an appliance. */
	SPARE_PART
}
