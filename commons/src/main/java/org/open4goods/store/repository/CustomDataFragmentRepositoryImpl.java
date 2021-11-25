package org.open4goods.store.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.open4goods.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import io.micrometer.core.annotation.Timed;

/**
 * This elasticsearch DataFragmentRepository allows to store DataFragments in
 * different indices depending on their attributeExoansionKey
 * TODO(gof) : ensure all indices (offers, brands, ...) have the same schema
 * @author goulven
 *
 */
public class CustomDataFragmentRepositoryImpl implements CustomDataFragmentRepository {


	private final static Logger log = LoggerFactory.getLogger(CustomDataFragmentRepositoryImpl.class);

	/** The indice where "default" (no companies, additif or other related ones) are stored */
//	private static final String DEFAULT_DATAFRAGMENTS_INDICE = "data";

	private static final IndexCoordinates INDEX_COORDS = IndexCoordinates.of(DataFragment.DATAFRAGMENTS_INDEX);

	private @Autowired ElasticsearchRestTemplate elasticsearchRestTemplate;

	@Override
	@Timed(value="repository.ElasticDataFragmentRepository.get()",description="Retrieve a list of existing DataFragments")
	public Map<String, DataFragment> get(final Set<DataFragment> buffer) {
		// Classifying the DataFragments against their respectiv indices
		final Map<String, DataFragment> classifiedFragments = new HashMap<>();

		
		String[] ids = buffer.stream().map(e -> e.getUrl()).toArray(size -> new String[size]);
		final IdsQueryBuilder queryBuilder = new IdsQueryBuilder().addIds(ids);
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
		elasticsearchRestTemplate.search  (initialQuery, DataFragment.class,INDEX_COORDS)
					 
					.forEach((df) -> classifiedFragments.put(df.getContent().getUrl() , df.getContent()));
		return classifiedFragments;
	}

	
//	@PostConstruct
//	/**
//	 * Due to the untyped nature of the attributes.rawValue, have to create
//	 * the schema as String for DataFragments. (allow to share Attribute object 
//	 * between the types AggregatedData and the types DataFragment)
//	 * TODO : Should be provided through @Mapping annotation on DataFragment
//	 */
//	public void dummyInit() {
//		
//		try {
//			DataFragment df = new DataFragment();
//			df.addReferentielAttribute("GTIN", "123");
//			df.addAttribute("dummy", "dummy", "fr", true, Sets.newHashSet());
//			
//			Collection<DataFragment> s = Sets.newHashSet();
//			
//			DataVersion dv = new DataVersion();
//			
//			dv.setAttributesAdded(new HashSet<>());
//			dv.setAttributesRemoved(new HashSet<>());		
//			dv.getAttributesAdded().add(df.getAttribute("dummy"));
//			dv.getAttributesRemoved().add(df.getAttribute("dummy"));
//			
//			df.getHistory().add(dv);
//			
//			s.add(df);
//			
//			save(s);
//		} catch (Exception e) {
//			log.error("Error while tweaking setting  of es",e);
//		}
//	}

	@Override
	@Timed(value="repository.ElasticDataFragmentRepository.save()",description="Persist a set of DataFragments")
	public void save(final Collection<DataFragment> collection) {

		elasticsearchRestTemplate.save(collection);
	}
	

	@Override
	public Stream<DataFragment> getByGtin(final String gtin) {
		final QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder("referentielAttributes.GTIN.keyword:\""+gtin+"\"");
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
//				//TODO(gof) : from conf
				.withPageable(PageRequest.of(0, 100))				
				.build();
				
		return elasticsearchRestTemplate.search(initialQuery, DataFragment.class,INDEX_COORDS)
				.map(e->e.getContent()).get();

	}
	
	@Override

	public Map<String, Set<DataFragment>> getByGtin(final Set<String> gtin) {
		
		
		final QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder("referentielAttributes.GTIN.keyword:("+ StringUtils.join(gtin, " OR ")+")");
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)		
				.build();
				
		
		Map<String, Set<DataFragment>> ret = new HashMap<>();
		
		elasticsearchRestTemplate.search(initialQuery, DataFragment.class,INDEX_COORDS)
				.map(e->e.getContent()).get().forEach(e -> {
					if (!ret.containsKey(e.gtin())) {
						ret.put(e.gtin(), new HashSet<>());
					}
					ret.get(e.gtin()).add(e);
	
				});

		return ret;
	}
	
	
	
	public Stream<DataFragment> export(final String query) {
		final QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder(query);
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
				//TODO(gof) : from conf
				.withPageable(PageRequest.of(0, 100))				
				.build();
				
		return elasticsearchRestTemplate.searchForStream(initialQuery, DataFragment.class,INDEX_COORDS).stream().map(e->e.getContent());
	}
	

	@Override
	public Stream<String> exportGtin() {
		final MatchAllQueryBuilder queryBuilder = new MatchAllQueryBuilder();
		final NativeSearchQuery initialQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
//				//TODO(gof) : from conf
				.withPageable(PageRequest.of(0, 300))					
				.withFields("referentielAttributes.GTIN")
				.build();
				
		return elasticsearchRestTemplate.searchForStream(initialQuery, DataFragment.class,INDEX_COORDS)
				.stream()
				.map(e ->e.getContent().gtin())
				.filter(e -> e != null);
				
	}
	
	@Override
	public Stream<String> exportGtin(Long limit) {
		return exportGtin().limit(limit);
		
	}


//	/**
//	 * Return all the indices contained in a set of DataFragments. (
//	 * @param buffer
//	 * @return
//	 */
//	private String[] getIndicesFor(final Set<DataFragment> buffer) {
//		final Set<String> indices = new HashSet<>();
//
//		for (final DataFragment df : buffer) {
//			indices.add(getIndiceFor(df));
//		}
//		return indices.toArray(new String[0]);
//	}
//
//
//	@Override
//	public Set<String>  exportQualifiers(final String scope) {
//		final Map<String, Long> ret = new HashMap<>();
//
//		//TODO(P1,perf, 0.5) : Check aggregation working
//				
//		final QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder("scope:\""+scope+"\"");
//		final NativeSearchQueryBuilder initialQuery = new NativeSearchQueryBuilder()				
//				.withQuery(queryBuilder).addAggregation(
//						AggregationBuilders.terms("qualifier").size(100000)
//						.field("qualifier.keyword"));
//
//		ParsedStringTerms result = (ParsedStringTerms)elasticsearchRestTemplate.searchForStream(initialQuery.build(), DataFragment.class).getAggregations().get("qualifier");
//		
//
//		for (Bucket b : result.getBuckets()) {
//			ret.put(b.getKeyAsString().trim().toUpperCase(), b.getDocCount());
//		}
//		
//		return ret.keySet();
//	}



}