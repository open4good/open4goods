package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.Locale;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.xwiki.XwikiContentBlocDto;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing XWiki content bloc
 */
@RestController
@RequestMapping
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Content", description = "Content blocs")
public class ContentsController {


    // TODO : mutualize constant
    private static final CacheControl ONE_HOUR_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final XWikiHtmlService xwikiHtmlService;
    private final XwikiFacadeService xwikiFacadeService;

    public ContentsController(XWikiHtmlService xwikiHtmlService,
                              XwikiFacadeService xwikiFacadeService) {
        this.xwikiHtmlService = xwikiHtmlService;
        this.xwikiFacadeService = xwikiFacadeService;
    }



    @GetMapping("/blocs/{blocId}")
    @Operation(
            summary = "Get content bloc",
            description = "Return the HTML content of the given XWiki bloc.",
            parameters = {
                    @Parameter(name = "blocId", description = "XWiki page path", example = "Main", in = ParameterIn.PATH, required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bloc found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = XwikiContentBlocDto.class))),
                    @ApiResponse(responseCode = "404", description = "Bloc not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<XwikiContentBlocDto> contentBloc(@PathVariable String blocId,
                                                           Locale locale,
                                                           @AuthenticationPrincipal UserDetails userDetails) {

        String htmlContent = xwikiHtmlService.html(blocId);
        String editLink = xwikiHtmlService.getEditPageUrl(blocId);
        XwikiContentBlocDto body = new XwikiContentBlocDto(blocId, htmlContent, editLink);

        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/pages/{xwikiPageId}")
    @Operation(
            summary = "Get XWiki page",
            description = "Return the rendered XWiki page along with metadata.",
            parameters = {
                    @Parameter(name = "xwikiPageId", description = "XWiki page path", example = "Main.WebHome", in = ParameterIn.PATH, required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullPage.class))),
                    @ApiResponse(responseCode = "404", description = "Page not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<FullPage> page(@PathVariable String xwikiPageId,
                                         Locale locale) {
        String normalized = translatePageId(xwikiPageId);
        FullPage page = xwikiFacadeService.getFullPage(normalized);
        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(page);
    }

    private String translatePageId(String raw) {
        if (raw == null) {
            return "";
        }
        String translated = raw.replace('.', '/').replace(':', '/');
        if (translated.startsWith("/")) {
            translated = translated.substring(1);
        }
        return translated;
    }


}
