package org.open4goods.services.feedservice.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Program returned by the Effinity (ex-Effiliation) API v3 Programs API
 * ({@code GET /apiv3/{apikey}/publisher/programs}, schema {@code ProgramLightRestDto}).
 *
 * <p>Only the fields used by the feed service are bound. The hypermedia {@code links} of the payload are
 * deliberately left out: their URLs carry the API key.</p>
 *
 * @param merchantId merchant (advertiser) identifier, the {@code id_affilieur} of the API v2
 * @param programId program identifier
 * @param merchantName merchant site name
 * @param merchantUrl merchant website URL
 * @param merchantDescription merchant description
 * @param programName program name
 * @param programLogoUrl program logo URL
 * @param status program status from the publisher point of view (active, inactive, pending...)
 * @param state program state: active, frozen, closing or closed
 * @param defaultTrackedLink default tracked link of the program
 * @param programCategories program categories
 * @param coverageCountries countries covered by the program
 * @param cookieDurationPostClick post-click cookie duration, for example {@code "30 days"}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EffiliationProgram(
        Long merchantId,
        Long programId,
        String merchantName,
        String merchantUrl,
        String merchantDescription,
        String programName,
        String programLogoUrl,
        String status,
        String state,
        String defaultTrackedLink,
        List<String> programCategories,
        List<String> coverageCountries,
        String cookieDurationPostClick) {
}
