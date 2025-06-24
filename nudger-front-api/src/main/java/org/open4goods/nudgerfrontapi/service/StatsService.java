package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.StatsDto;
import org.springframework.stereotype.Service;

/**
 * Provides statistics for the frontend.
 */
@Service
public class StatsService {

    /**
     * Retrieve statistics. In this early stage the data is stubbed.
     */
    public StatsDto fetchStats() {
        return new StatsDto(0, 0);
    }
}
