package org.open4goods.api.services;
//
//package org.open4goods.api.services;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
//
//import org.open4goods.aggregation.AbstractAggregationService;
//import org.open4goods.aggregation.aggregator.RealTimeAggregator;
//import org.open4goods.aggregation.services.aggregation.CommentsAggregationService;
//import org.open4goods.aggregation.services.aggregation.DescriptionsAggregationService;
//import org.open4goods.aggregation.services.aggregation.PriceAggregationService;
//import org.open4goods.aggregation.services.aggregation.ProsAndConsAggregationService;
//import org.open4goods.aggregation.services.aggregation.QuestionsAggregationService;
//import org.open4goods.api.config.yml.ApiProperties;
//import store.repository.DataFragmentRepository;
//import org.open4goods.config.yml.ui.VerticalConfig;
//import org.open4goods.exceptions.NotAddedException;
//import org.open4goods.model.constants.ReferentielKey;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.product.AggregatedData;
//import org.open4goods.services.EvaluationService;
//import org.open4goods.services.ImageMagickService;
//import org.open4goods.services.SerialisationService;
//
///**
// * This service is in charge of building AggregatedData in realtime mode
//
// * @author goulven
// *
// */
//public class RealTimeAggregationService {
//
//	
//	
//
//	protected static final Logger logger = LoggerFactory.getLogger(RealTimeAggregationService.class);
//
//	private @Autowired DataFragmentRepository repository;
//
//	private @Autowired ApiProperties config;
//
//	private @Autowired EvaluationService evaluationService;
//	
//	private @Autowired SerialisationService serialisationService;
//
//	private @Autowired RelatedDataService relationDataService;
//
//
//
//	private @Autowired ImageMagickService imageMagickService;
//	
//	private RealTimeAggregator aggregator;
//
//	private @Autowired AutowireCapableBeanFactory autowireBeanFactory;
//
//	///////////////////////////////////////////
//	// Composition of the aggregator used for real time
//	///////////////////////////////////////////
//	RealTimeAggregator realTimeAggregator() {
//
//		//TODO(gof) : from config
//		String logsFolder = config.getLogsFolder();
//
//		VerticalConfig uiconfig = new VerticalConfig();
//		
//		final List<AbstractAggregationService> services = new ArrayList<>();
//
////		// Identity (ids, name, ...)
////		services.add(new IdentityAggregationService(logsFolder));
////		
////		// Attributes
////		services.add(new GlobalAttributeAggregationService(logsFolder, config.getAttributeAggregationConfig()));
//		
//		// TODO : history
//		services.add(new PriceAggregationService(logsFolder,serialisationService));
//		
//		//TODO
//		//		services.add(new SiteMapAggregationService(this.config, this.config.getLogsFolder()));
//
//		services.add(new CommentsAggregationService(logsFolder, null));
//
//		services.add(new ProsAndConsAggregationService(logsFolder));
//		
//		services.add(new QuestionsAggregationService(logsFolder));
//
//		services.add(new DescriptionsAggregationService(config.getRealtimeAggregationConfig().getDescriptionsAggregationConfig(), logsFolder));
//
////		services.add(new RtBarCodeAggregationService(uiconfig, config.getRealtimeAggregationConfig().getBarcodeConfig(),null, logsFolder));
////
////		services.add(new RtMediaAggregationService(this.imageMagickService,  uiconfig,this.config.getLogsFolder()));	
////		
////		services.add(new RtRatingsAggregationService(this.config.getLogsFolder()));
////		
//
//				
////		services.add(new BlablaDescriptionsAggregationService(getBlablaConfigs(vConf),this.config.getLogsFolder() ));
//
////		services.add(new StatsAggregationService(this.config.getSnapshotsStorageFolder(), this.attributeConfigurationService,
////				this.config.getLogsFolder()));
//
//		final RealTimeAggregator ret = new RealTimeAggregator(services);
//
//		autowireBeanFactory.autowireBean(ret);
//
//		return ret;
//	}
//
//	public AggregatedData buildFromDatafragments(Stream<DataFragment> stream) {
//		return buildFromDatafragments(stream.collect(Collectors.toSet()));
//	}
//	public AggregatedData buildFromDatafragments(final Set<DataFragment> fragments) {
//
//		// TODO(gof) : make a spring instanciation
//		if (null == aggregator) {
//			logger.info("Instanciating real time aggregator");
//			aggregator = realTimeAggregator();
//		}
//
//		try {
//			return aggregator.build(fragments, null);
//		} catch (final NotAddedException e1) {
//			// TODO handle exceptions in this method
//			e1.printStackTrace();
//			return null;
//		}
//
//	}
//
//	public AggregatedData buildFromGtin(final String gtin) {
//
//		// TODO(gof) : make a spring instanciation
//		if (null == aggregator) {
//			logger.info("Instanciating real time aggregator");
//			aggregator = realTimeAggregator();
//		}
//
//		// TODO(gof) : validate numeric
//
//		// 1 - Get all with gtin
//		// TODO(gof) : limit from conf
//		final Set<DataFragment> fragments = repository
//				.export("referentielAttributes." + ReferentielKey.GTIN + ":" + gtin).limit(500)
//				.collect(Collectors.toSet());
//		logger.info("{} datafragments strictly match gtin {}", fragments.size(), gtin);
//
//		final Set<String> modelIds = fragments.stream().map(e -> e.brandUid()).filter(e -> null != e)
//				.collect(Collectors.toSet());
//		logger.info("{} brandUids found for {}", fragments.size(), gtin);
//
//		// Requesting for each model id
//		for (final String modelId : modelIds) {
//			final Set<DataFragment> brandUidFragments = repository
//					.export("referentielAttributes." + ReferentielKey.MODEL + ":\"" + modelId + "\"").limit(500)
//					.collect(Collectors.toSet());
//			logger.info("{} datafragments found for brandUid {}", brandUidFragments.size(), modelId);
//
//			for (final DataFragment df : brandUidFragments) {
//				if (null != df.gtin() && !df.gtin().equals(gtin)) {
//					logger.warn("Found conflicting gtin : {} for modelUid fragment {}", df, modelId);
//				} else {
//					fragments.add(df);
//				}
//			}
//		}
//
//		try {
//			return aggregator.build(fragments, null);
//		} catch (final NotAddedException e1) {
//			// TODO handle exceptions in this method
//			e1.printStackTrace();
//			return null;
//		}
//
//		// 2 - Retrieve brand-uid and request all datafragments having branduids
//		// 3 - Filter and alarm if unmatching gtin found
//
//	}
//
//
//
//}
