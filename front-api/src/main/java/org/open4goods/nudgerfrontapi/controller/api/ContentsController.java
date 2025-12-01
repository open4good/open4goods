package org.open4goods.nudgerfrontapi.controller.api;

import java.util.Locale;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.xwiki.FullPageDto;
import org.open4goods.nudgerfrontapi.dto.xwiki.XwikiContentBlocDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.nudgerfrontapi.service.XwikiFullPageService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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



    private final XWikiHtmlService xwikiHtmlService;
    private final XwikiFullPageService xwikiFullPageService;

    public ContentsController(XWikiHtmlService xwikiHtmlService,
                              XwikiFullPageService xwikiFullPageService) {
        this.xwikiHtmlService = xwikiHtmlService;
        this.xwikiFullPageService = xwikiFullPageService;
    }



    @GetMapping("/blocs/{blocId}")
    @Operation(
            summary = "Get content bloc",
            description = "Return the HTML content of the given XWiki bloc.",
            parameters = {
                    @Parameter(name = "blocId", description = "XWiki page path", example = "Main", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bloc found",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = XwikiContentBlocDto.class))),
                    @ApiResponse(responseCode = "404", description = "Bloc not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<XwikiContentBlocDto> contentBloc(@PathVariable String blocId,
                                                           @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                                           Locale locale,
                                                           @AuthenticationPrincipal UserDetails userDetails) {

    	String convertedBlocId = blocId.replace(":", "/");

        String htmlContent = xwikiHtmlService.html(convertedBlocId, domainLanguage.languageTag());
        String editLink = xwikiHtmlService.getEditPageUrl(convertedBlocId, domainLanguage.languageTag());
        XwikiContentBlocDto body = new XwikiContentBlocDto(convertedBlocId, htmlContent, editLink);

        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }
    @GetMapping("/pages/{xwikiPageId}")
    @Operation(
            summary = "Get XWiki page",
            description = "Return the rendered XWiki page along with metadata.",
            parameters = {
                    @Parameter(name = "xwikiPageId", description = "XWiki page path", example = "webpages:default:legal-notice:WebHome", in = ParameterIn.PATH, required = true),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Page found",
                            headers = @io.swagger.v3.oas.annotations.headers.Header(name = "X-Locale",
                                    description = "Resolved locale for textual payloads.",
                                    schema = @Schema(type = "string", example = "fr-FR")),
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = FullPageDto.class))),
                    @ApiResponse(responseCode = "404", description = "Page not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public ResponseEntity<FullPageDto> page(@PathVariable String xwikiPageId,
                                            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                            Locale locale) {

        String normalized = translatePageId(xwikiPageId);
        FullPageDto page = xwikiFullPageService.getFullPage(normalized, domainLanguage.languageTag());
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
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
