package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.VoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Records user votes. This implementation only logs submissions.
 */
@Service
public class VoteService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoteService.class);

    public void submit(VoteRequest request) {
        LOGGER.info("Received vote for {} with value {}", request.itemId(), request.value());
    }
}
