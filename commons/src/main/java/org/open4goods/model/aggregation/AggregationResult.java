package org.open4goods.model.aggregation;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains the stats / results about the construction of an AggregatedData
 * @author goulven
 *
 */
public class AggregationResult {



	private Set<ParticipantData> participantDatas = new HashSet<>();

	public Set<ParticipantData> getParticipantDatas() {
		return participantDatas;
	}

	public void setParticipantDatas(final Set<ParticipantData> participantDatas) {
		this.participantDatas = participantDatas;
	}






}
