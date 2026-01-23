package org.open4goods.nudgerfrontapi.controller.api;

import java.io.IOException;
import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.agent.AgentActivityDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentIssueDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentRequestDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentRequestResponseDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentTemplateDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.AgentService;
import org.open4goods.nudgerfrontapi.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/agents")
@Tag(name = "Agents", description = "AI Agent interactions")
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
public class AgentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/templates")
    @Operation(summary = "Get available agent templates")
    public ResponseEntity<List<AgentTemplateDto>> listTemplates(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Locale", domainLanguage.languageTag())
                .body(agentService.listTemplates(domainLanguage));
    }

    @PostMapping
    @Operation(summary = "Submit a request to an agent", 
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Agent request payload",
        required = true,
        content = @Content(schema = @Schema(implementation = AgentRequestDto.class))
    ),
    responses = {
        @ApiResponse(responseCode = "201", description = "Request submitted successfully", 
            content = @Content(schema = @Schema(implementation = AgentRequestResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AgentRequestResponseDto> submitRequest(
            @Valid @RequestBody AgentRequestDto request,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            HttpServletRequest httpRequest) {
        try {
            String clientIp = IpUtils.getIp(httpRequest);
            AgentRequestResponseDto response = agentService.submitRequest(request, clientIp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(response);
        } catch (IOException e) {
            LOGGER.error("Failed to submit agent request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{issueId}")
    @Operation(summary = "Get details of an agent-created issue")
    public ResponseEntity<AgentIssueDto> getIssue(
            @PathVariable String issueId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        try {
            return agentService.getIssue(issueId, domainLanguage)
                    .map(dto -> ResponseEntity.ok()
                            .header("X-Locale", domainLanguage.languageTag())
                            .body(dto))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IOException e) {
            LOGGER.error("Failed to get issue details", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/activity")
    @Operation(summary = "Get recent agent activity")
    public ResponseEntity<List<AgentActivityDto>> listActivity(
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        try {
            return ResponseEntity.ok()
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(agentService.listActivity(domainLanguage));
        } catch (IOException e) {
            LOGGER.error("Failed to list activity", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/mailto")
    @Operation(summary = "Get mailto link for fallback")
    public ResponseEntity<String> getMailto(
            @RequestParam(name = "agentId") String agentId,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        String mailto = agentService.getMailto(agentId, domainLanguage);
        if (mailto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("X-Locale", domainLanguage.languageTag())
                .body(mailto);
    }
}
