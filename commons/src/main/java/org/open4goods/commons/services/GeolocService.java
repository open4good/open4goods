//package org.open4goods.services;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import javax.annotation.PreDestroy;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.IOUtils;
//import org.rauschig.jarchivelib.Compressor;
//import org.rauschig.jarchivelib.CompressorFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import com.capsule.constants.TimeConstants;
//import com.capsule.exceptions.TechnicalException;
//import com.maxmind.geoip2.DatabaseReader;
//import com.maxmind.geoip2.model.CityResponse;
//
//public class GeolocService {
//
//	private static final Logger logger = LoggerFactory.getLogger(GeolocService.class);
//	/**
//	 * The file maxming geolite2 location file
//	 */
//	private final String mmdbCityFile;
//
//	/**
//	 * The URL of geolite Zip file
//	 */
//	private final String mmdbCityUrl;
//
//	private DatabaseReader geoloc;
//
//	/**
//	 * We use a state variable to enabel or not the request
//	 */
//	private AtomicBoolean initialized = new AtomicBoolean(false);
//
//	public GeolocService(final String mmdbCityFile, final String mmdbCityUrl) {
//		super();
//		this.mmdbCityFile = mmdbCityFile;
//		this.mmdbCityUrl = mmdbCityUrl;
//	}
//
//	@Scheduled(initialDelay = 100, fixedRate = TimeConstants.MAXMIND_DB_UPDATE)
//	public void initGeolocDatabase() {
//		final File existing = new File(this.mmdbCityFile);
//
//		// Disabling service
//		this.initialized = new AtomicBoolean(false);
//
//		// Closing DB in any case
//		IOUtils.closeQuietly(this.geoloc);
//
//		try {
//			if (!existing.exists()) {
//				// If file does not exist, download
//				logger.info("Geolocalisation mmdb file does not exists, download from {} to {}");
//				downloadMmdb(existing);
//
//			} else if (System.currentTimeMillis() - existing.lastModified() > TimeConstants.MAXMIND_DB_UPDATE) {
//				// If file exists but is too old, download
//				downloadMmdb(existing);
//			}
//
//			this.geoloc = new DatabaseReader.Builder(existing).build();
//
//			// Enabling service
//			this.initialized = new AtomicBoolean(true);
//		} catch (final Exception e) {
//			logger.error("Unable to initialize MaxMind engine, web geolocalisation will be disabled", e);
//			//TODO(design) : raise an alert (could be enough for an email...)
//		}
//	}
//
//
//	/**
//	 * Download and unzip mmdb file
//	 *
//	 * @param destination
//	 * @throws IllegalArgumentException
//	 * @throws IOException
//	 * @throws MalformedURLException
//	 * @throws FileNotFoundException
//	 */
//	private void downloadMmdb(final File destination) throws IllegalArgumentException, IOException  {
//		final File tmpFile = File.createTempFile("mmdb", ".tar.gz");
//		logger.warn("Downloading imdb file from {} to {} ",this.mmdbCityUrl,destination.getAbsolutePath());
//		FileUtils.copyURLToFile(new URL(this.mmdbCityUrl), tmpFile);
//
//		final Compressor compressor = CompressorFactory.createCompressor(tmpFile);
//		compressor.decompress(tmpFile, destination);
//
//		FileUtils.deleteQuietly(tmpFile);
//	}
//
//	@PreDestroy
//	public void shutdown() {
//		try {
//			logger.info("Shutdown geoloc");
//			IOUtils.closeQuietly(this.geoloc);
//		} catch (final Exception e) {
//			logger.error("Cannot shutdown geoloc", e);
//		}
//	}
//
//	/**
//	 * Get full gelocation informations from an inet
//	 *
//	 * @param inet
//	 * @return
//	 * @throws TechnicalException
//	 */
//	public CityResponse getByInet(final InetAddress inet) throws TechnicalException {
//		if (!this.initialized.get()) {
//			throw new TechnicalException("Geolocation is not enabled ");
//		}
//
//		try {
//			return this.geoloc.city(inet);
//		} catch (final Exception e) {
//			throw new TechnicalException("Error while geolocating " + inet, e);
//		}
//
//	}
//
//	/**
//	 * Get full gelocation informations from an IP
//	 *
//	 * @param inet
//	 * @return
//	 * @throws TechnicalException
//	 */
//	public CityResponse getByInet(final String ip) throws TechnicalException {
//
//			try {
//				return getByInet(InetAddress.getByName(ip));
//			} catch (final Exception e) {
//				throw new TechnicalException("Error while geolocating " + ip, e);
//			}
//
//	}
//
//}

