package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Verifies feed indexation remains wired to an externally configurable schedule.
 */
class BatchServiceSchedulingTest {

    @Test
    void fetchFeedsUsesConfigurableCron() throws NoSuchMethodException {
        Method method = BatchService.class.getMethod("fetchFeeds");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("${feed.indexation.cron:19 13 23 * * ?}");
    }
}
