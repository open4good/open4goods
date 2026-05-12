package org.open4goods.nudgerfrontapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheValueWeigherTest {

    private final CacheValueWeigher weigher = new CacheValueWeigher();

    @Test
    void nullValueWeighsMinimum() {
        assertThat(weigher.weigh("k", null)).isEqualTo(CacheValueWeigher.MIN_WEIGHT_BYTES);
    }

    @Test
    void smallStringWeighsMoreThanMinimum() {
        int weight = weigher.weigh("k", "hello");
        assertThat(weight).isGreaterThan(CacheValueWeigher.MIN_WEIGHT_BYTES);
    }

    @Test
    void largerListWeighsMoreThanSmallerList() {
        List<String> small = new ArrayList<>();
        small.add("a");
        List<String> large = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            large.add("entry-" + i);
        }

        int smallWeight = weigher.weigh("k", small);
        int largeWeight = weigher.weigh("k", large);

        assertThat(largeWeight).isGreaterThan(smallWeight);
    }

    @Test
    void responseEntityIsUnwrappedToItsBody() {
        List<String> body = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            body.add("payload-entry-" + i);
        }
        int bodyWeight = weigher.weigh("k", body);
        int entityWeight = weigher.weigh("k", ResponseEntity.ok(body));

        // Within an order of magnitude (entity adds a small wrapper)
        assertThat(entityWeight).isBetween(bodyWeight - 1024, bodyWeight + 4096);
    }
}
