package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration describing the direction of a price evolution compared to the
 * previously recorded value.
 */
@Schema(description = "Direction of the price change compared to the previous recorded value.")
public enum PriceTrendState {

    /** Price decreased between the last two measurements. */
    PRICE_DECREASE,

    /** Price remained stable between the last two measurements. */
    PRICE_STABLE,

    /** Price increased between the last two measurements. */
    PRICE_INCREASE
}
