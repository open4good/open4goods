package org.open4goods.nudgerfrontapi.service.opendata;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Manifests in the format written by the api ({@code OpenDataService}, {@code docs/architecture/opendata.md}) and a
 * clock that tests move forward.
 */
final class ManifestFixtures {

    static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    static final String GTIN_SHA256 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
    static final long GTIN_ROWS = 115_786_654L;
    static final long GTIN_BYTES = 6_400_000_000L;
    static final long ISBN_ROWS = 8_703_397L;
    static final long ISBN_BYTES = 210_000_000L;

    private ManifestFixtures() {
    }

    /**
     * @return a manifest with the GTIN, ISBN and one impact dataset, plus fields unknown to front-api
     */
    static String manifest(String gtinGeneratedAt, String isbnGeneratedAt) {
        return """
                {
                  "schemaVersion": 1,
                  "generatedAt": "%s",
                  "license": "ODbL-1.0",
                  "fieldAddedLater": {"compatible": true},
                  "datasets": [
                    {
                      "schemaVersion": 1, "id": "gtin", "file": "gtin-open-data.zip",
                      "url": "https://data.nudger.fr/gtin-open-data.zip", "format": "text/csv+zip",
                      "license": "ODbL-1.0", "bytes": %d, "sha256": "%s", "rows": %d, "indexCount": 115790000,
                      "generatedAt": "%s", "columns": [ { "name": "gtin", "fillRate": 1.0 } ],
                      "anotherNewField": 42
                    },
                    {
                      "schemaVersion": 1, "id": "isbn", "file": "isbn-open-data.zip", "format": "text/csv+zip",
                      "license": "ODbL-1.0", "bytes": %d, "sha256": "abc", "rows": %d, "indexCount": 8800000,
                      "generatedAt": "%s", "columns": [ { "name": "isbn", "fillRate": 1.0 } ]
                    },
                    {
                      "schemaVersion": 1, "id": "impact-tv", "file": "impact-tv.zip", "format": "text/csv+zip",
                      "license": "ODbL-1.0", "bytes": 1000000, "sha256": "def", "rows": 5000, "indexCount": 5000,
                      "generatedAt": "%s",
                      "columns": [ { "name": "gtin", "fillRate": 1.0, "unit": "", "description": "GTIN", "source": "x" } ]
                    }
                  ]
                }
                """.formatted(gtinGeneratedAt, GTIN_BYTES, GTIN_SHA256, GTIN_ROWS, gtinGeneratedAt, ISBN_BYTES,
                ISBN_ROWS, isbnGeneratedAt, gtinGeneratedAt);
    }

    /**
     * @return a manifest whose datasets were all generated {@code age} before {@link #NOW}
     */
    static String manifestAged(Duration age) {
        String at = NOW.minus(age).toString();
        return manifest(at, at);
    }

    /**
     * Clock moved forward by the tests.
     */
    static final class MutableClock extends Clock {

        private Instant instant = NOW;

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
