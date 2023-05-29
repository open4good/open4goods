//package org.open4goods.model.aggregation;
//
//import java.util.HashSet;
//import java.util.Set;
//
//import org.springframework.data.elasticsearch.annotations.Field;
//import org.springframework.data.elasticsearch.annotations.FieldType;
//
///**
// * Contains the stats / results about the construction of an Product
// * @author goulven
// *
// */
//public class AggregationResult {
//
//
//	@Field(index = false, store = false, type = FieldType.Object)
//	private Set<ParticipantData> participantDatas = new HashSet<>();
//
//	public Set<ParticipantData> getParticipantDatas() {
//		return participantDatas;
//	}
//
//	public void setParticipantDatas(final Set<ParticipantData> participantDatas) {
//		this.participantDatas = participantDatas;
//	}
//
//
//
//
//
//
//}
