package org.open4goods.nudgerfrontapi.controller;

import org.open4goods.nudgerfrontapi.dto.VoteRequest;
import org.open4goods.nudgerfrontapi.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@Tag(name = "Votes", description = "Submit user feedback votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/vote")
    @Operation(
            summary = "Submit a vote",
            description = "Record a user vote for a content item.",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vote recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid vote request")
    })
    public ResponseEntity<Void> vote(@RequestBody VoteRequest request) {
        voteService.submit(request);
        return ResponseEntity.ok().build();
    }
}
