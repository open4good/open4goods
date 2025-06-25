package org.open4goods.nudgerfrontapi.controller.api;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.SearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing the search endpoint used by the Nuxt frontend.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Search", description = "Product search operations")
public class SearchController {

    /**
     * Search for products matching the given query.  At the moment this
     * implementation returns an empty result set.
     *
     * @param query free text search terms
     * @return a response entity containing the search response
     */
    @GetMapping("/search")
    @Operation(
            summary = "Search products",
            description = "Return a paginated list of products matching the search query."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SearchResponse.class)))
    })
    public ResponseEntity<SearchResponse> search(
            @Parameter(description = "Free text query", example = "eco toothbrush")
            @RequestParam String query) {
        SearchResponse body = new SearchResponse(0, 0, 0, List.of());
        return ResponseEntity.ok(body);
    }
}
