package org.open4goods.nudgerfrontapi.service;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.PartnerDto;
import org.springframework.stereotype.Service;

/**
 * Provides partner information to the frontend.
 */
@Service
public class PartnerService {

    /**
     * Retrieve partner list. Stubbed for now.
     */
    public List<PartnerDto> fetchPartners() {
        return List.of();
    }
}
