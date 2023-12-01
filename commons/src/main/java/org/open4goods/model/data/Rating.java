package org.open4goods.model.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.Validable;
import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
public class Rating  implements Validable, Standardisable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Rating.class);

	@Field(index = false, store = false, type = FieldType.Keyword)
	private Set<String> tags = new HashSet<>();


	@Field(index = true, store = false, type = FieldType.Double)
	private Double value;

	/**
	 * The max scale for this rating
	 */
	@Field(index = false, store = false, type = FieldType.Double)
	private Double max;

	/**
	 * The (optional) min scale
	 */
	@Field(index = false, store = false, type = FieldType.Integer)
	private Integer min;

	/** The number of voters for this rating, if any**/
	@Field(index = false, store = false, type = FieldType.Integer)
	private Long numberOfVoters;


	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////


	@Override
	public Set<Standardisable> standardisableChildren() {
		return Sets.newHashSet(this);
	}

	@Override
	public void standardize(final StandardiserService standardiser,final Currency c) {
		standardiser.standarise(this);
	}


	@Override
	public void validate() throws ValidationException {

		final Set<ValidationMessage> result = new HashSet<>();

		if (null == value) {
			result.add(ValidationMessage.newValidationMessage("MISSING-VALUE"));
		}

		if (null == max) {
			result.add(ValidationMessage.newValidationMessage("MISSING-MAX"));
		}

		if (null == min) {
			result.add(ValidationMessage.newValidationMessage("MISSING-MIN"));
		}


		//		if (tags.isEmpty()) {
		//			result.add(ValidationMessage.newValidationMessage("MISSING-TAGS"));
		//		}

		if (tags.contains( RatingType.SITES.toString())  && null == numberOfVoters) {
			result.add(ValidationMessage.newValidationMessage("MISSING-NUMBER-OF-VOTERS"));
		}

		if (!result.isEmpty()) {
			throw new ValidationException("Rating is not valid", result);
		}

	}


	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		return StringUtils.join(tags) + " :  abs=" + value;
	}



	@Override
	public int hashCode() {
		return Objects.hash(value, min, max, tags);
	}


	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Rating o) {
			return Objects.equals(value, o.getValue()) && Objects.equals(min, o.getMin())
					&& Objects.equals(max, o.getMax())
					//					&& Objects.equals(this.label, o.getLabel())
					&& Objects.equals(tags, o.getTags());
		}

		return false;
	}

	////////////////////////////////////////
	// Templates methods
	///////////////////////////////////////
	public Long percent() {
		return Math.round(value * 100 / StandardiserService.DEFAULT_MAX_RATING);
	}


	///////////////////////
	// The following helper allow to know the ranking, on a 5 scale
	///////////////////////

	public boolean is(Integer number) {

        return switch (number) {
            case 1 -> is1();
            case 2 -> is2();
            case 3 -> is3();
            case 4 -> is4();
            case 5 -> is5();
            default -> {
                LOGGER.warn("Cannot get rating is({})", number);
                yield false;
            }
        };
	}

	public void addTag(RatingType rType) {
		tags.add(rType.toString());
	}

	public void addTag(String rType) {
		tags.add(rType);
	}



	@JsonIgnore
	public boolean is1() {
		return percent() <= 20L;
	}

	@JsonIgnore
	public boolean is2() {
		Long p = percent();
		return p > 20L && p <= 40;
	}
	@JsonIgnore
	public boolean is3() {
		Long p = percent();
		return p > 40L && p <= 60;
	}
	@JsonIgnore
	public boolean is4() {
		Long p = percent();
		return p > 60L && p < 80;
	}
	@JsonIgnore
	public boolean is5() {
		Long p = percent();
		return p >=80;
	}



	public String color() {
		if (null == value) {
			return "white";
		}
		final Long percent =  percent();

		if (percent > 80) {
			return "green";
		} else if (percent > 60) {
			return "blue";
		}else if (percent > 30) {
			return "yellow";
		}else {
			return "red";
		}


	}
	////////////////////////////////////////
	// Getters / Setters
	///////////////////////////////////////


	public Double getValue() {
		return value;
	}

	public void setValue(final Double value) {
		this.value = value;
	}

	public Double getMax() {
		return max;
	}

	public void setMax(final Double max) {
		this.max = max;
	}

	public Integer getMin() {
		return min;
	}

	public void setMin(final Integer min) {
		this.min = min;
	}


	public Long getNumberOfVoters() {
		return numberOfVoters;
	}

	public void setNumberOfVoters(final Long numberOfVoters) {
		this.numberOfVoters = numberOfVoters;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}






}
