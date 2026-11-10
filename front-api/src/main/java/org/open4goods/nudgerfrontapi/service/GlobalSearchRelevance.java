package org.open4goods.nudgerfrontapi.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.nudgerfrontapi.config.properties.GlobalSearchProperties;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Intervals;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Relevance rules of the global search (GOU-2029): accessory detection, long tail quality filter and ranking.
 * <p>
 * The rules only decorate the queries built for {@link SearchService#globalSearch}; the queries of
 * {@link SearchService#search} never go through this class. Accessory detection runs in Elasticsearch so that it
 * ranks the whole result set; the image and title checks run in Java on the documents read, because the index holds
 * neither {@code coverImagePath} nor an indexed image URL for products without a vertical.
 */
final class GlobalSearchRelevance {

	/** Indexed text field holding the merchant offer names. */
	static final String OFFER_NAMES_FIELD = "offerNames";
	private static final String OFFERS_COUNT_FIELD = "offersCount";
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	/** Placeholder images served by some feeds instead of a product picture. */
	private static final List<String> PLACEHOLDER_IMAGE_MARKERS = List.of("noimage", "no-image", "no_image");

	/**
	 * Source fields read to decide whether a long tail candidate is displayable: offer names, brand, images and
	 * offers. The complete product is fetched afterwards for the displayed slice only.
	 */
	private static final String[] CANDIDATE_SOURCE_FIELDS = { "id", OFFER_NAMES_FIELD, "coverImagePath",
			"resources.url", "attributes.referentielAttributes.BRAND", "price.offers", OFFERS_COUNT_FIELD, "vertical" };

	private final GlobalSearchProperties properties;
	private final OfferFreshnessService offerFreshnessService;
	/** Accessory terms, folded, each as its list of words. */
	private final List<List<String>> accessoryTerms;
	/** Words and expressions searched at the head of the offer names (folded terms and their plural). */
	private final List<String> accessoryVariants;

	GlobalSearchRelevance(GlobalSearchProperties properties, OfferFreshnessService offerFreshnessService) {
		this.properties = properties == null ? new GlobalSearchProperties() : properties;
		this.offerFreshnessService = offerFreshnessService;
		List<List<String>> terms = new ArrayList<>();
		Set<String> variants = new LinkedHashSet<>();
		for (String term : this.properties.getAccessoryTerms()) {
			List<String> words = words(term);
			if (words.isEmpty()) {
				continue;
			}
			terms.add(words);
			variants.add(String.join(" ", words));
			variants.add(String.join(" ", pluralizeFirst(words)));
		}
		this.accessoryTerms = List.copyOf(terms);
		this.accessoryVariants = List.copyOf(variants);
	}

	/**
	 * Tells whether the query asks for an accessory, in which case accessories are neither demoted nor set apart.
	 *
	 * @param query sanitized user query, may be {@code null}
	 * @return {@code true} when one of the accessory terms (or its plural) appears in the query
	 */
	boolean queryMentionsAccessory(String query) {
		List<String> queryWords = words(query);
		if (queryWords.isEmpty()) {
			return false;
		}
		for (List<String> term : accessoryTerms) {
			for (int start = 0; start + term.size() <= queryWords.size(); start++) {
				boolean matched = true;
				for (int i = 0; i < term.size() && matched; i++) {
					matched = sameWordOrPlural(queryWords.get(start + i), term.get(i));
				}
				if (matched) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Builds the query matching accessories: one accessory term (or its plural) within the first
	 * {@link GlobalSearchProperties#getAccessoryHeadWords()} words of an offer name.
	 * <p>
	 * An {@code intervals} query is used rather than {@code span_first}: it analyses each term with the field
	 * analyzer (lower case, accent folding) and its script filter keeps the intervals starting before the head limit.
	 * Positions of a multi-valued field run on from one value to the next, so only the first indexed offer name is
	 * checked; a term found further in a name ("lave-linge avec filtre") is ignored.
	 *
	 * @param query sanitized user query
	 * @return the accessory query, or {@code null} when no term is configured or the query asks for an accessory
	 */
	Query accessoryQuery(String query) {
		if (accessoryVariants.isEmpty() || queryMentionsAccessory(query)) {
			return null;
		}
		List<Intervals> matches = accessoryVariants.stream()
				.map(variant -> Intervals.of(i -> i.match(m -> m.query(variant).maxGaps(0).ordered(true))))
				.toList();
		String headFilter = "interval.start < " + Math.max(1, properties.getAccessoryHeadWords());
		return Query.of(q -> q.intervals(iq -> iq.field(OFFER_NAMES_FIELD)
				.anyOf(a -> a.intervals(matches).filter(f -> f.script(s -> s.source(src -> src.scriptString(headFilter)))))));
	}

	/**
	 * Adds the ranking signals to a global search query: a bonus for the exact expression and
	 * {@code log1p(offersCount)} added to the text score, so that at comparable relevance the product sold by more
	 * merchants comes first.
	 *
	 * @param base  filtering and matching query
	 * @param query sanitized user query, may be {@code null}
	 * @return the ranked query, or {@code base} when both signals are disabled
	 */
	Query rank(Query base, String query) {
		Query scored = base;
		if (StringUtils.hasText(query) && properties.getPhraseBoost() > 0) {
			Query phrase = Query.of(q -> q.matchPhrase(mp -> mp.field(OFFER_NAMES_FIELD).query(query)
					.slop(Math.max(0, properties.getPhraseSlop())).boost((float) properties.getPhraseBoost())));
			scored = Query.of(q -> q.bool(b -> b.must(base).should(phrase)));
		}
		if (properties.getOffersCountWeight() <= 0) {
			return scored;
		}
		Query matching = scored;
		return Query.of(q -> q.functionScore(fs -> fs.query(matching)
				.functions(f -> f.fieldValueFactor(v -> v.field(OFFERS_COUNT_FIELD)
						.modifier(FieldValueFactorModifier.Log1p).missing(0.0))
						.weight(properties.getOffersCountWeight()))
				.scoreMode(FunctionScoreMode.Sum)
				.boostMode(FunctionBoostMode.Sum)));
	}

	/**
	 * Demotes the accessories of a ranked query ({@code boosting}).
	 *
	 * @param ranked    ranked query
	 * @param accessory accessory query, may be {@code null}
	 * @return the query with accessories demoted, or {@code ranked} when {@code accessory} is {@code null}
	 */
	Query demoteAccessories(Query ranked, Query accessory) {
		if (accessory == null) {
			return ranked;
		}
		return Query.of(q -> q.boosting(b -> b.positive(ranked).negative(accessory)
				.negativeBoost(properties.getAccessoryNegativeBoost())));
	}

	/**
	 * Long tail rule: a product is displayed only with a fresh offer, an image and a readable title.
	 *
	 * @param product candidate read with {@link #candidateSourceFilter()}
	 * @return {@code true} when the product may be displayed
	 */
	boolean isDisplayable(Product product) {
		return product != null && offerFreshnessService.offersCount(product) > 0 && hasImage(product)
				&& hasReadableTitle(product);
	}

	/**
	 * Tells whether the product has a picture: a processed cover or a merchant image that is not a placeholder.
	 *
	 * @param product product to check
	 * @return {@code true} when an image can be displayed
	 */
	boolean hasImage(Product product) {
		if (StringUtils.hasText(product.getCoverImagePath())) {
			return true;
		}
		for (Resource image : product.unprocessedimages()) {
			String url = image.getUrl().toLowerCase(Locale.ROOT);
			if (PLACEHOLDER_IMAGE_MARKERS.stream().noneMatch(url::contains)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tells whether one offer name reads as a product title rather than a brand and a code: at least
	 * {@link GlobalSearchProperties#getMinTitleWords()} words, one of them alphabetic, at least
	 * {@link GlobalSearchProperties#getMinTitleWordLength()} letters long and other than the brand.
	 *
	 * @param product product to check
	 * @return {@code true} when a readable offer name exists
	 */
	boolean hasReadableTitle(Product product) {
		if (product.getOfferNames() == null) {
			return false;
		}
		Set<String> brandWords = new LinkedHashSet<>(words(brand(product)));
		for (String name : product.getOfferNames()) {
			List<String> nameWords = words(name);
			if (nameWords.size() >= properties.getMinTitleWords() && nameWords.stream()
					.anyMatch(word -> word.length() >= properties.getMinTitleWordLength()
							&& word.chars().allMatch(Character::isLetter) && !brandWords.contains(word))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Number of candidates read for one long tail page, before the Java filter.
	 *
	 * @param pageNumber zero-based page
	 * @param pageSize   displayed page size
	 * @return candidates to read from the first one, bounded by
	 *         {@link GlobalSearchProperties#getLongTailMaxCandidates()}
	 */
	int longTailCandidates(int pageNumber, int pageSize) {
		long wanted = (long) (pageNumber + 1) * pageSize * overRead();
		return (int) Math.min(wanted, maxCandidates());
	}

	/**
	 * Last long tail page that can be served, given the candidate bound.
	 *
	 * @param pageSize displayed page size
	 * @return number of reachable pages, at least 1
	 */
	int longTailReachablePages(int pageSize) {
		return (int) Math.max(1, maxCandidates() / ((long) Math.max(1, pageSize) * overRead()));
	}

	int accessoryResultsLimit() {
		return Math.max(0, properties.getAccessoryResultsLimit());
	}

	int overRead() {
		return Math.max(1, properties.getLongTailOverRead());
	}

	private int maxCandidates() {
		return Math.max(1, properties.getLongTailMaxCandidates());
	}

	/**
	 * Source filter of the candidate read: only the fields used by {@link #isDisplayable(Product)}.
	 *
	 * @return source filter
	 */
	static FetchSourceFilter candidateSourceFilter() {
		return new FetchSourceFilter(true, CANDIDATE_SOURCE_FIELDS.clone(), null);
	}

	private static String brand(Product product) {
		if (product.getAttributes() == null || product.getAttributes().getReferentielAttributes() == null) {
			return null;
		}
		return product.brand();
	}

	/**
	 * Splits a text into lower-case words without accents, as the {@code offerNames} analyzer does.
	 *
	 * @param text text, may be {@code null}
	 * @return words, empty when the text has none
	 */
	static List<String> words(String text) {
		if (!StringUtils.hasText(text)) {
			return List.of();
		}
		String folded = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("")
				.toLowerCase(Locale.ROOT);
		return Arrays.stream(NON_ALPHANUMERIC.split(folded)).filter(StringUtils::hasText).toList();
	}

	private static List<String> pluralizeFirst(List<String> words) {
		List<String> plural = new ArrayList<>(words);
		plural.set(0, plural(words.get(0)));
		return plural;
	}

	private static String plural(String word) {
		if (word.endsWith("s") || word.endsWith("x") || word.endsWith("z")) {
			return word;
		}
		return word.endsWith("eau") || word.endsWith("eu") ? word + "x" : word + "s";
	}

	private static boolean sameWordOrPlural(String word, String term) {
		return word.equals(term) || word.equals(plural(term));
	}
}
