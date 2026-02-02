package org.open4goods.services.geocode.model;

/**
 * Represents a geocode match result with its match type.
 *
 * @param record resolved city record
 * @param matchType match type used for lookup
 */
public record CityMatch(CityRecord record, MatchType matchType)
{
}
