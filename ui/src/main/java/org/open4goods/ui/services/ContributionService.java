package org.open4goods.ui.services;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.data.ContributionVote;
import org.open4goods.services.SerialisationService;
import org.open4goods.ui.config.yml.Reversement;
import org.open4goods.ui.config.yml.ReversementConfig;
import org.open4goods.ui.repository.ContributionVoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;

/**
 * This service is in charge of storing the user contributions choices (aka "nudges"). It also provides
 * facilities to monitor and get stats on the contributions. 
 */
public class ContributionService implements HealthIndicator {
	
	private static final String ALREADY_VOTED_CONST = "ALREADY_VOTED";

	private static final Logger logger = LoggerFactory.getLogger(ContributionService.class);
	
	private SerialisationService serialisationService;
	private ContributionVoteRepository repository;
	private ElasticsearchOperations elasticSearchOperations;
	private Cache cache;
	
	private AtomicInteger criticalExceptionscounter = new AtomicInteger(0);

	private ReversementConfig reversementConfig;
	

	
	public ContributionService(CacheManager cacheManager, SerialisationService serialisationService, ContributionVoteRepository repository, ReversementConfig reversementConfig,  ElasticsearchOperations elasticsearchOperations) {
		super();
		this.serialisationService = serialisationService;
		this.repository = repository;
		this.reversementConfig = reversementConfig;
		this.elasticSearchOperations = elasticsearchOperations;
		
		// Setting the cache to use for IP unbulking.
		// Means there a user cannot vote more than once an hour
		this.cache = cacheManager.getCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME);;
	}

	/**
	 * Store the user contribution vote, 
	 * @param ip
	 * @param token
	 * @param vote
	 * @return 
	 */
	public String processContributionVote(String ip, String ua, String token, String vote) {

		// Deserialize the token
		try {
			// Unencoding the token
			String str = SerialisationService.uncompressString(URLDecoder.decode(token,Charset.defaultCharset()));

			// Deserialisation 
			ContributionVote aff = serialisationService.fromJson(str, ContributionVote.class);
			
			aff.setIp(ip);
			aff.setUa(ua);
			ValueWrapper existantVote = cache.get(ip);
			
			if (null == existantVote) {
				aff.setVote(vote);
				cache.put(ip, vote);
			} else {
				aff.setVote(ALREADY_VOTED_CONST);
			}
			
			// TODO(P2,perf,0.5) : async index
			repository.save(aff);
			
			// Return the decoded affiliationtoken
			return aff.getUrl();
			
		} catch (final Exception e) {
			criticalExceptionscounter.incrementAndGet();
			logger.warn("Error handling contribution vote {}", token, e);
			return null;
		}
		
	
	}


	
	/**
	 * Return the total number of nudges since a given date
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR,  cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Long nudgesCountSinceLastReversement() {
		return repository.countByTsGreaterThanEqual(reversementConfig.getLastReversementEpoch());
	}
	
	
	/**
	 * Return the repartition of nudges per organization since the last reversement
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR,  cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Map<String,Long > nudgesRepartitionSinceLastReversement() {
		
		return byVotesSince(reversementConfig.getLastReversementEpoch());
	}
	

	/**
	 * Proceed to the elastic aggregation to get count by votes
	 * @param epoch
	 * @return
	 */
	private Map<String, Long> byVotesSince(Long epoch) {

		// Setting the query
		NativeQueryBuilder esQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery( new Criteria("id").exists()));

		// Adding standard aggregations
		esQuery = esQuery
				.withAggregation("vote", 	Aggregation.of(a -> a.terms(ta -> ta.field("vote").size(500))  ))
				;
	
		SearchHits<ContributionVote> results = elasticSearchOperations.search(esQuery.build(), ContributionVote.class, IndexCoordinates.of(ContributionVote.INDEX_NAME));

		// Handling aggregations results if relevant
		//TODO(gof) : this cast should be avoided
		ElasticsearchAggregations aggregations = (ElasticsearchAggregations)results.getAggregations();


		///////
		// Numeric aggregations
		///////
		StringTermsAggregate taxonomy = aggregations.get("vote").aggregation().getAggregate().sterms();

		Map<String, Long> ret = new HashMap<>();
		for (StringTermsBucket b : taxonomy.buckets().array()) {
			ret.put(b.key().stringValue(), b.docCount());
		}
 		
		
		return ret;
		
	}
	
	
	
	/**
	 * Custom healthcheck, simply goes to DOWN if critical exception occurs
	 */
	@Override
	public Health health() {
		
		Builder health;
		
		long eCount = criticalExceptionscounter.longValue();
		
		if (0L == eCount ) {
			health =  Health.up();
		} else {
			health =  Health.down();
		}

		return health.withDetail("critical_exceptions", eCount).build();
	}
	

	
	
}
