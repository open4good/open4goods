package org.open4goods.nudgerfrontapi.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.TeamMemberDto;
import org.open4goods.nudgerfrontapi.service.TeamService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class TeamController {

    private static final long TTL_SECONDS = 300;

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/team")
    @Operation(summary = "List team members")
    public ResponseEntity<List<TeamMemberDto>> team() {
        List<TeamMemberDto> members = teamService.getMembers();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(members);
    }
}
