package org.open4goods.services.opendata.model;

import java.util.List;

/**
 * Public manifest listing every dataset ({@code https://data.nudger.fr/manifest.json}, contract of the GOU-2237 plan).
 * It is written after the zips and the per-dataset manifests: a pass interrupted before it leaves the previous
 * manifest consistent with the previous files. Adding a field is compatible; removing a field or changing its meaning
 * increments {@link #SCHEMA_VERSION}.
 *
 * @param schemaVersion version of this contract
 * @param generatedAt   end of the generation pass, ISO 8601 UTC
 * @param license       SPDX identifier of the licence of the data
 * @param datasets      one entry per dataset
 */
public record OpenDataManifest(int schemaVersion, String generatedAt, String license, List<DatasetManifest> datasets) {

    /** Version of the manifest contract. */
    public static final int SCHEMA_VERSION = 1;

    /** Licence of the datasets. */
    public static final String LICENSE = "ODbL-1.0";
}
