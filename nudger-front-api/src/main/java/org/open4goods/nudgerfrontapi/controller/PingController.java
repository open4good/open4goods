package org.open4goods.nudgerfrontapi.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Simple health check")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
