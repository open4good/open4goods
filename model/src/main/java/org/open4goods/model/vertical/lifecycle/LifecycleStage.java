package org.open4goods.model.vertical.lifecycle;

/**
 * Lifecycle stages used to describe where an attribute contributes within an
 * assessment (ACV / lifecycle analysis).
 */
public enum LifecycleStage {

    /** Raw material extraction and primary processing. */
    EXTRACTION,

    /** Manufacturing and assembly of the product. */
    MANUFACTURING,

    /** Transportation and distribution stages. */
    TRANSPORTATION,

    /** Use phase during the product lifetime. */
    USE,

    /** End-of-life processing including recycling or disposal. */
    END_OF_LIFE
}
