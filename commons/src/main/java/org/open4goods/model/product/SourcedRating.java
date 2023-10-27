package org.open4goods.model.product;

import java.util.Objects;

import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Rating;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
// TODO : rename in Score
public class SourcedRating extends Rating{

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String datasourceName;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;

	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long date;


	/**
	 * The cardinality (min / max / avg ...)  for this rating
	 */
	@Field(index = false, store = false, type = FieldType.Object)
	private Cardinality cardinality;

	/**
	 * The relativ value, once re-scaled from cardinality  ('value' attr is the absolute value)
	 */
	@Field(index = false, store = false, type = FieldType.Double)
	private Double relValue;




	/**
	 * Constructor from a rating and a datafragment
	 * @param translated
	 * @param df
	 */
	public SourcedRating(Rating source, DataFragment df) {

		date = df.getLastIndexationDate();
		datasourceName = df.getDatasourceName();
		url=df.affiliatedUrlIfPossible();

		setMax(source.getMax());
		setMin(source.getMin());
		setNumberOfVoters(source.getNumberOfVoters());
		setTags(source.getTags());
		setValue(source.getValue());
	}




	public SourcedRating() {
	}

	////////////////////////////////
	// HashCode / equals
	////////////////////////////////



	@Override
	public String toString() {
		return super.toString()+", relValue=" + relValue + ", card : " + cardinality;
	}


	@Override
	public int hashCode() {
		return Objects.hash(getValue(), getMin(), getMax(), getTags(), date,datasourceName,url );
	}


	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof SourcedRating) {
			final SourcedRating o = (SourcedRating) obj;
			return Objects.equals(getValue(), o.getValue()) && Objects.equals(getMin(), o.getMin())
					&& Objects.equals(getMax(), o.getMax())
					//					&& Objects.equals(this.label, o.getLabel())
					&& Objects.equals(getTags(), o.getTags())
					&& Objects.equals(getDate(), o.getDate())
					&& Objects.equals(getDatasourceName(), o.getDatasourceName())
					&& Objects.equals(getUrl(), o.getUrl())
					;
		}
		return false;
	}

	////////////////////////////////
	// Getters / setters
	////////////////////////////////



	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(Long date) {
		this.date = date;
	}

	public Cardinality getCardinality() {
		return cardinality;
	}

	public void setCardinality(Cardinality cardinality) {
		this.cardinality = cardinality;
	}

	public Double getRelValue() {
		return relValue;
	}

	public void setRelValue(Double relValue) {
		this.relValue = relValue;
	}





}
