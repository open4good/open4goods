package org.open4goods.model.data;

import java.util.Objects;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Validable;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Score  implements Validable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Score.class);

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String name;;

	@Field(index = true, store = false, type = FieldType.Boolean)
	private Boolean virtual = false;
	
	@Field(index = true, store = false, type = FieldType.Boolean)
	// Is equals to relativ.value
	// TODO : Remove field ?
	private Double value;

	@Field(index = true, store = false, type = FieldType.Object)
	/** The source cardinalities, absolute mode.**/
	private Cardinality absolute;
	
	
	@Field(index = true, store = false, type = FieldType.Object)
	/** The relativised cardinalities, relativ mode.**/
	private Cardinality relativ;
	
		
	
	////////////////////////////////////////
	// Contracts
	///////////////////////////////////////

	public Score() {
	}
	

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
		sb.append((virtual ? "(virtual):" : ":") );
		if (null != relativ) {
			sb.append(relativ.getValue()).append("--> rel:(").append(relativ).append(")");
		}
		
		if (null != absolute) {
			sb.append(relativ.getValue()).append("--> abs:(").append(absolute).append(")");
		}
		
		
		return sb.toString();
	}



	@Override
	public int hashCode() {		
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof Score o) {
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

	public Long on20() {
		return Math.round(relativ.getValue() * 20 / StandardiserService.DEFAULT_MAX_RATING);
	}
	
	
	public String absValue () {
		return AggregatedPrice.numberFormater.format(absolute.getValue());
	}

	public String relValue () {
		return AggregatedPrice.numberFormater.format(relativ.getValue());
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

	
	
	
	/**
	 * Generate the letter score 
	 * @return
	 */
	public String letter() {
		Long percent = percent();
		
		if (percent <= 20.0) {
			return "E";
		} else if (percent <= 40.0) {
			return "D";
		}else if (percent <= 60.0) {
			return "C";
		}else if (percent <= 80.0) {
			return "B";
		} else {
			return "A";
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


	/**
	 * 
	 * @return the color bootsrap class name
	 */
	public String colorClassName() {
		Long p = percent();
		
		if (p >= 80) {
			return "success";
		} else if (p >=  50) {
			return "info";			
		} else {
			return "danger";
		}
		
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
