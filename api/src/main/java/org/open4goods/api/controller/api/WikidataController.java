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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin endpoints for Wikidata entity browsing and on-demand product completion.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Profile("!beta")
@Tag(name = "Wikidata", description = "Browse the local Wikidata entity cache, run raw SPARQL queries against Wikidata, "
        + "and trigger on-demand Wikidata completion for individual products. "
        + "Not active in the beta profile.")
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
            description = "Returns the WikidataEntity for the given Q-id from the local Elasticsearch cache. "
                    + "On a cache miss the entity is fetched from the live Wikidata API, stored and returned. "
                    + "Use this to inspect the enrichment data that will be applied to a product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "WikidataEntity found and returned"),
            @ApiResponse(responseCode = "404", description = "No entity found for the given Q-id, even after a live lookup")
    })
    public ResponseEntity<WikidataEntity> getEntity(
            @Parameter(description = "Wikidata Q-id of the entity to retrieve (e.g. 'Q42')", required = true)
            @PathVariable String qid) {
        return lookupService.fetchByQid(qid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wikidata/search/gtin/{gtin}")
    @Operation(
            summary = "Search Wikidata for a product by GTIN (debug)",
            description = "Executes a live SPARQL lookup on the Wikidata public endpoint for the given GTIN. "
                    + "This is a diagnostic endpoint useful for debugging cases where the local cache "
                    + "does not contain the expected entity. The result is NOT cached.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wikidata entity found for the GTIN"),
            @ApiResponse(responseCode = "404", description = "No Wikidata entity found for the GTIN")
    })
    public ResponseEntity<WikidataEntity> searchByGtin(
            @Parameter(description = "GTIN (EAN-13) to look up on Wikidata via live SPARQL", required = true)
            @PathVariable String gtin) {
        return searchService.searchByGtin(gtin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wikidata/sparql")
    @Operation(
            summary = "Execute a raw SPARQL query against Wikidata",
            description = "Sends the supplied SPARQL SELECT query to the Wikidata public SPARQL endpoint and returns "
                    + "the result rows as a list of variable-to-value maps. "
                    + "Intended for admin debugging and taxonomy exploration. "
                    + "Complex or long-running queries may time out on the Wikidata side.")
    @ApiResponse(responseCode = "200", description = "List of result rows; each row is a map of SPARQL variable name to value string")
    public List<Map<String, String>> executeSparql(
            @Parameter(description = "SPARQL SELECT query to execute against the Wikidata endpoint", required = true)
            @RequestParam String query) {
        return searchService.executeSparql(query);
    }

    @PostMapping("/wikidata/complete/{gtin}")
    @Operation(
            summary = "Force Wikidata completion for a single product",
            description = "Fetches the product from Elasticsearch, runs the full Wikidata completion pipeline "
                    + "(entity lookup, attribute extraction and enrichment), then re-indexes the updated product. "
                    + "The vertical parameter is optional: when omitted or set to 'default', the product's own "
                    + "vertical is used to resolve the VerticalConfig.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Wikidata completion triggered; response contains the Q-id assigned to the product"),
            @ApiResponse(responseCode = "404", description = "No product with the given GTIN found in the index")
    })
    public ResponseEntity<String> completeProduct(
            @Parameter(description = "GTIN (EAN-13) of the product to enrich via Wikidata", required = true)
            @PathVariable String gtin,
            @Parameter(description = "Vertical identifier used to resolve the VerticalConfig; defaults to 'default'")
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
    @Operation(
            summary = "Return the Wikidata entity index document count",
            description = "Returns the total number of WikidataEntity documents currently stored in the local "
                    + "Elasticsearch Wikidata index. Useful for monitoring cache growth.")
    @ApiResponse(responseCode = "200", description = "Map containing the 'wikidataEntities' count")
    public Map<String, Long> indexCount() {
        return Map.of("wikidataEntities", lookupService.indexCount());
    }
}
