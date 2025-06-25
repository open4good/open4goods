package org.open4goods.nudgerfrontapi.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.AssociationDto;
import org.open4goods.nudgerfrontapi.service.AssociationsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@Tag(name = "Associations", description = "Environmental associations data")
public class AssociationsController {

    private static final long TTL_SECONDS = 300;

    private final AssociationsService associationsService;

    public AssociationsController(AssociationsService associationsService) {
        this.associationsService = associationsService;
    }

    @GetMapping("/associations")
    @Operation(
            summary = "List associations",
            description = "Return the list of partner associations.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Associations returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AssociationDto.class, type = "array")))
    })
    public ResponseEntity<List<AssociationDto>> associations() {
        List<AssociationDto> list = associationsService.getAssociations();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(list);
    }
}
