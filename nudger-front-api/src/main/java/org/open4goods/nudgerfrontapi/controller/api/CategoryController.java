package org.open4goods.nudgerfrontapi.controller.api;

import org.open4goods.nudgerfrontapi.dto.CategoryListResponse;
import org.open4goods.nudgerfrontapi.dto.CategoryRequest;
import org.open4goods.nudgerfrontapi.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing the public category hierarchy used by the frontend.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Categories", description = "Retrieve product categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * List available root categories.
     *
     * @param request pagination and inclusion parameters
     * @return paged list of categories
     */
    @GetMapping("/categories")
    @Operation(
            summary = "List root categories",
            description = "Return paged root categories optionally with child nodes.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "page", description = "Page index", required = false),
                    @Parameter(name = "size", description = "Page size", required = false),
                    @Parameter(name = "include", description = "Include child categories", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categories returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CategoryListResponse.class)))
            }
    )
    public ResponseEntity<CategoryListResponse> categories(CategoryRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();
        var all = categoryService.listRootCategories(request.include());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        var body = new CategoryListResponse(page, size, all.size(), all.subList(from, to));
        return ResponseEntity.ok(body);
    }

    /**
     * Return a single category by its identifier.
     *
     * @param id      category identifier
     * @param request options such as including child categories
     * @return the category description
     */
    @GetMapping("/categories/{id}")
    @Operation(
            summary = "Get category",
            description = "Return a single category optionally with its children.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "id", description = "Category identifier", required = true),
                    @Parameter(name = "include", description = "Include child categories", required = false)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category returned",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CategoryService.CategoryDto.class))),
                    @ApiResponse(responseCode = "404", description = "Category not found")
            }
    )
    public ResponseEntity<CategoryService.CategoryDto> category(@PathVariable int id, CategoryRequest request) {
        var body = categoryService.getCategory(id, request.include());
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }
}
