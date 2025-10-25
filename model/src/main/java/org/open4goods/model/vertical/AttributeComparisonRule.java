package org.open4goods.model.vertical;

/**
 * Comparison rule describing which direction makes an attribute value more
 * desirable when comparing two products.
 */
public enum AttributeComparisonRule {

    /**
     * Higher values represent a better attribute (e.g. screen size, score).
     */
    GREATER,

    /**
     * Lower values represent a better attribute (e.g. weight, consumption).
     */
    LOWER
}
