package org.open4goods.api.controller.api;

import java.util.List;
import java.util.Map;

import org.open4goods.api.services.completion.WikidataCompletionService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.open4goods.services.wikidataservice.service.WikidataLookupService;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Admin endpoints for Wikidata entity browsing and on-demand product completion.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Profile("!beta")
public class WikidataController {

    private final WikidataLookupService lookupService;
    private final WikidataSearchService searchService;
    private final WikidataCompletionService completionService;
    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsService;

    public WikidataController(
            WikidataLookupService lookupService,
            WikidataSearchService searchService,
            WikidataCompletionService completionService,
            ProductRepository productRepository,
            VerticalsConfigService verticalsService) {
        this.lookupService = lookupService;
        this.searchService = searchService;
        this.completionService = completionService;
        this.productRepository = productRepository;
        this.verticalsService = verticalsService;
    }

    @GetMapping("/wikidata/entity/{qid}")
    @Operation(
            summary = "Fetch a Wikidata entity by Q-id",
            description = "Returns the cached WikidataEntity; fetches from Wikidata and caches on miss.")
    public ResponseEntity<WikidataEntity> getEntity(@PathVariable String qid) {
        return lookupService.fetchByQid(qid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wikidata/search/gtin/{gtin}")
    @Operation(
            summary = "Search Wikidata by GTIN (debug)",
            description = "Performs a live SPARQL lookup for the given GTIN. Useful for debugging lookup misses.")
    public ResponseEntity<WikidataEntity> searchByGtin(@PathVariable String gtin) {
        return searchService.searchByGtin(gtin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wikidata/sparql")
    @Operation(
            summary = "Execute a raw SPARQL query against Wikidata",
            description = "Returns the result rows as a list of variable-to-value maps. Admin/debug only.")
    public List<Map<String, String>> executeSparql(@RequestParam String query) {
        return searchService.executeSparql(query);
    }

    @PostMapping("/wikidata/complete/{gtin}")
    @Operation(
            summary = "Force Wikidata completion for a single product by GTIN",
            description = "Looks up the product in Elasticsearch, runs Wikidata completion, and re-indexes it.")
    public ResponseEntity<String> completeProduct(
            @PathVariable String gtin,
            @RequestParam(defaultValue = "default") String vertical) {
        Product product;
        try {
            product = productRepository.getById(Long.parseLong(gtin));
        } catch (ResourceNotFoundException | NumberFormatException e) {
            return ResponseEntity.notFound().build();
        }
        VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
        completionService.completeAndIndexProduct(vc, product);
        return ResponseEntity.ok("Wikidata completion triggered for " + gtin
                + ". Q-id: " + product.getExternalIds().getWikidata());
    }

    @GetMapping("/wikidata/index/count")
    @Operation(summary = "Return the document count in the Wikidata entity Elasticsearch index")
    public Map<String, Long> indexCount() {
        return Map.of("wikidataEntities", lookupService.indexCount());
    }
}
