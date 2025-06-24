package org.open4goods.nudgerfrontapi.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.PartnerDto;
import org.open4goods.nudgerfrontapi.service.PartnerService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint exposing partners.
 */
@RestController
public class PartnerController {

    private static final long TTL_SECONDS = 300;

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/partners")
    public ResponseEntity<List<PartnerDto>> partners() {
        List<PartnerDto> list = partnerService.fetchPartners();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(list);
    }
}
