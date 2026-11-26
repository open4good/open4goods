package org.open4goods.api.services.metriks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.metriks.SeoTemplateAnalyzer.DatedPageRow;
import org.open4goods.api.services.metriks.SeoTemplateAnalyzer.Report;
import org.open4goods.api.services.metriks.SeoTemplateAnalyzer.TemplateStat;

class SeoTemplateAnalyzerTest {

    private static final String BLOG = "https://nudger.fr/blog/un-article";
    private static final String TV_POST = "https://nudger.fr/blog/quelle-taille-de-tv-choisir-pour-son-salon";
    private static final String FICHE = "https://nudger.fr/televiseurs/8806097084532-tv-samsung";

    private static TemplateStat stat(Report report, SeoTemplate template) {
        return report.templates().stream().filter(s -> s.template() == template).findFirst().orElseThrow();
    }

    /** Blog at 1000 impressions/day until the window's last 7 days, then nothing (the Sept 10 blog outage). */
    @Test
    void blogDisappearingForAWeekRaisesTheAlertAndNothingElseDoes() {
        LocalDate end = LocalDate.of(2026, 9, 24);
        List<DatedPageRow> rows = new ArrayList<>();
        for (LocalDate day = end.minusDays(34); !day.isAfter(end); day = day.plusDays(1)) {
            boolean outage = day.isAfter(end.minusDays(7));
            rows.add(new DatedPageRow(day, BLOG, outage ? 0 : 10, outage ? 20 : 1000));
            rows.add(new DatedPageRow(day, FICHE, 5, 200));
        }

        Report report = SeoTemplateAnalyzer.analyse(rows, end, SeoTemplateAnalyzer.DEFAULT_EXCLUDED_PATHS);

        assertThat(report.alerts()).extracting(TemplateStat::template).containsExactly(SeoTemplate.BLOG);
        TemplateStat blog = stat(report, SeoTemplate.BLOG);
        assertThat(blog.recentImpressions()).isEqualTo(140);
        assertThat(blog.baselineImpressions()).isEqualTo(7000.0);
        assertThat(blog.lossRatio()).isGreaterThan(0.97);
        assertThat(stat(report, SeoTemplate.FICHE).alert()).isFalse();
        assertThat(stat(report, SeoTemplate.BLOG).daily()).hasSize(35);
    }

    @Test
    void dropOfExactlyFiftyPercentDoesNotAlertButMoreDoes() {
        LocalDate end = LocalDate.of(2026, 9, 24);
        assertThat(alertForRecentDailyImpressions(end, 50)).isFalse();
        assertThat(alertForRecentDailyImpressions(end, 49)).isTrue();
    }

    private boolean alertForRecentDailyImpressions(LocalDate end, long recentPerDay) {
        List<DatedPageRow> rows = new ArrayList<>();
        for (LocalDate day = end.minusDays(34); !day.isAfter(end); day = day.plusDays(1)) {
            rows.add(new DatedPageRow(day, BLOG, 0, day.isAfter(end.minusDays(7)) ? recentPerDay : 100));
        }
        return stat(SeoTemplateAnalyzer.analyse(rows, end, List.of()), SeoTemplate.BLOG).alert();
    }

    @Test
    void templateWithoutBaselineNeverAlertsAndHasNoLossRatio() {
        LocalDate end = LocalDate.of(2026, 9, 24);
        List<DatedPageRow> rows = List.of(new DatedPageRow(end, BLOG, 1, 10));

        Report report = SeoTemplateAnalyzer.analyse(rows, end, List.of());

        assertThat(report.alerts()).isEmpty();
        assertThat(stat(report, SeoTemplate.BLOG).lossRatio()).isNull();
        assertThat(stat(report, SeoTemplate.PDF).lossRatio()).isNull();
    }

    @Test
    void siteKpiLeavesTheTvPostOutButTemplateSeriesKeepIt() {
        LocalDate end = LocalDate.of(2026, 9, 24);
        List<DatedPageRow> rows = List.of(
                new DatedPageRow(end, TV_POST, 50, 560),
                new DatedPageRow(end.minusDays(40), BLOG, 9, 9),
                new DatedPageRow(end, BLOG, 10, 440));

        Report report = SeoTemplateAnalyzer.analyse(rows, end, SeoTemplateAnalyzer.DEFAULT_EXCLUDED_PATHS);

        assertThat(report.siteImpressions28d()).isEqualTo(440);
        assertThat(report.siteClicks28d()).isEqualTo(10);
        assertThat(report.excludedImpressions28d()).isEqualTo(560);
        assertThat(stat(report, SeoTemplate.BLOG).recentImpressions()).isEqualTo(1000);
    }
}
