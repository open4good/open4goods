package org.open4goods.model.product;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.data.aggregated.AggregatedComment;

public class AggregatedComments {
	
	/**
	 * The by date ordered aggregated comments
	 */
	private List<AggregatedComment> comments = new ArrayList<AggregatedComment>();

	public List<AggregatedComment> getComments() {
		return comments;
	}

	public void setComments(List<AggregatedComment> comments) {
		this.comments = comments;
	}
	
}
