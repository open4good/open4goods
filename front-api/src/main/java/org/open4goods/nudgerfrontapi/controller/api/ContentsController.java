package org.open4goods.nudgerfrontapi.controller.api;

import java.util.Locale;

import org.open4goods.nudgerfrontapi.dto.xwiki.XwikiContentBlocDto;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.nudgerfrontapi.config.CacheConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing XWiki content bloc
 */
@RestController
@RequestMapping("/contents/blocs")
@Validated
@Tag(name = "Content", description = "Expose nudger Xwiki based CMS content")
public class ContentsController {


	private XWikiHtmlService xwikiHtmlService;



    @GetMapping("/{blocId}")
    public ResponseEntity<XwikiContentBlocDto> contentBloc(@PathVariable @Parameter(allowEmptyValue = false,description = "", example = "", in = ParameterIn.PATH)
                                                       String blocId,
                                                       Locale locale){

        XwikiContentBlocDto body = new XwikiContentBlocDto();

                return ResponseEntity.ok()
                .cacheControl(CacheConfig.ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }


}
