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

@RestController
public class PostsController {

    private static final long TTL_SECONDS = 300;

    private final BlogService blogService;

    public PostsController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/posts")
    @Operation(summary = "List blog posts")
    public ResponseEntity<List<BlogPostDto>> posts(@RequestParam(required = false) String locale) {
        List<BlogPostDto> posts = blogService.getPosts(locale);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(TTL_SECONDS, TimeUnit.SECONDS))
                .body(posts);
    }

    @GetMapping("/posts/{slug}")
    @Operation(summary = "Get blog post by slug")
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
