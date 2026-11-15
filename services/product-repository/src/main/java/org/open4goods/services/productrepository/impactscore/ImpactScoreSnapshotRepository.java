package org.open4goods.services.productrepository.impactscore;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.impactscore.ImpactScoreSnapshot;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Elasticsearch storage of the monthly {@link ImpactScoreSnapshot}s (index {@value ImpactScoreSnapshot#INDEX_NAME}).
 * Written by the api batch, read by front-api. The document id is deterministic (vertical and month), so saving a
 * month twice replaces the document.
 */
public class ImpactScoreSnapshotRepository {

	/** Upper bound of the rows returned: the default Elasticsearch result window, far above all verticals over decades. */
	static final int MAX_ROWS = 10_000;

	private final ElasticsearchOperations operations;

	public ImpactScoreSnapshotRepository(ElasticsearchOperations operations) {
		this.operations = operations;
	}

	/**
	 * Whether the snapshot of a vertical and a month exists.
	 *
	 * @param id {@link ImpactScoreSnapshot#id()}
	 * @return {@code true} when stored
	 */
	public boolean exists(String id) {
		IndexOperations index = operations.indexOps(ImpactScoreSnapshot.class);
		return index.exists() && operations.exists(id, ImpactScoreSnapshot.class);
	}

	/**
	 * Stores a snapshot, creating the index on first use and replacing the document of the same id.
	 *
	 * @param snapshot the snapshot
	 */
	public void save(ImpactScoreSnapshot snapshot) {
		IndexOperations index = operations.indexOps(ImpactScoreSnapshot.class);
		if (!index.exists()) {
			index.createWithMapping();
		}
		operations.save(snapshot);
		index.refresh();
	}

	/**
	 * Snapshots, oldest first.
	 *
	 * @param verticalId the vertical, or {@code null} for all verticals
	 * @param fromMonth  first month included ({@code yyyy-MM}), or {@code null}
	 * @param toMonth    last month included ({@code yyyy-MM}), or {@code null}
	 * @return the snapshots, empty when the index does not exist yet
	 */
	public List<ImpactScoreSnapshot> find(String verticalId, String fromMonth, String toMonth) {
		if (!operations.indexOps(ImpactScoreSnapshot.class).exists()) {
			return List.of();
		}
		List<Criteria> conditions = new ArrayList<>();
		if (verticalId != null) {
			conditions.add(new Criteria("verticalId").is(verticalId));
		}
		if (fromMonth != null) {
			conditions.add(new Criteria("month").greaterThanEqual(fromMonth));
		}
		if (toMonth != null) {
			conditions.add(new Criteria("month").lessThanEqual(toMonth));
		}
		Query query = conditions.isEmpty() ? Query.findAll() : new CriteriaQuery(conditions.stream().reduce(Criteria::and).get());
		query.setPageable(PageRequest.of(0, MAX_ROWS, Sort.by("month", "verticalId")));
		return operations.search(query, ImpactScoreSnapshot.class).stream().map(SearchHit::getContent).toList();
	}
}
