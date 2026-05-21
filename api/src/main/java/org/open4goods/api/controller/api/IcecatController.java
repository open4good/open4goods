
package org.open4goods.api.controller.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.open4goods.api.dto.IcecatCategoryAttributesDto;
import org.open4goods.api.dto.IcecatCategoryCandidateDto;
import org.open4goods.api.dto.IcecatCategoryAttributesDto.IcecatCategoryAttributeDto;
import org.open4goods.api.dto.IcecatCategoryAttributesDto.IcecatCategoryFeatureGroupDto;
import org.open4goods.icecat.model.IcecatCategoryFeatureDocument;
import org.open4goods.icecat.model.IcecatCategoryFeatureGroupDocument;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.services.IcecatFeatureResolver;
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
	private final IcecatFeatureResolver icecatFeatureResolver;

	public IcecatController(IcecatService icecatService, VerticalsConfigService verticalsService,
			IcecatIndexService icecatIndexService, IcecatFeatureResolver icecatFeatureResolver) {
		this.icecatService = icecatService;
		this.verticalsService = verticalsService;
		this.icecatIndexService = icecatIndexService;
		this.icecatFeatureResolver = icecatFeatureResolver;
	}

	@GetMapping("/feature/resolve")
	@Operation(summary = "Resolve the icecat feature id and return the English name if an unambiguous match is found")
	public String getOriginalEnglishName(@RequestParam String name, @RequestParam String vertical) {
		VerticalConfig vc = verticalsService.getConfigByIdOrDefault(vertical);
		return icecatFeatureResolver.getOriginalEnglishName(name, vc);
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

	@GetMapping("/icecat/verticals/{verticalId}/candidate-categories")
	@Operation(
			summary = "Candidate Icecat categories for a vertical",
			description = "Returns Icecat category candidates from the Elasticsearch category index. "
					+ "The configured vertical Icecat category is returned first when present, "
					+ "then candidates found from vertical names."
	)
	public ResponseEntity<List<IcecatCategoryCandidateDto>> candidateCategoriesForVertical(
			@PathVariable String verticalId,
			@RequestParam(defaultValue = "20") int size) {
		VerticalConfig vc = verticalsService.getConfigById(verticalId);
		if (vc == null) {
			return ResponseEntity.notFound().build();
		}

		Map<Integer, IcecatCategoryCandidateDto> candidates = new LinkedHashMap<>();
		if (vc.getIcecatTaxonomyId() != null) {
			icecatIndexService.findCategory(vc.getIcecatTaxonomyId())
					.map(category -> toCandidate(category, "configured"))
					.ifPresent(candidate -> candidates.put(candidate.id(), candidate));
		}

		for (String term : verticalSearchTerms(vc)) {
			icecatIndexService.searchCategories(term, PageRequest.of(0, Math.max(1, size))).forEach(category -> {
				candidates.putIfAbsent(category.getId(), toCandidate(category, "search:" + term));
			});
			if (candidates.size() >= size) {
				break;
			}
		}

		return ResponseEntity.ok(candidates.values().stream().limit(size).toList());
	}

	@GetMapping("/icecat/categories/{id}/attributes")
	@Operation(
			summary = "List Icecat attributes available for a category",
			description = "Returns category-scoped attribute metadata and global Icecat feature metadata "
					+ "already stored in Elasticsearch. No live Icecat API call is made."
	)
	public ResponseEntity<IcecatCategoryAttributesDto> getCategoryAttributes(@PathVariable Integer id) {
		Optional<IcecatCategoryDocument> category = icecatIndexService.findCategory(id);
		if (category.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(toCategoryAttributes(category.get()));
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

	private IcecatCategoryCandidateDto toCandidate(IcecatCategoryDocument category, String source) {
		return new IcecatCategoryCandidateDto(
				category.getId(),
				category.getEnglishName(),
				category.getParentId(),
				category.getScore(),
				category.getLangNames(),
				source);
	}

	private Set<String> verticalSearchTerms(VerticalConfig vc) {
		Set<String> terms = new LinkedHashSet<>();
		addTerm(terms, vc.getId());
		if (vc.getId() != null) {
			addTerm(terms, vc.getId().replace('-', ' ').replace('_', ' '));
		}
		vc.getI18n().values().forEach(i18n -> {
			addTerm(terms, i18n.getCardTitle());
			addTerm(terms, i18n.getShortName());
			addTerm(terms, i18n.getLongName());
			addTerm(terms, i18n.getVerticalHomeTitle());
			addTerm(terms, i18n.getVerticalMetaTitle());
			addTerm(terms, i18n.getPrettyName().getPrefix());
			addTerm(terms, i18n.getSingular().getPrefix());
		});
		return terms;
	}

	private void addTerm(Set<String> terms, String term) {
		if (term != null && !term.isBlank()) {
			terms.add(term.trim());
		}
	}

	private IcecatCategoryAttributesDto toCategoryAttributes(IcecatCategoryDocument category) {
		Map<Integer, IcecatFeatureDocument> featureDocuments = icecatIndexService.findCategoryFeatureDocuments(category);
		List<IcecatCategoryAttributeDto> attributes = new ArrayList<>();
		List<IcecatCategoryFeatureDocument> categoryFeatures = category.getFeatures() == null
				? List.of()
				: category.getFeatures();
		for (IcecatCategoryFeatureDocument categoryFeature : categoryFeatures) {
			IcecatFeatureDocument feature = featureDocuments.get(categoryFeature.getId());
			Set<String> normalizedNames = feature == null || feature.getNormalizedNames() == null
					? Set.of()
					: feature.getNormalizedNames();
			List<String> langNames = feature == null || feature.getLangNames() == null
					? List.of()
					: feature.getLangNames();
			attributes.add(new IcecatCategoryAttributeDto(
					categoryFeature.getId(),
					feature == null ? null : feature.getEnglishName(),
					feature == null ? null : feature.getType(),
					categoryFeature.getType(),
					categoryFeature.getCategoryFeatureGroupId(),
					categoryFeature.getCategoryFeatureId(),
					categoryFeature.getNo(),
					categoryFeature.getClazz(),
					categoryFeature.getDefaultDisplayUnit(),
					categoryFeature.getLimitDirection(),
					categoryFeature.getMandatory(),
					categoryFeature.getSearchable(),
					categoryFeature.getUseDropdownInput(),
					categoryFeature.getValueSorting(),
					normalizedNames,
					langNames,
					localizedNames(langNames)));
		}
		List<IcecatCategoryFeatureGroupDto> groups = (category.getFeatureGroups() == null
				? List.<IcecatCategoryFeatureGroupDocument>of()
				: category.getFeatureGroups()).stream()
				.map(group -> new IcecatCategoryFeatureGroupDto(group.getId(), group.getFeatureGroupIds()))
				.toList();
		return new IcecatCategoryAttributesDto(category.getId(), category.getEnglishName(), groups, attributes);
	}

	private Map<String, String> localizedNames(List<String> langNames) {
		if (langNames == null) {
			return Map.of();
		}
		Map<String, String> result = new LinkedHashMap<>();
		for (String langName : langNames) {
			int separator = langName.indexOf(':');
			if (separator > 0 && separator < langName.length() - 1) {
				result.put(langName.substring(0, separator), langName.substring(separator + 1));
			}
		}
		return result;
	}
}
