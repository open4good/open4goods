package org.open4goods.model.data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.Validable;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
public class Score  implements Validable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Score.class);

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String name;;

	@Field(index = true, store = false, type = FieldType.Boolean)
	private Boolean virtual = false;
	
	@Field(index = true, store = false, type = FieldType.Boolean)
	private Double value;

	@Field(index = true, store = false, type = FieldType.Object)
	/** The source cardinalities, absolute mode.**/
	private Cardinality absolute;
	
	
	@Field(index = true, store = false, type = FieldType.Object)
	/** The relativised cardinalities, absolute mode.**/
	private Cardinality relativ;
	
	

	
	
	
	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////



	public Score(String name, Double value) {
		this.name = name;
		this.value = value;
	}


	@Override
	public void validate() throws ValidationException {

		}


	////////////////////////////////////////
	// toString / Equals / HashCode
	///////////////////////////////////////

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder(name);
		sb.append((virtual == true ? "(virtual)" : "") );
		if (null != relativ) {
			sb.append( relativ.getValue() +  "--> rel:("+ relativ +")");
		}
		
		if (null != absolute) {
			sb.append( relativ.getValue() +  "--> rel:("+ absolute +")");
		}
		
		
		return sb.toString();
	}



	@Override
	public int hashCode() {		
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Score) {
			final Score o = (Score) obj;
			return Objects.equals(name, o.getName()) ;
		}

		return false;
	}

	////////////////////////////////////////
	// Templates methods
	///////////////////////////////////////
	public Long percent() {
		return Math.round(relativ.getValue() * 100 / StandardiserService.DEFAULT_MAX_RATING);
	}


	///////////////////////
	// The following helper allow to know the ranking, on a 5 scale
	///////////////////////

	public boolean is(Integer number) {

		switch (number) {
		case 1:
			return is1();
		case 2:
			return is2();
		case 3:
			return is3();
		case 4:
			return is4();
		case 5:
			return is5();
		default:
			LOGGER.warn("Cannot get rating is({})",number);
			return false;
		}
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
		if (null == relativ.getValue()) {
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


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Boolean getVirtual() {
		return virtual;
	}


	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
	}


	public Cardinality getAbsolute() {
		return absolute;
	}


	public void setAbsolute(Cardinality absolute) {
		this.absolute = absolute;
	}


	public Cardinality getRelativ() {
		return relativ;
	}


	public void setRelativ(Cardinality relativ) {
		this.relativ = relativ;
	}


	public Double getValue() {
		return value;
	}


	public void setValue(Double value) {
		this.value = value;
	}



}
