package org.open4goods.nudgerfrontapi.service.css;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class CssDryRunReportTest {

    @Test
    void reportsEligibleExaminedAndReasonsPerVertical() {
        Map<CssExclusionReason, Long> tv = new EnumMap<>(CssExclusionReason.class);
        tv.put(CssExclusionReason.NOT_ENOUGH_MERCHANT_DOMAINS, 7L);
        tv.put(CssExclusionReason.NO_IMAGE, 1L);
        Map<String, CssDryRunReport.VerticalReport> verticals = new TreeMap<>();
        verticals.put("tv", new CssDryRunReport.VerticalReport(10, 3, tv));
        verticals.put("fridge", new CssDryRunReport.VerticalReport(5, 5, new EnumMap<>(CssExclusionReason.class)));
        CssDryRunReport report = new CssDryRunReport("2026-11-10T10:00:00Z", verticals);

        assertThat(report.examined()).isEqualTo(15);
        assertThat(report.eligible()).isEqualTo(8);
        assertThat(report.toMarkdown())
                .contains("**8** / 15")
                .contains("| tv | 10 | 3 | NOT_ENOUGH_MERCHANT_DOMAINS: 7, NO_IMAGE: 1 |")
                .contains("| fridge | 5 | 5 |");
    }
}
