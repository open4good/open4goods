package org.open4goods.services.feedservice.definition;

/**
 * Completion contract a feed version declares for its records.
 *
 * <p>{@code FULL} asserts the entire source population: records absent from a
 * successfully completed full feed are missing and must be deleted explicitly.
 * {@code PARTIAL} asserts only the records it names; absence says nothing about
 * a record's continued existence. {@code DELETION} asserts that the named
 * records must be removed.
 */
public enum FeedSemantics {
    FULL,
    PARTIAL,
    DELETION
}
