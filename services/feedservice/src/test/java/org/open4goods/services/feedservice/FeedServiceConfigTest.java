package org.open4goods.services.feedservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {FeedConfiguration.class})
@EnableConfigurationProperties(FeedConfiguration.class)
@ActiveProfiles("test")
public class FeedServiceConfigTest {

    @Autowired
    private FeedConfiguration feedConfiguration;

    @Test
    void testDefaultValues() {
        assertThat(feedConfiguration.getAwin().getCron()).isEqualTo("0 43 1 * * ?");
        assertThat(feedConfiguration.getAwin().isEnabled()).isTrue();
        assertThat(feedConfiguration.getAwin().getCacheTtlDays()).isEqualTo(1);

        assertThat(feedConfiguration.getEffiliation().getCron()).isEqualTo("30 43 1 * * ?");
        assertThat(feedConfiguration.getEffiliation().isEnabled()).isTrue();
        assertThat(feedConfiguration.getEffiliation().getCacheTtlDays()).isEqualTo(1);
    }


}
