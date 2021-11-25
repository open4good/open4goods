
package org.open4goods.model.data;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.model.Standardisable;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * An DataVersion allows versionning of DataFragment. fields not null are the
 * previous state of the offer (whome changes date are timestamped in 'date)
 *
 *TODO(feature) : version shipping time, shipping cost, quantityInStock, warranty
 * @author goulven
 *
 */
public class DataVersion implements Standardisable {

	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)	
	private long date;

	//TODO(gof) : remove other than price ?
	
	private Price price;

//	private Set<Description> descriptionsRemoved;
//
//	private Set<Description> descriptionsAdded;
//
//	private Set<Resource> resourcesRemoved;
//
//	private Set<Resource> resourcesAdded;
//
//	private Set<Comment> commentsAdded;
//
//	private Set<Comment> commentsRemoved;
//
//	private Set<Attribute> attributesAdded;
//	private Set<Attribute> attributesRemoved;
//
//	private Set<Question> questionsAdded;
//	private Set<Question> questionsRemoved;
//
//	private Set<ProsOrCons> prosAdded;
//	private Set<ProsOrCons> prosRemoved;
//
//	private Set<ProsOrCons> consAdded;
//	private Set<ProsOrCons> consRemoved;
//
//	private Set<Rating> ratingsAdded;
//
//	private Set<Rating> ratingsRemoved;



	//////////////////////////////////////////
	// Contract
	//////////////////////////////////////////

	@Override
	public Set<Standardisable> standardisableChildren() {
		final Set<Standardisable> ret = new HashSet<>();

//		ret.addAll(ratingsAdded);
//		ret.addAll(ratingsRemoved);
//
//		ret.addAll(commentsAdded);
//		ret.addAll(commentsRemoved);

		ret.add(price);

		return ret;

	}

	@Override
	public void standardize(final StandardiserService standardiser, final Currency c) {

		for (final Standardisable s : standardisableChildren()) {
			s.standardize(standardiser, c);
		}
	}

	//////////////////////////////////////////
	// Getter / Setter
	//////////////////////////////////////////

	public long getDate() {
		return date;
	}

	public void setDate(final long date) {
		this.date = date;
	}

	public Price getPrice() {
		return price;
	}

	public void setPrice(final Price price) {
		this.price = price;
	}


}
