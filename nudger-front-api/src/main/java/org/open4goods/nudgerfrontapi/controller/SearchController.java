package org.open4goods.nudgerfrontapi.controller;

import java.util.HashSet;

import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.nudgerfrontapi.dto.SearchRequest;
import org.open4goods.nudgerfrontapi.dto.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public SearchResponse search(SearchRequest request) {
        VerticalSearchResponse resp = searchService.globalSearch(
                request.query(),
                request.fromPrice(),
                request.toPrice(),
                request.categories() == null ? null : new HashSet<>(request.categories()),
                request.condition() == null ? null : ProductCondition.valueOf(request.condition()),
                request.page() == null ? 0 : request.page(),
                request.size() == null ? 10 : request.size(),
                0,
                request.sort());
        return new SearchResponse(resp.getTotalResults(),
                request.page() == null ? 0 : request.page(),
                request.size() == null ? 10 : request.size(),
                resp.getData());
    }
}
