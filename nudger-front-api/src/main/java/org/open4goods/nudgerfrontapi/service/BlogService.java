package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.nudgerfrontapi.dto.BlogPostDto;
import org.springframework.stereotype.Service;

/**
 * Simple in-memory blog service used for the front API.
 */
@Service
public class BlogService {

    private final Map<String, BlogPostDto> posts = new HashMap<>();

    public BlogService() {
        Date now = new Date();
        posts.put("hello-world", new BlogPostDto("hello-world", "Hello World", "en",
                "Demo post", "Hello world content", now));
        posts.put("bonjour", new BlogPostDto("bonjour", "Bonjour", "fr",
                "Article de d\u00e9monstration", "Bonjour le monde", now));
    }

    /**
     * List available posts optionally filtered by locale.
     */
    public List<BlogPostDto> getPosts(String locale) {
        if (locale == null) {
            return new ArrayList<>(posts.values());
        }
        return posts.values().stream()
                .filter(p -> locale.equalsIgnoreCase(p.locale()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a post by its slug and optional locale.
     */
    public BlogPostDto getPost(String slug, String locale) {
        BlogPostDto post = posts.get(slug);
        if (post == null) {
            return null;
        }
        if (locale == null || locale.equalsIgnoreCase(post.locale())) {
            return post;
        }
        return null;
    }
}
