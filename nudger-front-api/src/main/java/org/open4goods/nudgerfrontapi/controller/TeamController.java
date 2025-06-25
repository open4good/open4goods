package org.open4goods.nudgerfrontapi.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.TeamMemberDto;
import org.open4goods.nudgerfrontapi.service.TeamService;
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
@Tag(name = "Team", description = "Information about project team members")
public class TeamController {

    private static final long TTL_SECONDS = 300;

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/team")
    @Operation(
            summary = "List team members",
            description = "Return all team members contributing to the project.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TeamMemberDto.class, type = "array")))
    })
    public ResponseEntity<List<TeamMemberDto>> team() {
        List<TeamMemberDto> members = teamService.getMembers();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(members);
    }
}
