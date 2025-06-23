package org.open4goods.nudgerfrontapi.dto;

/**
 * Request object for rendering a product view.
 *
 * @param gtin the product GTIN
 */
public record ProductViewRequest(long gtin) {
}
