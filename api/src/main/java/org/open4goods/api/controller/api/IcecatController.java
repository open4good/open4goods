
package org.open4goods.api.controller.api;

import java.util.Map;
import java.util.Optional;

import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Admin endpoints for Icecat reference data browsing and vertical category matching.
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
@Profile("!beta")
public class IcecatController {

	private final IcecatService icecatService;
	private final VerticalsConfigService verticalsService;
	private final IcecatIndexService icecatIndexService;

	public IcecatController(IcecatService icecatService, VerticalsConfigService verticalsService,
			IcecatIndexService icecatIndexService) {
		this.icecatService = icecatService;
		this.verticalsService = verticalsService;
		this.icecatIndexService = icecatIndexService;
	}

	@GetMapping("/feature/resolve")
	@Operation(summary = "Resolve the icecat feature id and return the English name if an unambiguous match is found")
	public String getOriginalEnglishName(@RequestParam String name, @RequestParam String vertical) {
		VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
		return icecatService.getOriginalEnglishName(name, vc);
	}

	@GetMapping("/{vertical}/featuregroups/")
	@Operation(summary = "Load the list of features aggregated by UiFeatureGroup")
	public Map<String, String> getFeaturesGroup(@PathVariable String vertical) {
		VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
		return icecatService.types(vc);
	}

	@GetMapping("/features/{featuresId}/")
	@Operation(summary = "Load the Feature for a given id")
	public IcecatFeature getFeature(@PathVariable Integer featuresId) {
		return icecatService.getFeaturesById().get(featuresId);
	}

	// -------------------------------------------------------------------------
	// Category admin endpoints
	// -------------------------------------------------------------------------

	@GetMapping("/icecat/categories")
	@Operation(summary = "List all Icecat categories from the Elasticsearch index")
	public Iterable<IcecatCategoryDocument> getAllCategories() {
		return icecatIndexService.findAllCategories();
	}

	@GetMapping("/icecat/categories/search")
	@Operation(summary = "Search Icecat categories by English name")
	public Page<IcecatCategoryDocument> searchCategories(
			@RequestParam String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return icecatIndexService.searchCategories(q, PageRequest.of(page, size));
	}

	@GetMapping("/icecat/categories/{id}")
	@Operation(summary = "Get a single Icecat category by ID")
	public ResponseEntity<IcecatCategoryDocument> getCategory(@PathVariable Integer id) {
		return icecatIndexService.findCategory(id)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/icecat/categories/{id}/features")
	@Operation(summary = "Search Icecat features by English name (scoped to context of category)")
	public Page<IcecatFeatureDocument> searchFeaturesByCategory(
			@PathVariable Integer id,
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return icecatIndexService.searchFeatures(q, PageRequest.of(page, size));
	}

	@PostMapping("/icecat/vertical/{verticalId}/category/{catId}")
	@Operation(summary = "Assign an Icecat category to a vertical (returns the resolved vertical config)")
	public ResponseEntity<VerticalConfig> assignCategoryToVertical(
			@PathVariable String verticalId,
			@PathVariable Integer catId) {
		VerticalConfig vc = verticalsService.getConfigById(verticalId);
		if (vc == null) {
			return ResponseEntity.notFound().build();
		}
		vc.setIcecatTaxonomyId(catId);
		return ResponseEntity.ok(vc);
	}

	// -------------------------------------------------------------------------
	// Index management endpoints
	// -------------------------------------------------------------------------

	@GetMapping("/icecat/index/sync")
	@Operation(summary = "Trigger a re-sync of Icecat reference data from in-memory loaders to Elasticsearch")
	public Map<String, String> syncIndex() {
		icecatIndexService.syncFromLoaders();
		long[] counts = icecatIndexService.indexCounts();
		return Map.of(
				"features", String.valueOf(counts[0]),
				"categories", String.valueOf(counts[1]),
				"featureGroups", String.valueOf(counts[2]),
				"suppliers", String.valueOf(counts[3]));
	}

	@GetMapping("/icecat/index/counts")
	@Operation(summary = "Return the document counts for all Icecat Elasticsearch indexes")
	public Map<String, Long> indexCounts() {
		long[] counts = icecatIndexService.indexCounts();
		return Map.of(
				"features", counts[0],
				"categories", counts[1],
				"featureGroups", counts[2],
				"suppliers", counts[3]);
	}
}
