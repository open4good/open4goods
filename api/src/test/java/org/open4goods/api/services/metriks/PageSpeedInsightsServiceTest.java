package org.open4goods.api.services.metriks;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;

import tools.jackson.databind.ObjectMapper;

class PageSpeedInsightsServiceTest {

    private static final String RESPONSE = """
            {"lighthouseResult":{"categories":{"performance":{"score":0.83}},
             "audits":{"largest-contentful-paint":{"numericValue":2400.4},
                       "cumulative-layout-shift":{"numericValue":0.05},
                       "total-blocking-time":{"numericValue":120}}},
             "loadingExperience":{"metrics":{"LARGEST_CONTENTFUL_PAINT_MS":{"percentile":2100},
                       "CUMULATIVE_LAYOUT_SHIFT_SCORE":{"percentile":8}}}}""";

    private PageSpeedInsightsService service(String key) {
        return new PageSpeedInsightsService(key, "https://nudger.fr/", List.of("/", "/blog"), "",
                HttpClient.newHttpClient());
    }

    @Test
    void parsesLabMetricsAndFieldDataWhenPresent() throws Exception {
        List<MetriksEvent> events = service("k").parse("home", "https://nudger.fr/", "2026-11-26T00:00:00Z",
                new ObjectMapper().readTree(RESPONSE));

        assertThat(events).extracting(MetriksEvent::getId).containsExactly("psi.home.performance", "psi.home.lcp",
                "psi.home.cls", "psi.home.tbt", "psi.home.field_lcp", "psi.home.field_cls");
        assertThat(events.get(0).getValue().doubleValue()).isEqualTo(83.0);
        assertThat(events.get(5).getValue().doubleValue()).isEqualTo(0.08);
        assertThat(events.get(0).getParams()).containsEntry("fieldData", true);
    }

    @Test
    void absentMetricIsAnErrorNotZero() throws Exception {
        List<MetriksEvent> events = service("k").parse("home", "https://nudger.fr/", "t",
                new ObjectMapper().readTree("{\"lighthouseResult\":{}}"));

        assertThat(events).hasSize(4).allSatisfy(e -> {
            assertThat(e.getStatus()).isEqualTo("error");
            assertThat(e.getValue()).isNull();
        });
    }

    @Test
    void withoutApiKeyNothingIsMeasuredAndTheSnapshotIsAnError() {
        PageSpeedInsightsService service = service("");

        assertThat(service.refresh()).singleElement().extracting(MetriksEvent::getStatus).isEqualTo("error");
        assertThat(service.latestEvents()).singleElement().extracting(MetriksEvent::getErrorMessage)
                .asString().contains("api-key");
    }

    @Test
    void urlIdFromPath() {
        assertThat(PageSpeedInsightsService.urlId("/")).isEqualTo("home");
        assertThat(PageSpeedInsightsService.urlId("/televiseurs/televiseur-55-pouces"))
                .isEqualTo("televiseurs_televiseur-55-pouces");
    }
}
