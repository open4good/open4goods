package org.open4goods.nudgerfrontapi.service.css;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of a CSS dry run: per vertical, products examined, eligible, and exclusions by reason (GOU-2080).
 *
 * @param generatedAt ISO-8601 date of the run
 * @param verticals   report of each vertical, by vertical id
 */
public record CssDryRunReport(String generatedAt, Map<String, VerticalReport> verticals) {

    /**
     * @param examined   products examined
     * @param eligible   products eligible (one NDJSON line each)
     * @param exclusions products excluded, by reason; a product failing several rules counts once per reason
     */
    public record VerticalReport(long examined, long eligible, Map<CssExclusionReason, Long> exclusions) {
    }

    /**
     * @return total of products examined
     */
    public long examined() {
        return verticals.values().stream().mapToLong(VerticalReport::examined).sum();
    }

    /**
     * @return total of eligible products
     */
    public long eligible() {
        return verticals.values().stream().mapToLong(VerticalReport::eligible).sum();
    }

    /**
     * @return the report as Markdown (one table row per vertical, then the exclusion counts)
     */
    public String toMarkdown() {
        StringBuilder md = new StringBuilder("# CSS dry run (").append(generatedAt).append(")\n\n")
                .append("Eligible: **").append(eligible()).append("** / ").append(examined()).append(" examined\n\n")
                .append("| Vertical | Examined | Eligible | Exclusions |\n|---|---:|---:|---|\n");
        verticals.forEach((id, v) -> md.append("| ").append(id).append(" | ").append(v.examined()).append(" | ")
                .append(v.eligible()).append(" | ")
                .append(v.exclusions().entrySet().stream().map(e -> e.getKey() + ": " + e.getValue())
                        .collect(Collectors.joining(", ")))
                .append(" |\n"));
        return md.toString();
    }
}
