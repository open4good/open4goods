package org.open4goods.nudgerfrontapi.service.share;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionResponseDto;

/**
 * In-memory implementation of {@link ShareResolutionStore} with TTL eviction.
 */
public class InMemoryShareResolutionStore implements ShareResolutionStore {

    private final Map<String, StoredResolution> store = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryShareResolutionStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(ShareResolutionResponseDto response, Instant expiresAt) {
        store.put(response.token(), new StoredResolution(response, expiresAt));
    }

    @Override
    public Optional<ShareResolutionResponseDto> get(String token) {
        StoredResolution holder = store.get(token);
        if (holder == null) {
            return Optional.empty();
        }
        if (clock.instant().isAfter(holder.expiresAt())) {
            store.remove(token);
            return Optional.empty();
        }
        return Optional.of(holder.response());
    }

    private record StoredResolution(ShareResolutionResponseDto response, Instant expiresAt) {
    }
}
