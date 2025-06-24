package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.HomeCompositeResponse;
import org.open4goods.nudgerfrontapi.dto.PartnerDto;
import org.open4goods.nudgerfrontapi.dto.StatsDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aggregates data for the home endpoint.
 */
@Service
public class HomeService {

    private final StatsService statsService;
    private final PartnerService partnerService;

    public HomeService(StatsService statsService, PartnerService partnerService) {
        this.statsService = statsService;
        this.partnerService = partnerService;
    }

    /**
     * Retrieve the home composite response.
     */
    public HomeCompositeResponse getHome() {
        StatsDto stats = statsService.fetchStats();
        List<PartnerDto> partners = partnerService.fetchPartners();
        return new HomeCompositeResponse(stats, partners);
    }
}
