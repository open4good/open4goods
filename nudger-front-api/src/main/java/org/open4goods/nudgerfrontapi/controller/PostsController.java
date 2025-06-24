package org.open4goods.nudgerfrontapi.controller;

import java.util.List;

import org.open4goods.nudgerfrontapi.dto.BlogPostDto;
import org.open4goods.nudgerfrontapi.service.BlogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
public class PostsController {

    private final BlogService blogService;

    public PostsController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/posts")
    @Operation(summary = "List blog posts")
    public List<BlogPostDto> posts(@RequestParam(required = false) String locale) {
        return blogService.getPosts(locale);
    }

    @GetMapping("/posts/{slug}")
    @Operation(summary = "Get blog post by slug")
    public BlogPostDto post(@PathVariable String slug, @RequestParam(name = "locale", required = false) String locale) {
        return blogService.getPost(slug, locale);
    }
}
