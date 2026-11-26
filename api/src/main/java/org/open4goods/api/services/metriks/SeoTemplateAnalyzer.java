package org.open4goods.api.services.metriks;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure computation over Search Console {@code date x page} rows: per-template daily series, a site KPI that leaves
 * out the "taille de TV" blog post (56 % of the impressions, GOU-2703) and the per-template loss alert.
 * <p>
 * Alert rule: a template alerts when its impressions over the last {@link #RECENT_DAYS} days fall below
 * {@code 1 - }{@link #LOSS_THRESHOLD} of its baseline, the average of the {@link #BASELINE_DAYS} days that
 * precede that period scaled to {@link #RECENT_DAYS} days (sum / 4). The baseline excludes the recent days so
 * that an ongoing drop does not dilute its own reference. A template without baseline impressions never alerts
 * (absence is not zero).
 */
public final class SeoTemplateAnalyzer {

    /** Length of the recent period compared with the baseline. */
    public static final int RECENT_DAYS = 7;
    /** Length of the baseline period preceding the recent one. */
    public static final int BASELINE_DAYS = 28;
    /** Loss above which a template alerts: more than 50 % of the baseline impressions gone. */
    public static final double LOSS_THRESHOLD = 0.5;
    /** Pages left out of the site KPI: the "quelle taille de TV" post, 56 % of the impressions (GOU-2703). */
    public static final List<String> DEFAULT_EXCLUDED_PATHS = List.of("/blog/quelle-taille-de-tv-choisir-pour-son-salon");
    /** Days needed to evaluate the rule. */
    public static final int TOTAL_DAYS = RECENT_DAYS + BASELINE_DAYS;

    private SeoTemplateAnalyzer() {
    }

    /** One Search Analytics row on the {@code date, page} dimensions. */
    public record DatedPageRow(LocalDate date, String page, long clicks, long impressions) {
    }

    /** Clicks and impressions of one template on one day. */
    public record DayStat(LocalDate date, long clicks, long impressions) {
    }

    /**
     * Evaluation of one template.
     *
     * @param recentImpressions   impressions over the last {@value #RECENT_DAYS} days
     * @param baselineImpressions baseline impressions scaled to {@value #RECENT_DAYS} days
     * @param lossRatio           {@code 1 - recent / baseline}, {@code null} without baseline
     * @param alert               true when the loss exceeds the threshold
     */
    public record TemplateStat(SeoTemplate template, long recentClicks, long recentImpressions,
                               double baselineImpressions, double baselineClicks, Double lossRatio, boolean alert,
                               List<DayStat> daily) {
    }

    /**
     * Result of the analysis.
     *
     * @param siteClicks28d      clicks of the last 28 days, excluded pages left out
     * @param siteImpressions28d impressions of the last 28 days, excluded pages left out
     * @param excludedImpressions28d impressions of the excluded pages over the same 28 days
     */
    public record Report(LocalDate start, LocalDate end, List<TemplateStat> templates, long siteClicks28d,
                         long siteImpressions28d, long excludedImpressions28d) {
        /** @return the templates currently in alert */
        public List<TemplateStat> alerts() {
            return templates.stream().filter(TemplateStat::alert).toList();
        }
    }

    /**
     * Analyses the rows over the {@value #TOTAL_DAYS} days ending on {@code end}.
     *
     * @param rows          Search Analytics rows (any order, days outside the window are ignored)
     * @param end           last day of the window (inclusive)
     * @param excludedPaths lower-case URL paths left out of the site KPI (e.g. the TV size post)
     */
    public static Report analyse(List<DatedPageRow> rows, LocalDate end, List<String> excludedPaths) {
        LocalDate start = end.minusDays(TOTAL_DAYS - 1L);
        LocalDate recentStart = end.minusDays(RECENT_DAYS - 1L);
        LocalDate kpiStart = end.minusDays(27L);

        Map<SeoTemplate, Map<LocalDate, long[]>> perTemplate = new EnumMap<>(SeoTemplate.class);
        long siteClicks = 0;
        long siteImpressions = 0;
        long excludedImpressions = 0;
        for (DatedPageRow row : rows) {
            if (row.date().isBefore(start) || row.date().isAfter(end)) {
                continue;
            }
            SeoTemplate template = SeoTemplate.classify(row.page());
            long[] cell = perTemplate.computeIfAbsent(template, t -> new TreeMap<>())
                    .computeIfAbsent(row.date(), d -> new long[2]);
            cell[0] += row.clicks();
            cell[1] += row.impressions();
            if (!row.date().isBefore(kpiStart)) {
                if (excludedPaths.contains(SeoTemplate.pathOf(row.page()))) {
                    excludedImpressions += row.impressions();
                } else {
                    siteClicks += row.clicks();
                    siteImpressions += row.impressions();
                }
            }
        }

        List<TemplateStat> stats = new ArrayList<>();
        for (SeoTemplate template : SeoTemplate.values()) {
            Map<LocalDate, long[]> days = perTemplate.getOrDefault(template, Map.of());
            List<DayStat> daily = new ArrayList<>();
            long recentClicks = 0;
            long recentImpressions = 0;
            long baseClicks = 0;
            long baseImpressions = 0;
            for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1L)) {
                long[] cell = days.getOrDefault(day, new long[2]);
                daily.add(new DayStat(day, cell[0], cell[1]));
                if (day.isBefore(recentStart)) {
                    baseClicks += cell[0];
                    baseImpressions += cell[1];
                } else {
                    recentClicks += cell[0];
                    recentImpressions += cell[1];
                }
            }
            double scale = (double) RECENT_DAYS / BASELINE_DAYS;
            double baselineImpressions = baseImpressions * scale;
            Double loss = baselineImpressions > 0 ? 1.0 - recentImpressions / baselineImpressions : null;
            boolean alert = loss != null && loss > LOSS_THRESHOLD;
            stats.add(new TemplateStat(template, recentClicks, recentImpressions, baselineImpressions,
                    baseClicks * scale, loss, alert, daily));
        }
        return new Report(start, end, stats, siteClicks, siteImpressions, excludedImpressions);
    }
}
