package org.open4goods.eprelservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of an EPREL product group entry.
 *
 * @param code      internal code provided by EPREL
 * @param urlCode   URL friendly code used for catalogue downloads
 * @param name      human readable name of the group
 * @param regulation regulation reference associated with the group
 */
public record EprelProductGroup(
        @JsonProperty("code") String code,
        @JsonProperty("url_code") String urlCode,
        @JsonProperty("name") String name,
        @JsonProperty("regulation") String regulation)
{
}
