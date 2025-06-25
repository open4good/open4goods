package org.open4goods.nudgerfrontapi.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.open4goods.nudgerfrontapi.dto.BlogPostDto;
import org.open4goods.nudgerfrontapi.service.BlogService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Blog", description = "Access blog posts for the website")
public class PostsController {

    private static final long TTL_SECONDS = 300;

    private final BlogService blogService;

    public PostsController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/posts")
    @Operation(
            summary = "List blog posts",
            description = "Return published blog posts. Optionally filter by locale.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = @Parameter(name = "locale", description = "Locale code", required = false)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogPostDto.class, type = "array")))
    })
    public ResponseEntity<List<BlogPostDto>> posts(@RequestParam(required = false) String locale) {
        List<BlogPostDto> posts = blogService.getPosts(locale);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(posts);
    }

    @GetMapping("/posts/{slug}")
    @Operation(
            summary = "Get blog post",
            description = "Return a single blog post identified by its slug.",
            security = @SecurityRequirement(name = "bearer-jwt"),
            parameters = {
                    @Parameter(name = "slug", description = "Post slug", required = true),
                    @Parameter(name = "locale", description = "Locale code", required = false)
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BlogPostDto.class))),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<BlogPostDto> post(@PathVariable String slug, @RequestParam(name = "locale", required = false) String locale) {
        BlogPostDto post = blogService.getPost(slug, locale);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(post);
    }
}
