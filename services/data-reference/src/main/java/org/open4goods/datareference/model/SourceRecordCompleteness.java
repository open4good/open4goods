package org.open4goods.datareference.model;

/**
 * Whether a head states everything the source knows, or only named coordinates.
 */
public enum SourceRecordCompleteness {
    /** The head replaces every prior assertion for the record. */
    FULL,
    /** The head changes only the coordinates it names; others survive. */
    PARTIAL
}
