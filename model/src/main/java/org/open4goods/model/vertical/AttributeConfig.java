
package org.open4goods.model.vertical;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.lifecycle.LifecycleStage;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author goulven
 *
 */
public class AttributeConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(AttributeConfig.class);

	private static final Map<String,AttributeParser> parserInstances = new HashMap<>();


	/**
	 * The identifier for this attribute.
	 */
	private String key;



	/**
	 * The associated font awesome icon
	 */
	private String faIcon = "fa-wrench";


        /**
         * The localised units
         */
        private Localisable<String,String> unit;

        /**
         * Localised suffix appended to the raw value when displayed to end-users.
         * Designed for compact units such as symbols or abbreviations (for example the inch symbol).
         */
        private Localisable<String, String> suffix;


	/**
	 * The localised names
	 */
	private Localisable<String,String> name ;

	/**
	 * Indicates if it is numeric or terms filter. Will impact how the queries are made, and how the attribute is rendered in vertical search
	 */
	private AttributeType filteringType = AttributeType.TEXT;

	/**
	 * The icecat features id's this attribute is mapped to
	 */
	private Set<String> icecatFeaturesIds = new HashSet<String>();

	/**
	 * The associated eprel features names.
	 */
	private Set<String> eprelFeatureNames = new HashSet<String>();



        /**
         * If true, this attribute will be added as a score, mapped through the numericMapping configuration attribute and an application of the scoring (min/max) mechanism
         */
        private boolean asScore = false;

        /**
         * Localised label used when the attribute is displayed as a score title.
         */
        private Localisable<String, String> scoreTitle;

        /**
         * Localised description explaining what the score measures.
         */
        private Localisable<String, String> scoreDescription;

        /**
         * Localised text describing why this score matters to the impact calculation.
         */
        private Localisable<String, String> scoreUtility;

        /**
         * Names of the composite scores this attribute participates in.
         */
        private Set<String> participateInScores = new HashSet<>();

        /**
         * Lifecycle stages (ACV) represented by this attribute when used as a score.
         */
        private Set<LifecycleStage> participateInACV = new HashSet<>();

        /**
         * Comparison rule describing which values are preferred by users.
         */
        private AttributeComparisonRule userBetterIs;

        /**
         * Comparison rule describing which values are preferred for impact scoring.
         */
        private AttributeComparisonRule impactBetterIs;

        /**
         * Scoring configuration used when this attribute is translated into a score.
         */
        private ScoreScoringConfig scoring;

	/**
	 * The ordering that must be applied to this attributes values after aggregations. (ie rendered in search attributes selection)
	 */
	private Order attributeValuesOrdering = Order.COUNT;

	/**
	 * If true, the ordering applied to aggregations will be reversed
	 */
	private Boolean attributeValuesReverseOrder = false;

	/**
	 * The attribute name that matches this attribute definition. key is datasourcename or "all" if applies on any.
	 */
	private Map<String, Set<String>> synonyms = new HashMap<>();


	/**
	 * The parser class for custom types / datas parsing
	 */
	private AttributeParserConfig parser = new AttributeParserConfig();

	/**
	 * If set, text attributes will be converted to numerics using this table conversion
	 */
	private Map<String,Double> numericMapping = new HashMap<>();


	/**
	 * If set, fixed text mappings conversion
	 */
	private Map<String,String> mappings = new HashMap<>();



	/**
	 *
	 * @return the instance of the defined parser, if set.
	 * @throws ResourceNotFoundException
	 */
	@JsonIgnore
	public AttributeParser getParserInstance() throws ResourceNotFoundException{


		final String clazz = parser.getClazz();
		if (StringUtils.isEmpty(clazz)) {
			throw new ResourceNotFoundException("No parser class defined for " + getKey());
		}

		AttributeParser parser = null;
		// Instanciating / caching the parser
		if (null == parserInstances.get(clazz)) {
			try {
				parser = (AttributeParser) Class.forName(clazz).getDeclaredConstructor().newInstance();
				LOGGER.info("{} parser has been placed in uidMap");
				parserInstances.put(clazz, parser);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
				throw new ResourceNotFoundException("Cannot instanciate type : " + clazz,e);
			}
        }

		return parserInstances.get(clazz);
	}

	/**
	 *
	 * @param string
	 * @return
	 */
	public String applyCase(final String string) {
		if (parser.getLowerCase()) {
			return string.toLowerCase();
		}

		if (parser.getUpperCase()) {
			return string.toUpperCase();
		}
		return string;
	}

	/**
	 * Add a synomym to this attribute config
	 * @param store
	 * @param synonym
	 */
	public void addSynonym(final String store, final String synonym) {
		if (!synonyms.containsKey(store)) {
			synonyms.put(store,new HashSet<>());
		}

		synonyms.get(store).add(synonym);



	}


	/**
	 * If attributeconfig involves a rating translation, allows to get the max
	 * @return
	 * @throws ValidationException
	 * @throws NoSuchFieldException
	 */
	public Double maxRating() throws ValidationException, NoSuchFieldException {
		if (!asScore) {
			throw new ValidationException("Attribute is not configured to be translated as a rating");
		}

		if ( 0 == numericMapping.size()) {
			throw new ValidationException("Attribute is not configured to be translated as a rating, but has no numericMapping configuration");
		}

		return numericMapping.values().stream().max(Comparator.comparing(Double::valueOf)).orElseThrow();
	}


	/**
	 * If attributeconfig involves a rating translation, allows to get the min
	 * @return
	 * @throws ValidationException
	 * @throws NoSuchFieldException
	 */
	public Double minRating() throws ValidationException, NoSuchFieldException {
		if (!asScore) {
			throw new ValidationException("Attribute is not configured to be translated as a rating");
		}

		if ( 0 == numericMapping.size()) {
			throw new ValidationException("Attribute is not configured to be translated as a rating, but has no numericMapping configuration");
		}

		return numericMapping.values().stream().min(Comparator.comparing(Double::valueOf)).orElseThrow();
	}



	@Override
	public String toString() {
		return key + ":" + filteringType;
	}


	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof AttributeConfig) {
			String tmp = ((AttributeConfig)obj).getKey();
			if (null == tmp) {
				return tmp == key;
			}

			return key.equals(tmp);
		}

		return false;
	}




	public String i18n( final String language) {
		return name == null ? "null:"+language : name.i18n(language);
	}

	public AttributeType getFilteringType() {
		return filteringType;
	}


	public void setFilteringType(final AttributeType type) {
		this.filteringType = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}



	public Map<String, Set<String>> getSynonyms() {
		return synonyms;
	}




	public void setSynonyms(final Map<String, Set<String>> synonyms) {
		this.synonyms = synonyms;
	}



	public String getFaIcon() {
		return faIcon;
	}

	public void setFaIcon(final String faIcon) {
		this.faIcon = faIcon;
	}


	public AttributeParserConfig getParser() {
		return parser;
	}

	public void setParser(final AttributeParserConfig parser) {
		this.parser = parser;
	}

	public Order getAttributeValuesOrdering() {
		return attributeValuesOrdering;
	}

	public void setAttributeValuesOrdering(final Order order) {
		attributeValuesOrdering = order;
	}

	public Boolean getAttributeValuesReverseOrder() {
		return attributeValuesReverseOrder;
	}

	public void setAttributeValuesReverseOrder(final Boolean reverseOrder) {
		attributeValuesReverseOrder = reverseOrder;
	}



	public Map<String, Double> getNumericMapping() {
		return numericMapping;
	}



	public void setNumericMapping(final Map<String, Double> numericMapping) {
		this.numericMapping = numericMapping;
	}

        public boolean isAsScore() {
                return asScore;
        }

        public void setAsScore(boolean asRating) {
                this.asScore = asRating;
        }

        /**
         * Gets the localised score title used when rendering impact components.
         *
         * @return translated score title or {@code null} when not defined.
         */
        public Localisable<String, String> getScoreTitle() {
                return scoreTitle;
        }

        /**
         * Sets the localised score title used when rendering impact components.
         *
         * @param scoreTitle localised value to persist
         */
        public void setScoreTitle(Localisable<String, String> scoreTitle) {
                this.scoreTitle = scoreTitle;
        }

        /**
         * Gets the localised description for this score.
         *
         * @return translated description or {@code null}.
         */
        public Localisable<String, String> getScoreDescription() {
                return scoreDescription;
        }

        /**
         * Sets the localised description for this score.
         *
         * @param scoreDescription localised value to set
         */
        public void setScoreDescription(Localisable<String, String> scoreDescription) {
                this.scoreDescription = scoreDescription;
        }

        /**
         * Gets the localised utility text describing why the score matters.
         *
         * @return translated utility text or {@code null}.
         */
        public Localisable<String, String> getScoreUtility() {
                return scoreUtility;
        }

        /**
         * Sets the localised utility text describing why the score matters.
         *
         * @param scoreUtility localised value to set
         */
        public void setScoreUtility(Localisable<String, String> scoreUtility) {
                this.scoreUtility = scoreUtility;
        }

        /**
         * Names the composite scores this attribute participates in.
         *
         * @return set of parent score identifiers
         */
        public Set<String> getParticipateInScores() {
                return participateInScores;
        }

        /**
         * Sets the composite scores this attribute participates in.
         *
         * @param participateInScores list of score identifiers
         */
        public void setParticipateInScores(Set<String> participateInScores) {
                this.participateInScores = participateInScores;
        }

        /**
         * Lifecycle stages covered by this attribute when it acts as a score.
         *
         * @return lifecycle stages set
         */
        public Set<LifecycleStage> getParticipateInACV() {
                return participateInACV;
        }

        /**
         * Sets the lifecycle stages covered by this attribute when it acts as a score.
         *
         * @param participateInACV lifecycle stages to assign
         */
        public void setParticipateInACV(Set<LifecycleStage> participateInACV) {
                this.participateInACV = participateInACV;
        }

        public Localisable<String, String> getName() {
                return name;
        }

	public void setName(Localisable<String, String> name) {
		this.name = name;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, String> mappings) {
		this.mappings = mappings;
	}


	public Set<String> getIcecatFeaturesIds() {
		return icecatFeaturesIds;
	}

	public void setIcecatFeaturesIds(Set<String> icecatFeaturesIds) {
		this.icecatFeaturesIds = icecatFeaturesIds;
	}

        /**
         * Gets the comparison rule describing which values are preferred by users.
         *
         * @return the user preference comparison rule, defaults to {@link AttributeComparisonRule#GREATER}.
         */
        public AttributeComparisonRule getUserBetterIs() {
                return userBetterIs == null ? AttributeComparisonRule.GREATER : userBetterIs;
        }

        /**
         * Sets the comparison rule describing which values are preferred by users.
         *
         * @param userBetterIs the comparison rule to apply for user-facing comparisons.
         */
        public void setUserBetterIs(AttributeComparisonRule userBetterIs) {
                this.userBetterIs = userBetterIs;
        }

        /**
         * Gets the comparison rule describing which values are preferred for impact scoring.
         *
         * @return the impact comparison rule, defaults to {@link AttributeComparisonRule#GREATER}.
         */
        public AttributeComparisonRule getImpactBetterIs() {
                return impactBetterIs == null ? AttributeComparisonRule.GREATER : impactBetterIs;
        }

        /**
         * Sets the comparison rule describing which values are preferred for impact scoring.
         *
         * @param impactBetterIs the comparison rule to apply for impact scoring.
         */
        public void setImpactBetterIs(AttributeComparisonRule impactBetterIs) {
                this.impactBetterIs = impactBetterIs;
        }

        /**
         * Gets the scoring configuration block for this attribute.
         *
         * @return scoring configuration or {@code null} if none configured.
         */
        public ScoreScoringConfig getScoring() {
                return scoring;
        }

        /**
         * Sets the scoring configuration block for this attribute.
         *
         * @param scoring scoring configuration to use
         */
        public void setScoring(ScoreScoringConfig scoring) {
                this.scoring = scoring;
        }

        public Localisable<String, String> getUnit() {
                return unit;
        }

        public void setUnit(Localisable<String, String> unit) {
                this.unit = unit;
        }

        /**
         * Gets the suffix appended to the attribute value for display purposes.
         *
         * @return the configured suffix or {@code null} when no suffix is required
         */
        public Localisable<String, String> getSuffix() {
                return suffix;
        }

        /**
         * Sets the suffix appended to the attribute value for display purposes.
         *
         * @param suffix localised suffix to append to rendered values
         */
        public void setSuffix(Localisable<String, String> suffix) {
                this.suffix = suffix;
        }

		public Set<String> getEprelFeatureNames() {
			return eprelFeatureNames;
		}

		public void setEprelFeatureNames(Set<String> eprelFeatureNames) {
			this.eprelFeatureNames = eprelFeatureNames;
		}


}
