package org.open4goods.nudgerfrontapi.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.nudgerfrontapi.config.properties.GlobalSearchProperties;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.FieldValue;
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
	/** Keyword field marking accessories and spare parts (GOU-2235). */
	static final String NATURE_FIELD = "nature";
	private static final List<String> NATURES = List.of("ACCESSORY", "SPARE_PART");
	private static final String OFFERS_COUNT_FIELD = "offersCount";
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
	/** Words that open the "of the requested object" clause: {@code de}, {@code du}, {@code des}, {@code d'}. */
	private static final List<String> OF_WORDS = List.of("de", "du", "des", "d");
	/** Words of the query that do not designate an object. */
	private static final Set<String> QUERY_STOP_WORDS = Set.of("pour", "de", "du", "des", "la", "le", "les", "et", "avec",
			"sans", "un", "une", "compatible", "compatibles");
	/** Minimum length of a single query word taken as an object term. */
	private static final int MIN_QUERY_TERM_LENGTH = 4;
	/** Gap tolerated between a part name, {@code de} and the object ({@code moteur de la lave-linge}). */
	private static final int OF_MAX_GAPS = 1;
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	/** Placeholder images served by some feeds instead of a product picture. */
	private static final List<String> PLACEHOLDER_IMAGE_MARKERS = List.of("noimage", "no-image", "no_image");
	/** Articles and prepositions skipped before the head of a name and around the object of the query. */
	private static final Set<String> HEAD_FILLER_WORDS = Set.of("a", "au", "aux", "de", "d", "du", "des", "le", "la",
			"les", "l", "un", "une", "et", "the");
	/** Parenthesised parts of a name ({@code (92212687)}), left out of the head detection. */
	private static final Pattern PARENTHESES = Pattern.compile("\\([^)]*\\)");

	/**
	 * Source fields read to decide whether a long tail candidate is displayable: offer names, brand, images and
	 * offers. The complete product is fetched afterwards for the displayed slice only.
	 */
	private static final String[] CANDIDATE_SOURCE_FIELDS = { "id", OFFER_NAMES_FIELD, "coverImagePath",
			"resources.url", "attributes.referentielAttributes.BRAND", "price.offers", OFFERS_COUNT_FIELD, "vertical",
			"datasourceCategories" };

	private final GlobalSearchProperties properties;
	private final OfferFreshnessService offerFreshnessService;
	/** Accessory terms, folded, each as its list of words. */
	private final List<List<String>> accessoryTerms;
	/** Words and expressions searched at the head of the offer names (folded terms and their plural). */
	private final List<String> accessoryVariants;
	/** Accessory expressions searched anywhere in the offer names, folded, each as its list of words. */
	private final List<List<String>> accessoryPhrases;
	/** Folded words skipped before the head of a name: modifiers and known brands (GOU-2474). */
	private final Set<String> headSkippedWords;

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
		List<List<String>> phrases = new ArrayList<>();
		for (String phrase : this.properties.getAccessoryPhrases()) {
			List<String> words = words(phrase);
			if (!words.isEmpty()) {
				phrases.add(words);
			}
		}
		this.accessoryPhrases = List.copyOf(phrases);
		Set<String> skipped = new LinkedHashSet<>();
		Stream.concat(this.properties.getHeadModifiers().stream(), this.properties.getKnownBrands().stream())
				.map(GlobalSearchRelevance::words).forEach(skipped::addAll);
		this.headSkippedWords = Set.copyOf(skipped);
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
		for (List<String> term : Stream.concat(accessoryTerms.stream(), accessoryPhrases.stream()).toList()) {
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
		if ((accessoryVariants.isEmpty() && accessoryPhrases.isEmpty()) || queryMentionsAccessory(query)) {
			return null;
		}
		List<Intervals> matches = accessoryVariants.stream().map(GlobalSearchRelevance::phrase).toList();
		String headFilter = "interval.start < " + Math.max(1, properties.getAccessoryHeadWords());
		Intervals head = Intervals.of(i -> i.anyOf(a -> a.intervals(matches)
				.filter(f -> f.script(s -> s.source(src -> src.scriptString(headFilter))))));
		List<Intervals> clauses = new ArrayList<>();
		if (!accessoryVariants.isEmpty()) {
			clauses.add(head);
		}
		accessoryPhrases.forEach(words -> clauses.add(phrase(String.join(" ", words))));
		List<String> queryWords = words(query);
		List<String> objectTerms = objectTerms(queryWords);
		if (!queryWords.contains("pour")) {
			objectTerms.forEach(term -> clauses.add(phrase("pour " + term)));
		}
		if (!queryWords.contains("de") && !queryWords.contains("du") && !queryWords.contains("des")) {
			Intervals partNames = Intervals.of(i -> i.anyOf(a -> a.intervals(matches)));
			Intervals ofWords = Intervals.of(i -> i.anyOf(a -> a.intervals(OF_WORDS.stream()
					.map(GlobalSearchRelevance::phrase).toList())));
			for (String term : objectTerms) {
				clauses.add(Intervals.of(i -> i.allOf(a -> a.ordered(true).maxGaps(OF_MAX_GAPS)
						.intervals(List.of(partNames, ofWords, phrase(term))))));
			}
		}
		if (!queryWords.contains("compatible") && !queryWords.contains("compatibles")) {
			clauses.add(phrase("compatible"));
			clauses.add(phrase("compatibles"));
		}
		return Query.of(q -> q.intervals(iq -> iq.field(OFFER_NAMES_FIELD).anyOf(a -> a.intervals(clauses))));
	}

	/**
	 * Accessory query of the long tail (products without a vertical): the name rules of
	 * {@link #accessoryQuery(String)} or a marked {@code nature} (GOU-2235). The {@code nature} clause is kept out of
	 * {@link #accessoryQuery(String)} so that a product of a vertical is never demoted in its own group because of it.
	 *
	 * @param query sanitized user query
	 * @return the long tail accessory query, or {@code null} when the query asks for an accessory
	 */
	Query longTailAccessoryQuery(String query) {
		if (queryMentionsAccessory(query)) {
			return null;
		}
		Query nature = Query.of(q -> q.terms(t -> t.field(NATURE_FIELD)
				.terms(v -> v.value(NATURES.stream().map(FieldValue::of).toList()))));
		Query names = accessoryQuery(query);
		if (names == null) {
			return nature;
		}
		return Query.of(q -> q.bool(b -> b.should(names).should(nature).minimumShouldMatch("1")));
	}

	/**
	 * Java statement of the accessory rules held by {@link #accessoryQuery(String)}, applied to one offer name:
	 * an accessory term at the head of the name, {@code pour <object>}, {@code <part> de <object>} or the word
	 * {@code compatible}, the last two only when the query does not use those words itself. Accents, case and
	 * hyphens are ignored ({@code lave-linge} = {@code lave linge}).
	 *
	 * @param query     sanitized user query
	 * @param offerName offer name of the product
	 * @return {@code true} when the name reads as an accessory of the requested object
	 */
	boolean isAccessoryName(String query, String offerName) {
		List<String> name = words(offerName);
		List<String> queryWords = words(query);
		if (name.isEmpty() || queryMentionsAccessory(query)) {
			return false;
		}
		for (List<String> phrase : accessoryPhrases) {
			if (indexOf(name, phrase, 0, false) >= 0) {
				return true;
			}
		}
		int headLimit = Math.max(1, properties.getAccessoryHeadWords());
		for (List<String> term : accessoryTerms) {
			for (int start = 0; start < Math.min(headLimit, name.size()); start++) {
				if (matchesAt(name, start, term, true)) {
					return true;
				}
			}
		}
		if (!queryWords.contains("compatible") && !queryWords.contains("compatibles")
				&& (name.contains("compatible") || name.contains("compatibles"))) {
			return true;
		}
		for (List<String> object : objectTerms(queryWords).stream().map(GlobalSearchRelevance::words).toList()) {
			if (!queryWords.contains("pour") && indexOfPour(name, object)) {
				return true;
			}
			if (!queryWords.contains("de") && !queryWords.contains("du") && !queryWords.contains("des")
					&& partNameOfObject(name, object)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Long tail rule relative to the query (GOU-2474): the product is set apart when one of its offer names holds the
	 * object of the query after its head ({@code Ressort Lave-linge - Twinstar} for {@code lave-linge}) and none
	 * starts with it. See {@link #isForeignHeadName(String, String, String)}. Categorized products are checked too
	 * (GOU-2497) unless {@link GlobalSearchProperties#isForeignHeadUncategorizedOnly()} is on; nothing is set apart
	 * when the query asks for an accessory.
	 *
	 * @param query   sanitized user query
	 * @param product long tail candidate read with {@link #candidateSourceFilter()}
	 * @return {@code true} when the product reads as a part or an accessory of the requested object
	 */
	boolean isForeignHeadProduct(String query, Product product) {
		if (product == null || product.getOfferNames() == null || !properties.isForeignHeadEnabled()
				|| (properties.isForeignHeadUncategorizedOnly() && product.getDatasourceCategories() != null
						&& !product.getDatasourceCategories().isEmpty())
				|| queryMentionsAccessory(query)) {
			return false;
		}
		Set<String> brandWords = new LinkedHashSet<>(words(brand(product)));
		List<String> object = queryObject(words(query), brandWords);
		if (object.isEmpty()) {
			return false;
		}
		boolean foreign = false;
		for (String name : product.getOfferNames()) {
			ObjectPosition position = objectPosition(object, name, brandWords);
			if (position == ObjectPosition.HEAD) {
				return false;
			}
			foreign |= position == ObjectPosition.AFTER_HEAD;
		}
		return foreign;
	}

	/**
	 * Foreign head rule applied to one offer name: the name holds the object of the query, but the object is not the
	 * head of the name. The object is the query without its leading and trailing brands, articles, modifiers and words
	 * holding a digit ({@code lave-vaisselle bosch} gives {@code lave vaisselle}); the head is the first word of the
	 * name once the same words and the parenthesised parts are skipped ({@code Bosch Série 4 Lave-vaisselle} starts
	 * with {@code lave vaisselle}). Plurals, accents, case and hyphens are ignored. A query naming the part itself
	 * ({@code ressort lave-linge}) has that part as object, so its names are kept.
	 *
	 * @param query     sanitized user query
	 * @param offerName offer name of the product
	 * @param brand     brand of the product, may be {@code null}
	 * @return {@code true} when the object of the query follows another head noun
	 */
	boolean isForeignHeadName(String query, String offerName, String brand) {
		if (!properties.isForeignHeadEnabled() || queryMentionsAccessory(query)) {
			return false;
		}
		Set<String> brandWords = new LinkedHashSet<>(words(brand));
		List<String> object = queryObject(words(query), brandWords);
		return !object.isEmpty() && objectPosition(object, offerName, brandWords) == ObjectPosition.AFTER_HEAD;
	}

	private List<String> queryObject(List<String> queryWords, Set<String> brandWords) {
		int start = 0;
		int end = queryWords.size();
		while (start < end && isHeadSkipped(queryWords.get(start), brandWords)) {
			start++;
		}
		while (end > start && isHeadSkipped(queryWords.get(end - 1), brandWords)) {
			end--;
		}
		return queryWords.subList(start, end);
	}

	private ObjectPosition objectPosition(List<String> object, String offerName, Set<String> brandWords) {
		if (!StringUtils.hasText(offerName)) {
			return ObjectPosition.ABSENT;
		}
		List<String> name = words(PARENTHESES.matcher(offerName).replaceAll(" "));
		int head = 0;
		while (head < name.size() && isHeadSkipped(name.get(head), brandWords)) {
			head++;
		}
		for (int start = head; start + object.size() <= name.size(); start++) {
			if (sameWords(name, start, object)) {
				return start == head ? ObjectPosition.HEAD : ObjectPosition.AFTER_HEAD;
			}
		}
		return ObjectPosition.ABSENT;
	}

	private boolean isHeadSkipped(String word, Set<String> brandWords) {
		return HEAD_FILLER_WORDS.contains(word) || headSkippedWords.contains(word) || brandWords.contains(word)
				|| word.chars().anyMatch(Character::isDigit);
	}

	private static boolean sameWords(List<String> name, int start, List<String> object) {
		for (int i = 0; i < object.size(); i++) {
			String word = name.get(start + i);
			String term = object.get(i);
			if (!sameWordOrPlural(word, term) && !sameWordOrPlural(term, word)) {
				return false;
			}
		}
		return true;
	}

	/** Where the object of the query stands in an offer name. */
	private enum ObjectPosition {
		ABSENT, HEAD, AFTER_HEAD
	}

	private boolean partNameOfObject(List<String> name, List<String> object) {
		for (List<String> term : accessoryTerms) {
			for (int start = 0; start < name.size(); start++) {
				if (!matchesAt(name, start, term, true)) {
					continue;
				}
				int end = start + term.size();
				for (int ofIndex = end; ofIndex <= end + OF_MAX_GAPS && ofIndex < name.size(); ofIndex++) {
					if (!OF_WORDS.contains(name.get(ofIndex))) {
						continue;
					}
					int gaps = ofIndex - end;
					for (int objectStart = ofIndex + 1; objectStart <= ofIndex + 1 + OF_MAX_GAPS - gaps
							&& objectStart < name.size(); objectStart++) {
						if (matchesAt(name, objectStart, object, false)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private static boolean indexOfPour(List<String> name, List<String> object) {
		int from = 0;
		int found;
		while ((found = indexOf(name, object, from, false)) >= 0) {
			if (found > 0 && "pour".equals(name.get(found - 1))) {
				return true;
			}
			from = found + 1;
		}
		return false;
	}

	private static int indexOf(List<String> name, List<String> object, int from, boolean pluralAware) {
		for (int start = from; start + object.size() <= name.size(); start++) {
			if (matchesAt(name, start, object, pluralAware)) {
				return start;
			}
		}
		return -1;
	}

	private static boolean matchesAt(List<String> name, int start, List<String> term, boolean pluralAware) {
		if (start + term.size() > name.size()) {
			return false;
		}
		for (int i = 0; i < term.size(); i++) {
			String word = name.get(start + i);
			boolean same = pluralAware && i == 0 ? sameWordOrPlural(word, term.get(i)) : word.equals(term.get(i));
			if (!same) {
				return false;
			}
		}
		return true;
	}

	/** Terms designating the requested object: the whole query, then each of its words of 4 letters or more. */
	private static List<String> objectTerms(List<String> queryWords) {
		Set<String> terms = new LinkedHashSet<>();
		if (!queryWords.isEmpty()) {
			terms.add(String.join(" ", queryWords));
		}
		queryWords.stream().filter(w -> w.length() >= MIN_QUERY_TERM_LENGTH && !QUERY_STOP_WORDS.contains(w))
				.forEach(terms::add);
		return new ArrayList<>(terms);
	}

	private static Intervals phrase(String text) {
		return Intervals.of(i -> i.match(m -> m.query(text).maxGaps(0).ordered(true)));
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
