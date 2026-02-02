package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.UserGeolocationService;
import org.open4goods.nudgerfrontapi.utils.IpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller exposing user-specific endpoints for the frontend.
 */
@RestController
@RequestMapping("/user")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "User", description = "User-specific endpoints for the frontend")
public class UserController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserGeolocationService userGeolocationService;

    /**
     * Creates a new user controller.
     *
     * @param userGeolocationService geolocation service
     */
    public UserController(UserGeolocationService userGeolocationService)
    {
        this.userGeolocationService = userGeolocationService;
    }

    /**
     * Resolve the current user geolocation using the geocode microservice.
     *
     * @param domainLanguage language hint used for locale headers
     * @param ip optional IP override
     * @param httpRequest HTTP request used to resolve the client IP
     * @return geolocation response
     */
    @GetMapping("/geoloc")
    @Operation(
            summary = "Get user geolocation",
            description = "Resolve the requesting user IP address using the MaxMind GeoIP database.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class)),
                    @Parameter(name = "ip", in = ParameterIn.QUERY, required = false,
                            description = "Optional IP address override. Defaults to the client IP.",
                            schema = @Schema(type = "string", example = "81.2.69.142"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Geolocation returned",
                            headers = @Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserGeolocDto.class))),
                    @ApiResponse(responseCode = "400", description = "Missing or invalid IP parameter"),
                    @ApiResponse(responseCode = "404", description = "Geolocation not found"),
                    @ApiResponse(responseCode = "502", description = "Geocode service unavailable")
            }
    )
    public ResponseEntity<UserGeolocDto> geoloc(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                @RequestParam(name = "ip", required = false) String ip,
                                                HttpServletRequest httpRequest)
    {
        LOGGER.info("User geolocation requested");
        String resolvedIp = StringUtils.hasText(ip) ? ip : IpUtils.getIp(httpRequest);
        if (!StringUtils.hasText(resolvedIp))
        {
            LOGGER.warn("User geolocation requested without a resolvable IP");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Locale", domainLanguage.languageTag())
                    .build();
        }
        UserGeolocDto response;
        try
        {
            response = userGeolocationService.resolve(resolvedIp);
        }
        catch (IllegalStateException ex)
        {
            LOGGER.error("Geocode service error while resolving user geolocation", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .header("X-Locale", domainLanguage.languageTag())
                    .build();
        }
        if (response == null)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Locale", domainLanguage.languageTag())
                    .build();
        }
        return ResponseEntity.ok()
                .header("X-Locale", domainLanguage.languageTag())
                .body(response);
    }
}
