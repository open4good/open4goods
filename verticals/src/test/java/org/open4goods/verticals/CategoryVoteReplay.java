package org.open4goods.verticals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.vertical.VerticalConfig;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Before/after replay of the vote between datasources (GOU-2256, GOU-2317) on an export of indexed products, without
 * Elasticsearch or network: for each product, the vertical given by the historical rule, the vertical given by the
 * vote, and, when they differ, the populations (a) to (d) of {@code verticals/README.md} the product belongs to.
 *
 * <p>Input: a JSON object {@code id -> {"categoriesByDatasources": {datasource: category}, "eprelGroup": "..."}}, the
 * {@code categoriesByDatasources} and {@code eprelDatas.productGroup} of the indexed product ({@code eprelGroup} may be
 * missing or null; other fields are ignored). The map keeps the order of the export: the historical rule takes the
 * first matching category, so its result depends on that order when several verticals match (population (c)).
 *
 * <p>Populations, computed on the raw categories as the vote reads them (markers searched without removing the
 * {@code natureNeutralCategoryFragments}). A product may belong to several; a difference outside all four is
 * "unclassified", which the README rules out:
 * <ul>
 * <li>(a) a historical vertical and a category holding a {@code sparePartCategoryMarkers} or
 * {@code accessoryCategoryMarkers} marker that is not an excluding token of that vertical;</li>
 * <li>(b) no historical vertical, no candidate confirmed by the EPREL group, a matched category, and a category holding
 * an excluding token of a matched vertical;</li>
 * <li>(c) categories matching at least two verticals;</li>
 * <li>(d) an EPREL group belonging to a vertical that no category matches, while a category matches another one.</li>
 * </ul>
 *
 * <p>Used by {@code CategoryVoteReplayTest}: on the versioned sample in CI, and on an export given by
 * {@code -Dcategoryvote.replay.input} (procedure in {@code verticals/README.md}).
 */
final class CategoryVoteReplay {

	/** Row label of the products without vertical. */
	static final String NO_VERTICAL = "(none)";

	private static final JsonMapper EXPORT_READER = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	private final VerticalsConfigService service;

	/**
	 * One exported product.
	 *
	 * @param categoriesByDatasources category by datasource name, in the order of the export
	 * @param eprelGroup              EPREL product group, or null
	 */
	record ExportedProduct(Map<String, String> categoriesByDatasources, String eprelGroup) {
	}

	/** Populations of {@code verticals/README.md} where the vote may differ from the historical rule. */
	enum Population {
		A("(a)"), B("(b)"), C("(c)"), D("(d)");

		private final String label;

		Population(String label) {
			this.label = label;
		}

		String label() {
			return label;
		}
	}

	/**
	 * Result of one product.
	 *
	 * @param id                 product id of the export
	 * @param historical         vertical id given by the historical rule, or null
	 * @param vote               vertical id given by the vote, or null
	 * @param populations        populations the product belongs to, whatever the results
	 * @param eprelConfirmedLost true when the EPREL group confirms the historical vertical and the vote does not give it
	 */
	record ProductResult(String id, String historical, String vote, Set<Population> populations,
			boolean eprelConfirmedLost) {

		boolean differs() {
			return !Objects.equals(historical, vote);
		}

		/** @return true for a difference outside the four populations */
		boolean unclassified() {
			return differs() && populations.isEmpty();
		}
	}

	/**
	 * Counts of one vertical. A difference counts in the row of the vertical it leaves and in the row of the vertical
	 * it reaches, and in every population it belongs to.
	 */
	static final class VerticalCounts {
		int historical;
		int vote;
		int lost;
		int gained;
		final Map<Population, Integer> differencesByPopulation = new TreeMap<>();
		int unclassified;

		int population(Population population) {
			return differencesByPopulation.getOrDefault(population, 0);
		}

		private void countDifference(ProductResult result) {
			result.populations().forEach(p -> differencesByPopulation.merge(p, 1, Integer::sum));
			if (result.unclassified()) {
				unclassified++;
			}
		}
	}

	/**
	 * Report of a replay.
	 *
	 * @param products    every product result, in the order of the export
	 * @param byVertical  counts by vertical id, {@link #NO_VERTICAL} for the products without vertical
	 * @param unreadable  ids whose categories could not be replayed, with the error
	 */
	record Report(List<ProductResult> products, SortedMap<String, VerticalCounts> byVertical,
			Map<String, String> unreadable) {

		List<ProductResult> differences() {
			return products.stream().filter(ProductResult::differs).toList();
		}

		long unclassified() {
			return products.stream().filter(ProductResult::unclassified).count();
		}

		List<ProductResult> eprelConfirmedLosses() {
			return products.stream().filter(ProductResult::eprelConfirmedLost).toList();
		}
	}

	/**
	 * @param service the vertical configurations, with {@code categoryVoteEnabled} off so that
	 *                {@link VerticalsConfigService#getVerticalForCategories(Map, String)} gives the historical rule
	 * @throws IllegalArgumentException when the vote is enabled on the service
	 */
	CategoryVoteReplay(VerticalsConfigService service) {
		this.service = Objects.requireNonNull(service, "service");
		if (service.isCategoryVoteEnabled()) {
			throw new IllegalArgumentException(
					"The historical side needs categoryVoteEnabled: false on the service of the replay");
		}
	}

	/**
	 * Reads an export.
	 *
	 * @param input JSON object {@code id -> {categoriesByDatasources, eprelGroup}}
	 * @return the products by id, in the order of the export
	 * @throws IOException when the input cannot be read
	 */
	static Map<String, ExportedProduct> readExport(InputStream input) throws IOException {
		Map<String, ExportedProduct> products = EXPORT_READER.readValue(input,
				new TypeReference<LinkedHashMap<String, ExportedProduct>>() {
				});
		return products == null ? Map.of() : products;
	}

	/**
	 * Replays the historical rule and the vote on every product.
	 *
	 * @param export products by id
	 * @return the report
	 */
	Report replay(Map<String, ExportedProduct> export) {
		List<ProductResult> results = new ArrayList<>();
		Map<String, String> unreadable = new LinkedHashMap<>();
		for (Entry<String, ExportedProduct> entry : export.entrySet()) {
			try {
				results.add(replay(entry.getKey(), entry.getValue()));
			} catch (RuntimeException e) {
				unreadable.put(entry.getKey(), e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}

		SortedMap<String, VerticalCounts> byVertical = new TreeMap<>();
		for (ProductResult result : results) {
			VerticalCounts historical = byVertical.computeIfAbsent(label(result.historical()), k -> new VerticalCounts());
			historical.historical++;
			VerticalCounts vote = byVertical.computeIfAbsent(label(result.vote()), k -> new VerticalCounts());
			vote.vote++;
			if (result.differs()) {
				historical.lost++;
				historical.countDifference(result);
				vote.gained++;
				vote.countDifference(result);
			}
		}
		return new Report(List.copyOf(results), Collections.unmodifiableSortedMap(byVertical),
				Collections.unmodifiableMap(unreadable));
	}

	/**
	 * Replays one product.
	 *
	 * @param id      product id
	 * @param product exported product
	 * @return its result
	 */
	ProductResult replay(String id, ExportedProduct product) {
		Map<String, String> categories = product.categoriesByDatasources() == null ? Map.of()
				: product.categoriesByDatasources();
		String eprelGroup = product.eprelGroup();

		VerticalConfig historical = service.getVerticalForCategories(categories, eprelGroup);
		VerticalConfig vote = service.getVerticalByCategoryVote(categories, eprelGroup);
		Set<Population> populations = populations(categories, eprelGroup, historical);
		boolean eprelConfirmedLost = historical != null && service.isConfirmedByEprel(historical, eprelGroup)
				&& (vote == null || !historical.getId().equals(vote.getId()));
		return new ProductResult(id, historical == null ? null : historical.getId(), vote == null ? null : vote.getId(),
				Collections.unmodifiableSet(populations), eprelConfirmedLost);
	}

	private Set<Population> populations(Map<String, String> categories, String eprelGroup, VerticalConfig historical) {
		List<String> rows = new ArrayList<>();
		Set<VerticalConfig> candidates = new LinkedHashSet<>();
		for (Entry<String, String> entry : categories.entrySet()) {
			if (StringUtils.isBlank(entry.getValue())) {
				continue;
			}
			rows.add(entry.getValue());
			VerticalConfig matched = service.matchingVertical(entry.getKey(), entry.getValue());
			if (matched != null) {
				candidates.add(matched);
			}
		}

		Set<Population> populations = EnumSet.noneOf(Population.class);
		if (historical != null && rows.stream().anyMatch(row -> service.carriesNatureMarker(row)
				&& !VerticalsConfigService.containsAny(row, historical.getExcludingTokensFromCategoriesMatching()))) {
			populations.add(Population.A);
		}
		if (historical == null && !candidates.isEmpty()
				&& candidates.stream().noneMatch(candidate -> service.isConfirmedByEprel(candidate, eprelGroup))
				&& rows.stream().anyMatch(row -> candidates.stream().anyMatch(candidate -> VerticalsConfigService
						.containsAny(row, candidate.getExcludingTokensFromCategoriesMatching())))) {
			populations.add(Population.B);
		}
		if (candidates.size() > 1) {
			populations.add(Population.C);
		}
		if (!candidates.isEmpty() && StringUtils.isNotBlank(eprelGroup)
				&& candidates.stream().noneMatch(candidate -> service.isConfirmedByEprel(candidate, eprelGroup))
				&& service.getConfigs().values().stream()
						.anyMatch(vertical -> service.isConfirmedByEprel(vertical, eprelGroup))) {
			populations.add(Population.D);
		}
		return populations;
	}

	private static String label(String vertical) {
		return vertical == null ? NO_VERTICAL : vertical;
	}

	/**
	 * Renders a report as Markdown, ready to paste in the activation PR.
	 *
	 * @param report the report
	 * @param source what was replayed, shown in the title line
	 * @return the Markdown text
	 */
	static String toMarkdown(Report report, String source) {
		StringBuilder md = new StringBuilder();
		md.append("# Category vote replay\n\n");
		md.append("Input: ").append(source).append(". Products: ").append(report.products().size())
				.append(". Unreadable: ").append(report.unreadable().size()).append(". Differences: ")
				.append(report.differences().size()).append(" (unclassified: ").append(report.unclassified())
				.append("). EPREL-confirmed products losing their historical vertical: ")
				.append(report.eprelConfirmedLosses().size()).append(".\n\n");

		md.append("## By vertical\n\n");
		md.append("A difference counts in the row of the vertical it leaves and in the row of the vertical it reaches, ")
				.append("and in every population it belongs to.\n\n");
		md.append("| Vertical | Historical | Vote | Lost | Gained | (a) | (b) | (c) | (d) | Unclassified |\n");
		md.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
		report.byVertical().forEach((vertical, counts) -> {
			md.append("| ").append(vertical).append(" | ").append(counts.historical).append(" | ").append(counts.vote)
					.append(" | ").append(counts.lost).append(" | ").append(counts.gained);
			for (Population population : Population.values()) {
				md.append(" | ").append(counts.population(population));
			}
			md.append(" | ").append(counts.unclassified).append(" |\n");
		});

		md.append("\n## Changes\n\n");
		md.append("| Historical | Vote | Products | (a) | (b) | (c) | (d) | Unclassified |\n");
		md.append("|---|---|---:|---:|---:|---:|---:|---:|\n");
		Map<String, List<ProductResult>> changes = new TreeMap<>();
		for (ProductResult result : report.differences()) {
			changes.computeIfAbsent(label(result.historical()) + " | " + label(result.vote()), k -> new ArrayList<>())
					.add(result);
		}
		changes.forEach((change, results) -> {
			md.append("| ").append(change).append(" | ").append(results.size());
			for (Population population : Population.values()) {
				md.append(" | ").append(results.stream().filter(r -> r.populations().contains(population)).count());
			}
			md.append(" | ").append(results.stream().filter(ProductResult::unclassified).count()).append(" |\n");
		});

		md.append("\n## Differences\n\n");
		md.append("| Id | Historical | Vote | Populations |\n");
		md.append("|---|---|---|---|\n");
		for (ProductResult result : report.differences()) {
			md.append("| ").append(result.id()).append(" | ").append(label(result.historical())).append(" | ")
					.append(label(result.vote())).append(" | ").append(populationLabels(result)).append(" |\n");
		}

		if (!report.eprelConfirmedLosses().isEmpty()) {
			md.append("\n## EPREL-confirmed products losing their historical vertical\n\n");
			report.eprelConfirmedLosses().forEach(r -> md.append("- ").append(r.id()).append('\n'));
		}
		if (!report.unreadable().isEmpty()) {
			md.append("\n## Unreadable products\n\n");
			report.unreadable().forEach((id, error) -> md.append("- ").append(id).append(": ").append(error)
					.append('\n'));
		}
		return md.toString();
	}

	private static String populationLabels(ProductResult result) {
		if (result.populations().isEmpty()) {
			return "unclassified";
		}
		return String.join(" ", result.populations().stream().map(Population::label).toList());
	}
}
