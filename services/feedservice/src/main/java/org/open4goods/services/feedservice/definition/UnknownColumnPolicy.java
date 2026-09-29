package org.open4goods.services.feedservice.definition;

/**
 * Policy applied to a source column with no configured mapping.
 *
 * <p>{@code REPORT} is the only member on purpose: an unknown column is always
 * surfaced for review and never silently guessed from its label.
 */
public enum UnknownColumnPolicy {
    REPORT
}
