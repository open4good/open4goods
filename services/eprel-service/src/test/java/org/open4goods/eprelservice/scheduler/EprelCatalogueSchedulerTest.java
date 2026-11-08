package org.open4goods.eprelservice.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.services.eprelservice.scheduler.EprelCatalogueScheduler;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Verifies the EPREL refresh does not depend on how long the application has been running.
 */
class EprelCatalogueSchedulerTest
{
    @Test
    void refreshFollowsAConfigurableCronWithoutStartupDelay() throws NoSuchMethodException
    {
        Scheduled scheduled = EprelCatalogueScheduler.class.getMethod("schedule").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${eprel.refresh-cron:0 0 3 */2 * *}");
        assertThat(scheduled.initialDelayString()).isEmpty();
        assertThat(scheduled.fixedDelayString()).isEmpty();
    }
}
