package org.open4goods.nudgerfrontapi.service.share;

import java.time.Instant;
import java.util.Optional;

import org.open4goods.nudgerfrontapi.dto.share.ShareResolutionResponseDto;

/**
 * Abstraction for storing share resolution snapshots.
 */
public interface ShareResolutionStore {

    /**
     * Persist the provided snapshot until the given expiration time.
     *
     * @param response  response to store
     * @param expiresAt expiration timestamp
     */
    void save(ShareResolutionResponseDto response, Instant expiresAt);

    /**
     * Retrieve the snapshot associated with the token when still valid.
     *
     * @param token unique token identifying the resolution
     * @return optional snapshot
     */
    Optional<ShareResolutionResponseDto> get(String token);
}
