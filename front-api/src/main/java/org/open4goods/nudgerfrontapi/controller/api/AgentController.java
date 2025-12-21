package org.open4goods.nudgerfrontapi.controller.api;

import java.io.IOException;

import org.kohsuke.github.GHFileNotFoundException;
import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.agent.AgentErrorResponseDto;
import org.open4goods.nudgerfrontapi.dto.agent.AgentStatusDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing agent workflow state backed by GitHub issues and branches.
 */
@RestController
@RequestMapping("/agents")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Agents", description = "Suivi des workflows d'agents automatisés.")
public class AgentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Return the workflow status and branch URL associated with the given GitHub issue.
     *
     * @param issueId        GitHub issue identifier
     * @param domainLanguage localisation hint propagated to the response header
     * @return workflow state and branch URL if available
     */
    @GetMapping("/{issueId}")
    @Operation(
            summary = "Récupérer l'état d'un agent",
            description = "Expose l'état du workflow pour un ticket GitHub et la branche associée si elle existe.",
            parameters = {
                    @Parameter(name = "issueId", in = ParameterIn.PATH, required = true,
                            description = "Identifiant GitHub de l'issue suivie.",
                            schema = @Schema(type = "string", example = "128")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Langue de domaine (future localisation).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "État trouvé",
                            headers = @Header(name = "X-Locale", description = "Locale résolue.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AgentStatusDto.class))),
                    @ApiResponse(responseCode = "400", description = "Identifiant invalide",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AgentErrorResponseDto.class))),
                    @ApiResponse(responseCode = "404", description = "Issue inconnue",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AgentErrorResponseDto.class))),
                    @ApiResponse(responseCode = "502", description = "Erreur GitHub",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AgentErrorResponseDto.class)))
            }
    )
    public ResponseEntity<?> getAgentStatus(@PathVariable("issueId") String issueId,
                                            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        try {
            AgentStatusDto status = agentService.getAgentStatus(issueId);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(status);
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Invalid issue id '{}': {}", issueId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(new AgentErrorResponseDto("L'identifiant d'issue doit être numérique."));
        } catch (GHFileNotFoundException ex) {
            LOGGER.warn("Issue {} not found on GitHub", issueId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(new AgentErrorResponseDto("Issue inconnue ou inaccessible."));
        } catch (IOException ex) {
            LOGGER.error("GitHub error while reading issue {}: {}", issueId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .cacheControl(CacheControl.noCache())
                    .header("X-Locale", domainLanguage.languageTag())
                    .body(new AgentErrorResponseDto("Erreur lors de la récupération des données GitHub."));
        }
    }
}
