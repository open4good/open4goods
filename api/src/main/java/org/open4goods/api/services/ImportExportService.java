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
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.helper.BoundedExecutor;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.data.DataFragment;
import org.open4goods.services.SerialisationService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * This service is in charge of backuping datafragments in a flat file, and to
 * delete old used ones
 * 
 * @author Goulven.Furet
 *
 *         TODO(P3,perf,0.5) : compress backup file
 *         TODO(P3,perf,0.5) : Quiet a "unzipped legacy mode" and a "http compressed mode". Split services, organize
 *         TODO : Leverage the "cleanup on export phase" ?
 */
public class ImportExportService {


	private static final String SAVED_FILE = "saved.backup";

	private static final String TODELETE_FILE = "todelete.backup";

	private static final Logger logger = LoggerFactory.getLogger(ImportExportService.class);


	DataFragmentRepository dataFragmentsRepository;

	private final DataFragmentStoreService storeService;

	private final SerialisationService serialisationService;


	private final ApiProperties apiProperties;

	private Logger statsLogger;

	public ImportExportService( final DataFragmentRepository dataFragmentsRepository,
			final DataFragmentStoreService storeService, final SerialisationService serialisationService, ApiProperties apiProperties) {
		super();
		
		statsLogger = GenericFileLogger.initLogger("stats-export", Level.INFO, apiProperties.logsFolder(), false);
		
		this.dataFragmentsRepository = dataFragmentsRepository;
		this.storeService = storeService;
		this.serialisationService = serialisationService;
		this.apiProperties = apiProperties;
	}

	/**
	 * The thread used to parallelize import task
	 * 
	 * @author goulven
	 *
	 */
	class ImportTask implements Runnable {
		private final List<DataFragment> dfs;
		private long actualLines;

		public ImportTask(List<DataFragment> dfs, long actual) {
			this.dfs = dfs;
			this.actualLines = actual;
		}

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			
			dataFragmentsRepository.save(dfs);
			
			logger.info("ImportWorker has indexed {} DataFragments in {}ms. (lines processed : {})",  dfs.size(),System.currentTimeMillis()-now, actualLines);

			
			
		}
	}

	/**
	 * Stream datafragments to a ziped file on HttpResponse. The export filtering is
	 * applied
	 * 
	 * @param response
	 * @param zipFilename
	 * @throws IOException
	 */
	public void dataFragmentsfragmentsToHttpResponse(HttpServletResponse response, String zipFilename, Long maxFragments)
			throws IOException {

		// setting headers
		response.setStatus(HttpServletResponse.SC_OK);
		response.addHeader("Content-Disposition", "attachment; filename=\"" + zipFilename + ".zip\"");

		
		ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

		zipOutputStream.putNextEntry(new ZipEntry("datafragments.json"));

		Stream<DataFragment> fragments = dataFragmentsRepository.export("*");
		final AtomicLong counter = new AtomicLong();
			fragments
				.takeWhile(f -> counter.incrementAndGet() < maxFragments)
				.forEach(f -> {
				String fragment = serialisationService.toJson(transformExport(f)) + "\n";
				try (InputStream str = IOUtils.toInputStream(fragment, Charset.defaultCharset())) {
					IOUtils.copy((str), zipOutputStream);
				} catch (IOException e) {
					logger.error("Error closing stream",e);
				}

		});
		zipOutputStream.closeEntry();

		zipOutputStream.close();
	}
	
	
	
	/**
	 * Operates the import of datafragments from files (no updates, raw inserts)
	 * @throws InvalidParameterException 
	 */
	public void doImport() throws InvalidParameterException {



		logger.info("Importing file");
		final File saved = new File(apiProperties.getDatafragmentsBackupFolder() + "/" + SAVED_FILE);

		Path path = Paths.get(saved.toURI());
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(apiProperties.getDataFragmentsDequeueWorkers());

		BoundedExecutor boundedExecutor = new BoundedExecutor(executor, apiProperties.getDataFragmentsDequeueWorkers());
		
		AtomicLong counter = new AtomicLong(0L);
		try {

			List<DataFragment> dfs = new ArrayList<DataFragment>();
			Files.lines(path).forEach(e -> {
				try {

					DataFragment frag = serialisationService.fromJson(e, DataFragment.class);
					// Applying transformation rules
					transformImport(frag);
					
					dfs.add(frag);
					if (dfs.size() > apiProperties.getDataFragmentsDequeueSize()) {
						
						long actual = counter.addAndGet(dfs.size());
						ImportTask task = new ImportTask(new ArrayList<>(dfs), actual);
						dfs.clear();
						boundedExecutor.submitTask(task);
					}

				} catch (Exception e1) {
					logger.error("Deserialisation error", e);
				}

			});

			// Remaining items
			dataFragmentsRepository.save(dfs);
		} catch (IOException e) {
			logger.error("Error accessing import file at {}", saved.getAbsolutePath(), e);
		}

		executor.shutdown();

	}


	/**
	 * Sends a mail with all queued messages
	 * @throws InvalidParameterException 
	 */
//	@Scheduled(initialDelay = TimeConstants.DATAFRAGMENT_EXPORT_AND_CLEANUP_FREQUENCY, fixedDelay = TimeConstants.DATAFRAGMENT_EXPORT_AND_CLEANUP_FREQUENCY)
	public void exportAndCleanup() throws InvalidParameterException {



		final Long now = System.currentTimeMillis();

	

		new File(apiProperties.getDatafragmentsBackupFolder()).mkdirs();

		// Creating file descriptors
		final File toDelete = new File(apiProperties.getDatafragmentsBackupFolder() + "/" + TODELETE_FILE);
		final File saved = new File(apiProperties.getDatafragmentsBackupFolder() + "/" + SAVED_FILE);

		try {
			toDelete.createNewFile();
			saved.createNewFile();
		} catch (final IOException e1) {
			logger.error("Error while creating backup files", e1);
		}

		final AtomicLong savedCounter = new AtomicLong();
		final AtomicLong deletedCounter = new AtomicLong();

		try {
			final FileWriter toDeleteWriter = new FileWriter(toDelete, false);
			final FileWriter savedWriter = new FileWriter(saved, false);

			// Pass on each items
			dataFragmentsRepository.export("*").forEach(df -> {

				try {
					if (mustPurge(df)) {
						// This item must be deleted
						toDeleteWriter.write(serialisationService.toJson(df));
						toDeleteWriter.write("\n");
						deletedCounter.incrementAndGet();
					} else {
						// This item must be backuped

						// Apply sanitisations rules
						transformExport(df);

						savedWriter.write(serialisationService.toJson(df));
						savedWriter.write("\n");
						savedCounter.incrementAndGet();

					}
				} catch (final Exception e) {
					logger.error("Error while serialising a datafragment : {}", df, e);
				}
			});

			// NOTE(gof) : not properly closed if exception
			IOUtils.closeQuietly(toDeleteWriter);
			IOUtils.closeQuietly(savedWriter);

			// Delete the toDelete items

			// read file into stream, try-with-resources

			// TODO(feature) : enable effectiv deletion
			try (Stream<String> stream = Files.lines(Paths.get(toDelete.getAbsolutePath()))) {

				stream.forEach(line -> {
					try {
						final DataFragment df = serialisationService.fromJson(line, DataFragment.class);

						dataFragmentsRepository. deleteById (df.getUrl());

					} catch (final Exception e) {
						logger.error("Error while deleting a datafragment", e);
					}

				});

			} catch (final IOException e) {
				logger.error("Error while reading todelete file", e);
			}

		} catch (final IOException e) {
			logger.error("Error while exporting and cleaning", e);
		}



		statsLogger.warn( " items backuped, " + deletedCounter.longValue() + " removed in " + (System.currentTimeMillis() - now) + "ms"	);

	}

	
	
	
	
	
	/**
	 * Apply transformation on the DataFragment in the import phase (flat file to
	 * elastic)
	 * 
	 * @param df
	 * @return
	 */
	private void transformImport(DataFragment df) {
		
	
		

	}

	/**
	 *
	 * @param df
	 * @return 
	 */
	public DataFragment transformExport(DataFragment df) {
	
		
		return df;
	}

	/**
	 * Return true if a dataFragment must be deleted (exported into "TODELETE" file, false otherwise
	 * 
	 * @param df
	 * @return
	 */
	private boolean mustPurge(final DataFragment df) {


		return false;
	}
	
	
	
	
	
	

	
	
	

}
