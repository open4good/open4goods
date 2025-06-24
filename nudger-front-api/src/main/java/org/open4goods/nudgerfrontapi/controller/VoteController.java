package org.open4goods.nudgerfrontapi.controller;

import org.open4goods.nudgerfrontapi.dto.VoteRequest;
import org.open4goods.nudgerfrontapi.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/vote")
    @Operation(summary = "Submit a vote")
    public ResponseEntity<Void> vote(@RequestBody VoteRequest request) {
        voteService.submit(request);
        return ResponseEntity.ok().build();
    }
}
