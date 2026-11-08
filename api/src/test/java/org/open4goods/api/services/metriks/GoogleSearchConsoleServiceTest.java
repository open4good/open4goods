package org.open4goods.api.services.metriks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Tests the pagination and parsing of the Search Analytics response, bypassing the HTTP call and
 * Google credentials entirely (see {@link GoogleSearchConsoleService#paginate}).
 */
class GoogleSearchConsoleServiceTest {

    private GoogleSearchConsoleService newService() {
        return new GoogleSearchConsoleService("", "", "sc-domain:nudger.fr", 28, 3, null);
    }

    private String pageOf(int rowCount) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) {
                rows.append(',');
            }
            rows.append("{\"keys\":[\"https://nudger.fr/page").append(i).append("\"],\"impressions\":1}");
        }
        return "{\"rows\":[" + rows + "]}";
    }

    @Test
    void paginate_sumsTwoFullPagesAndOnePartialPage() throws Exception {
        GoogleSearchConsoleService service = newService();
        List<String> pages = List.of(pageOf(25000), pageOf(25000), pageOf(123));
        AtomicInteger calls = new AtomicInteger(0);

        long total = service.paginate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28),
                startRow -> pages.get(calls.getAndIncrement()));

        assertThat(total).isEqualTo(25000L + 25000L + 123L);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void paginate_stopsOnFirstPageWhenBelowRowLimit() throws Exception {
        GoogleSearchConsoleService service = newService();

        long total = service.paginate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28),
                startRow -> pageOf(42));

        assertThat(total).isEqualTo(42L);
    }

    @Test
    void countRows_withoutRowsField_returnsZero() {
        GoogleSearchConsoleService service = newService();

        assertThat(service.countRows("{}")).isZero();
    }

    @Test
    void countRows_withEmptyRowsArray_returnsZero() {
        GoogleSearchConsoleService service = newService();

        assertThat(service.countRows("{\"rows\":[]}")).isZero();
    }

    @Test
    void paginate_propagatesIoExceptionFromFetcher() {
        GoogleSearchConsoleService service = newService();

        assertThatThrownBy(() -> service.paginate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28), startRow -> {
            throw new IOException("Search Console API HTTP 500: boom");
        })).isInstanceOf(IOException.class).hasMessageContaining("HTTP 500");
    }

    @Test
    void pagesWithImpressionsResult_exposesTruncationFlag() {
        var result = new GoogleSearchConsoleService.PagesWithImpressionsResult(
                50000L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28), true);

        assertThat(result.count()).isEqualTo(50000L);
        assertThat(result.truncated()).isTrue();
    }
}
