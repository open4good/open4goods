package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoSortableFields;
import org.open4goods.nudgerfrontapi.dto.xwiki.XwikiContentBlocDto;
import org.open4goods.nudgerfrontapi.service.ProductMappingService;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing XWiki content bloc
 */
@RestController
@RequestMapping("/contents/blocs")
@Validated
@Tag(name = "Content", description = "Expose nudger Xwiki based CMS content")
public class ContentsController {


	// TODO : mutualize constant
    private static final CacheControl ONE_HOUR_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();


	private XWikiHtmlService xwikiHtmlService;



    @GetMapping("/{blocId}")
    public ResponseEntity<XwikiContentBlocDto> contentBloc(@PathVariable @Parameter(allowEmptyValue = false,description = "", example = "", in = ParameterIn.PATH)
                                                       String blocId,
                                                       Locale locale){

        XwikiContentBlocDto body = new XwikiContentBlocDto();
        // TODO : implement this method, using xwikiHtmlservice

		return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }


}
