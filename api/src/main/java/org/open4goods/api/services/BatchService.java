package org.open4goods.api.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.store.DataFragmentStoreService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.helper.BoundedExecutor;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.data.DataFragment;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * This service is in charge of various batches
 * @author Goulven.Furet
 */
public class BatchService {



	private static final Logger logger = LoggerFactory.getLogger(BatchService.class);


	private DataFragmentRepository dataFragmentsRepository;

	private AggregatedDataRepository dataRepository;
	
	private VerticalsConfigService verticalsService;

	private final ApiProperties apiProperties;

	private Logger dedicatedLogger;

	public BatchService( final DataFragmentRepository dataFragmentsRepository,
								AggregatedDataRepository dataRepository,
								ApiProperties apiProperties,
								VerticalsConfigService verticalsService) {
		super();
		
		dedicatedLogger = GenericFileLogger.initLogger("stats-export", Level.INFO, apiProperties.logsFolder(), false);		
		this.dataFragmentsRepository = dataFragmentsRepository;
		this.apiProperties = apiProperties;
		this.dataRepository =dataRepository;
		this.verticalsService=verticalsService;
	}


	/**
	 * The batch used to associate verticals on AggregatedDatas
	 */
	public void definesVertical() {
		
		
		dedicatedLogger.info("Starting batch verticalisation");
		dataRepository.exportAll().forEach(e -> {
		
			// Getting the config for the category, if any
			for (String cat : e.getDatasourceCategories()) {
				
				VerticalConfig vConf = verticalsService.getVerticalForCategoryName(cat);
				
				if (null != vConf) {
					// We have a match. Associate vertical ID annd save
					e.setVertical(vConf.getId());
					
					// Index
					//TODO : Bulk index for performance
					dedicatedLogger.info("Vertical {} for vertical {}", vConf.getId() , e.bestName());

					dataRepository.index(e);
					
				}
				
			}
			
			dedicatedLogger.info("End batch verticalisation");

			
		});
		
		
	}
	

	
	
	

}
