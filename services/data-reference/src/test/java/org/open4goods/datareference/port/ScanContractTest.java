package org.open4goods.datareference.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The batch-scan contract every port scanning stored data is written against.
 */
class ScanContractTest {

    @Test
    void aFirstPageStartsWithNoCursorAndAStableOrder() {
        ScanRequest first = ScanRequest.first(500);

        assertThat(first.cursor()).isEmpty();
        assertThat(first.order()).isEqualTo(ScanOrder.RECORD_KEY);
        assertThat(first.failurePolicy()).isEqualTo(ScanFailurePolicy.FAIL_FAST);
    }

    @Test
    void resumingKeepsOrderAndFailureBehaviour() {
        ScanRequest first = new ScanRequest(ScanCursor.start(), 100, ScanOrder.OBSERVED_AT,
                ScanFailurePolicy.SKIP_AND_REPORT);

        ScanRequest next = first.resumeAt(new ScanCursor("abc"));

        assertThat(next.cursor()).contains(new ScanCursor("abc"));
        assertThat(next.pageSize()).isEqualTo(100);
        assertThat(next.order()).isEqualTo(ScanOrder.OBSERVED_AT);
        assertThat(next.failurePolicy()).isEqualTo(ScanFailurePolicy.SKIP_AND_REPORT);
    }

    @Test
    void rejectsAPageSizeOutsideTheContract() {
        assertThatThrownBy(() -> ScanRequest.first(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ScanRequest.first(ScanRequest.MAX_PAGE_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankCursorToken() {
        assertThatThrownBy(() -> new ScanCursor("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEmptyPageWithACursorIsNotTheEndOfTheScan() {
        ScanPage<String> empty = new ScanPage<>(List.of(), Optional.of(new ScanCursor("next")), List.of());

        // A store filtering server-side legitimately returns nothing while advancing.
        assertThat(empty.elements()).isEmpty();
        assertThat(empty.hasMore()).isTrue();
    }

    @Test
    void aPageWithoutACursorEndsTheScan() {
        assertThat(ScanPage.last(List.of("a", "b")).hasMore()).isFalse();
    }

    @Test
    void pageElementOrderIsPreserved() {
        assertThat(ScanPage.last(List.of("c", "a", "b")).elements()).containsExactly("c", "a", "b");
    }

    @Test
    void aFailureNamesTheElementAndTheReason() {
        assertThatThrownBy(() -> new ScanFailure("", "boom")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScanFailure("icecat/REC-1", " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ScanFailure("icecat/REC-1", "unreadable payload").elementId())
                .isEqualTo("icecat/REC-1");
    }
}
