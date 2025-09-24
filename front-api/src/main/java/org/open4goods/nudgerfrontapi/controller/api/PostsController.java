package org.open4goods.nudgerfrontapi.controller.api;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.dto.PageDto;
import org.open4goods.nudgerfrontapi.dto.blog.BlogPostDto;
import org.open4goods.nudgerfrontapi.dto.blog.BlogTagDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.services.blog.service.BlogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
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

import com.rometools.rome.io.FeedException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing blog posts coming from XWiki.
 */
@RestController
@RequestMapping("/blog")
@Validated
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Blog", description = "Blog posts, tags and RSS feed")
public class PostsController {

    private static final CacheControl ONE_HOUR_PUBLIC_CACHE = CacheControl.maxAge(Duration.ofHours(1)).cachePublic();

    private final BlogService blogService;

    public PostsController(BlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/posts")
    @Operation(
            summary = "List blog posts",
            description = "Return paginated blog posts optionally filtered by tag.",
            parameters = {
                    @Parameter(name = "tag", in = ParameterIn.QUERY, description = "Category/tag to filter on"),
                    @Parameter(name = "pageNumber", in = ParameterIn.QUERY,
                            description = "Zero-based page index",
                            schema = @Schema(type = "integer", minimum = "0")),
                    @Parameter(name = "pageSize", in = ParameterIn.QUERY,
                            description = "Page size",
                            schema = @Schema(type = "integer", minimum = "0")),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Posts returned",
                            headers = {
                                    @Header(name = "Link", description = "Pagination links as defined by RFC 8288"),
                                    @Header(name = "X-Locale", description = "Resolved locale for textual payloads.",
                                            schema = @Schema(type = "string", example = "fr-FR"))
                            },
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PageDto.class)))
            }
    )
    public ResponseEntity<Page<BlogPostDto>> posts(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String tag,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<BlogPostDto> posts = blogService.getPosts(tag).stream()
                .map(this::map)
                .toList();

        int start = (int) Math.min(pageable.getOffset(), posts.size());
        int end = Math.min(start + pageable.getPageSize(), posts.size());
        Page<BlogPostDto> body = new PageImpl<>(posts.subList(start, end), pageable, posts.size());

        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping("/posts/{slug}")
    @Operation(
            summary = "Get blog post",
            description = "Return a single blog post identified by its slug.",
            parameters = {
                    @Parameter(name = "slug", in = ParameterIn.PATH, required = true, description = "Post slug"),
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Post found",
                            
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BlogPostDto.class))),
                    @ApiResponse(responseCode = "404", description = "Post not found")
            }
    )
    public ResponseEntity<BlogPostDto> post(@PathVariable String slug,
                                            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        BlogPost post = blogService.getPostsByUrl().get(slug);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(map(post));
    }

    @GetMapping("/tags")
    @Operation(
            summary = "List blog tags",
            description = "Return the list of available blog tags with post counts.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tags returned",
                            
                            content = @Content(mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = BlogTagDto.class))))
            }
    )
    public ResponseEntity<List<BlogTagDto>> tags(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage) {
        List<BlogTagDto> body = blogService.getTags().entrySet().stream()
                .map(e -> new BlogTagDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .cacheControl(ONE_HOUR_PUBLIC_CACHE)
                .body(body);
    }

    @GetMapping(value = "/rss", produces = "application/rss+xml")
    @Operation(
            summary = "Blog RSS feed",
            description = "Return an RSS feed for all blog posts.",
            parameters = {
                    @Parameter(name = "domainLanguage", in = ParameterIn.QUERY, required = true,
                            description = "Language driving localisation of textual fields (future use).",
                            schema = @Schema(implementation = DomainLanguage.class))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Feed returned",
                            
                            content = @Content(mediaType = "application/rss+xml"))
            }
    )
    public ResponseEntity<String> rss(@RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                      Locale locale) throws FeedException {
        String feed = blogService.rss(locale == null ? Locale.getDefault().getLanguage() : locale.getLanguage());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(feed);
    }

    private BlogPostDto map(BlogPost post) {
        return new BlogPostDto(
                post.getUrl(),
                post.getTitle(),
                post.getAuthor(),
                post.getSummary(),
                post.getBody(),
                post.getCategory(),
                post.getImage(),
                post.getEditLink(),
                post.getCreated() == null ? null : post.getCreated().getTime(),
                post.getModified() == null ? null : post.getModified().getTime()
        );
    }
}
