package org.open4goods.services.exposeddocs.controller;

import java.util.List;

import org.open4goods.services.exposeddocs.dto.ExposedDocsContentDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsOverviewDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsSearchResultDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsTreeNodeDto;
import org.open4goods.services.exposeddocs.service.ExposedDocsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing embedded documentation and prompt resources.
 */
@RestController
@RequestMapping("/exposed")
@ConditionalOnProperty(prefix = "exposed-docs", name = "controller-enabled", havingValue = "true", matchIfMissing = true)
public class ExposedDocsController
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedDocsController.class);

    private final ExposedDocsService service;

    public ExposedDocsController(ExposedDocsService service)
    {
        this.service = service;
    }

    /**
     * Lists the available resource categories.
     *
     * @return overview of exposed categories
     */
    @GetMapping
    public ResponseEntity<ExposedDocsOverviewDto> getOverview()
    {
        LOGGER.info("Listing exposed documentation categories");
        return ResponseEntity.ok(service.getOverview());
    }

    /**
     * Returns the tree view for a single category.
     *
     * @param categoryId category identifier
     * @return hierarchical tree for the category
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<ExposedDocsTreeNodeDto> getTree(@PathVariable String categoryId)
    {
        LOGGER.info("Listing exposed docs tree for category {}", categoryId);
        return ResponseEntity.ok(service.getCategoryTree(categoryId));
    }

    /**
     * Retrieves the content of a given resource path.
     *
     * @param categoryId category identifier
     * @param path resource path relative to the category root
     * @return content of the resource
     */
    @GetMapping("/{categoryId}/content")
    public ResponseEntity<ExposedDocsContentDto> getContent(@PathVariable String categoryId,
                                                            @RequestParam("path") String path)
    {
        LOGGER.info("Fetching exposed docs content for category {} and path {}", categoryId, path);
        return ResponseEntity.ok(service.getContent(categoryId, path));
    }

    /**
     * Searches embedded resources by path and optional content matching.
     *
     * @param query search query
     * @param categories optional categories to search
     * @param pathPrefix optional path prefix filter
     * @param searchContent whether to search file content
     * @param includeContent whether to include file content in the response
     * @return list of matching resources
     */
    @GetMapping("/search")
    public ResponseEntity<List<ExposedDocsSearchResultDto>> search(@RequestParam(value = "query", required = false) String query,
                                                                   @RequestParam(value = "categories", required = false) List<String> categories,
                                                                   @RequestParam(value = "pathPrefix", required = false) String pathPrefix,
                                                                   @RequestParam(value = "searchContent", defaultValue = "false") boolean searchContent,
                                                                   @RequestParam(value = "includeContent", defaultValue = "false") boolean includeContent)
    {
        LOGGER.info("Searching exposed docs with query '{}'", query);
        return ResponseEntity.ok(service.search(query, categories, pathPrefix, searchContent, includeContent));
    }
}
