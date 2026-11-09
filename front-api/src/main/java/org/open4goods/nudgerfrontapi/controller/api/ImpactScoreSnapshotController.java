package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;
import java.util.regex.Pattern;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.impactscore.ImpactScoreSnapshotDto;
import org.open4goods.services.productrepository.impactscore.ImpactScoreSnapshotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * History of the monthly ImpactScore snapshots, for the methodology history page and the quarterly barometer.
 */
@RestController
@RequestMapping("/impactscore")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "ImpactScore", description = "Aggregated ImpactScore history per vertical")
public class ImpactScoreSnapshotController {

    private static final Pattern MONTH = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

    private final ImpactScoreSnapshotRepository repository;

    public ImpactScoreSnapshotController(ImpactScoreSnapshotRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/snapshots")
    @Operation(
            summary = "List the monthly ImpactScore snapshots",
            description = "Aggregated snapshots (counts, percentiles, coverage, EPREL matching, top GTINs), oldest first. Filters are optional.",
            parameters = {
                    @Parameter(name = "verticalId", description = "Restrict to one vertical."),
                    @Parameter(name = "from", description = "First month included, yyyy-MM."),
                    @Parameter(name = "to", description = "Last month included, yyyy-MM.")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Snapshots returned",
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ImpactScoreSnapshotDto.class)))),
                    @ApiResponse(responseCode = "400", description = "Invalid month"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    public ResponseEntity<List<ImpactScoreSnapshotDto>> snapshots(
            @RequestParam(name = "verticalId", required = false) String verticalId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        if (invalid(from) || invalid(to)) {
            return ResponseEntity.badRequest().build();
        }
        List<ImpactScoreSnapshotDto> body = repository.find(verticalId, from, to).stream()
                .map(ImpactScoreSnapshotDto::from).toList();
        return ResponseEntity.ok().cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE).body(body);
    }

    private static boolean invalid(String month) {
        return month != null && !MONTH.matcher(month).matches();
    }
}
