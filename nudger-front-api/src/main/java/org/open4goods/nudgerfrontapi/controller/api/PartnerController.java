package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.PartnerDto;
import org.open4goods.nudgerfrontapi.service.PartnerService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoint exposing partners.
 */
@RestController
@Tag(name = "Partners", description = "Information about project partners")
public class PartnerController {

    private static final long TTL_SECONDS = 300;

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @GetMapping("/partners")
    @Operation(
            summary = "List partners",
            description = "Return all partner organisations.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partners returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PartnerDto.class, type = "array")))
    })
    public ResponseEntity<List<PartnerDto>> partners() {
        List<PartnerDto> list = partnerService.fetchPartners();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(list);
    }
}
