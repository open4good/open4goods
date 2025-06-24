package org.open4goods.nudgerfrontapi.service;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.TeamMemberDto;
import org.springframework.stereotype.Service;

/**
 * Provides team members information.
 */
@Service
public class TeamService {

    private final List<TeamMemberDto> members = List.of(
            new TeamMemberDto("Alice", "Founder"),
            new TeamMemberDto("Bob", "Engineer"));

    public List<TeamMemberDto> getMembers() {
        return members;
    }
}
