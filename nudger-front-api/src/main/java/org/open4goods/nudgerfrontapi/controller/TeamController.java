package org.open4goods.nudgerfrontapi.controller;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.TeamMemberDto;
import org.open4goods.nudgerfrontapi.service.TeamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/team")
    @Operation(summary = "List team members")
    public List<TeamMemberDto> team() {
        return teamService.getMembers();
    }
}
