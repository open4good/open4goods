package org.open4goods.services.remotefilecaching.service;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.config.CacheResourceConfig;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;


/**
 * This service is in charge of periodicaly download and make availlable remote files. It also allows to unzip / untar
 * <p>
 * Cached files are named after the SHA-256 of their URL (see {@link #cacheFileName(String)}): remote URLs can carry
 * credentials (API keys as path segments or query values), which must not show up in a directory listing. Copies
 * left under the former URL-derived name are adopted on first access (see {@link #resolveCacheFile(String, String)}).
 * @author goulven
 */
public class RemoteFileCachingService {


	private final Map<String,CacheResourceConfig> configs = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(RemoteFileCachingService.class);

        private static final int BUFFER_SIZE = 2048;

        /** Prefix of the file a resource is downloaded to before it replaces the cached copy. */
        private static final String TMP_PREFIX = "tmp-";

        /** Replaces whatever part of a URL is not logged. */
        private static final String REDACTED = "<redacted>";

        /** HTTP status code quoted by {@link java.net.HttpURLConnection} failures. */
        private static final Pattern HTTP_STATUS = Pattern.compile("HTTP response code: (\\d{3})");

        private final int connectionTimeout;
        private final int readTimeout;

	private final String resourceFolder;
	
	// TODO(p3,design) : Inject
	private RestTemplate restTemplate = new RestTemplate();


        public RemoteFileCachingService(final String resourceFolder) {
                this(resourceFolder, new RemoteFileCachingProperties());
        }

        public RemoteFileCachingService(final String resourceFolder, RemoteFileCachingProperties properties) {
                super();
                this.resourceFolder = resourceFolder;
                this.connectionTimeout = properties.getConnectionTimeout();
                this.readTimeout = properties.getReadTimeout();
                final File rFolder = new File(resourceFolder);
                rFolder.mkdirs();
        }


	public void cacheResource(final CacheResourceConfig resourceConfig) {
		configs.put(resourceConfig.getUrl(), resourceConfig);
	}


	public File getResource(final String url, Integer refreshInDays) throws InvalidParameterException, IOException {
		return getResource(url, refreshInDays, null);
	}

	/**
	 * Returns the cached copy of a remote resource, downloading it when missing or older than
	 * {@code refreshInDays}.
	 *
	 * @param url remote resource URL
	 * @param refreshInDays cache TTL, in days
	 * @param logLabel label written in diagnostic logs instead of the URL; {@code null} to log only the scheme,
	 *        host and port of the URL. Either way the log line also carries the cache file name
	 * @return the cached file, which may not exist when the resource could never be retrieved
	 */
	public File getResource(final String url, Integer refreshInDays, String logLabel) throws InvalidParameterException, IOException {

		File resource = resolveCacheFile(resourceFolder, url);

		CacheResourceConfig conf = configs.get(url);
		if (null == conf) {
			conf = new CacheResourceConfig();
			conf.setRefreshInDays(refreshInDays);
			conf.setUrl(url);
			conf.setLogLabel(logLabel);
			configs.put(url, conf);
			logger.info("No cache config for resource {}, caching with default config", loggableResource(conf));
		}

		try {
			if (!resource.exists()) {
				logger.info("Resource {} does not exist, will download it", loggableResource(conf));
				resource = retrieve(conf);
			} else if ((System.currentTimeMillis() - resource.lastModified()) > TimeUnit.DAYS.toMillis(conf.getRefreshInDays())) {
				logger.warn("Resource {} is outdated, will replace it", loggableResource(conf));
				resource = retrieve(conf);
			}
		} catch (final Exception e) {
			logger.error("Resource {} cannot be retrieved ({})", loggableResource(conf), e.getClass().getSimpleName());
		}

		return resource;

	}



	public File retrieve(final CacheResourceConfig conf) throws TechnicalException {

		try {
			String cacheFileName = cacheFileName(conf.getUrl());
			File tmpFile = new File(resourceFolder+File.separator+TMP_PREFIX+cacheFileName);
			File destFile = new File(resourceFolder+File.separator+cacheFileName);

			tmpFile = download(conf.getUrl(), tmpFile, loggableResource(conf));



			if (conf.getUnzip()) {
				destFile = unzip(tmpFile,destFile,conf.getExtractedFileName());
			} else {
				tmpFile.renameTo(destFile);
			}

			FileUtils.deleteQuietly(tmpFile);
			// A fresh copy now exists under the hashed name: drop the one named after the URL, if still there.
			deleteLegacyCacheFiles(resourceFolder, conf.getUrl());
			
			return destFile;
		} catch (TechnicalException e) {
			throw e;
		} catch (Exception e) {
			throw new TechnicalException("Error retrieving resource",e);
		}
	}

	
	
	 /**
     * Decompresses a GZIP file to the specified output file.
     * Utilizes a buffer for efficient reading and writing.
     *
     * @param gzipFile The GZIP file to decompress.
     * @param newFile  The file to write the decompressed data to.
     */
    public void decompressGzipFile(File gzipFile, File newFile) {
        // Using try-with-resources to ensure streams are closed automatically
        try (FileInputStream fis = new FileInputStream(gzipFile);
             GZIPInputStream gis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(newFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            // Read from GZIPInputStream and write to FileOutputStream
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }

            logger.info("Successfully decompressed {} to {}", gzipFile.getAbsolutePath(), newFile.getAbsolutePath());

        } catch (IOException e) {
            // Log the error with specific details about the input/output files
            logger.error("Error occurred while decompressing GZIP file '{}' to '{}'.", 
                         gzipFile.getAbsolutePath(), newFile.getAbsolutePath(), e);
        }
    }

    /**
     * Downloads a file from the given URL to a temporary file with a specified safe name.
     *
     * @param url      The URL of the file to download.
     * @param safeName A safe name to use as part of the temporary file name.
     * @return The temporary file where the content has been downloaded.
     * @throws IOException If an I/O error occurs during file download or creation; the message never quotes
     *         the URL (see {@link #redactedCause(Exception)}).
     */
    public File downloadToTmpFile(String url, String safeName) throws IOException {
        // Create a temporary file with a normalized safe name
        File destFile = File.createTempFile("csv", IdHelper.normalizeFileName(safeName) + ".csv");

        logger.info("Downloading CSV for '{}' from '{}' to '{}'", safeName, loggableUrl(url), destFile.getAbsolutePath());

        try {
            // Handle HTTP/HTTPS URLs
            if (url.startsWith("http")) {
                FileUtils.copyURLToFile(new URL(url), destFile, connectionTimeout, readTimeout);
                
            }
            // Handle classpath resources if necessary (currently commented out)
            // else if (url.startsWith(CLASSPATH_PREFIX)) {
            //     ClassPathResource res = new ClassPathResource(url.substring(CLASSPATH_PREFIX.length()));
            //     FileUtils.copyInputStreamToFile(res.getInputStream(), destFile);
            // }
            // Handle local file paths
            else {
                destFile = new File(url);
            }

            logger.info("File successfully downloaded to '{}'", destFile.getAbsolutePath());
        } catch (Exception e) {
        	// Delete the tmp file
        	FileUtils.deleteQuietly(destFile);
            // JDK messages quote the URL, which can carry the API key of the feed
            throw redactedCause(e);
        }

        return destFile;
    }
    
    
    

	public File decompressGzipAndDeleteSource(File destFile) throws IOException {
		File tmpFile = File.createTempFile("gzip", "gzip");
		decompressGzipFile(destFile, tmpFile);
		Files.delete(destFile.toPath());
		return new File(tmpFile.getAbsolutePath());
	}

	public File unzipFileAndDeleteSource(File sourceFile) throws IOException {
		String targetFolder = sourceFile.getParent() + File.separator + "unzipped";
		logger.info("Unzipping CSV data from {} to {}", sourceFile.getAbsolutePath(), targetFolder);
		new File(targetFolder).mkdirs();

		try (ZipFile zipFile = new ZipFile(sourceFile)) {
			zipFile.extractAll(targetFolder);
		} catch (ZipException e) {
			logger.error("Error extracting CSV data", e);
			throw e;
		}

		FileUtils.deleteQuietly(sourceFile);
		File zipedDestFolder = new File(targetFolder);

		if (zipedDestFolder.list().length > 1) {
			logger.error("Multiple files in {}, cannot operate", sourceFile.getAbsolutePath());
			throw new IOException("Multiple files in zip archive");
		}

		return zipedDestFolder.listFiles()[0];
	}

    
    
    

	// TODO : Should not expose the tmpFile
	public File unzip(final File tmpFile, final File destFile, String fileToKeep) throws ZipException {
		final ZipFile zipFile = new ZipFile(tmpFile);
		//		File zipedDestFile = File.createTempFile("csv_zipped", dsProperties.getName());

		final String targetFolder = tmpFile.getParent() + File.separator+  "unziped";

		logger.info("Unzipping CSV data from {} to {}", tmpFile.getAbsolutePath(), targetFolder);

		new File(targetFolder).mkdirs();
		zipFile.extractAll(targetFolder);

		final File zipedDestFolder = new File(targetFolder);
		File res = null;


		if (zipedDestFolder.list().length > 1) {

			for (final File child : zipedDestFolder.listFiles()) {
				if (null != fileToKeep && child.getName().equals(fileToKeep)) {
					res = child;
					break;
				}
			}
		} else {
			res = zipedDestFolder.listFiles()[0];
		}

		if (null == res) {
			logger.warn("No result files in zip extraction, of ultiple and no extractedfilename in config");
		} else {
			res.renameTo(destFile);
		}

		IOUtils.closeQuietly(zipFile);
		destFile.setLastModified(System.currentTimeMillis());
		return destFile;

	}

	/**
	 * TODO : Should use a streamed, threaded pool caped version
	 * @param url
	 * @param tmpFile
	 * @return
	 * @throws TechnicalException
	 */
	public File download(final String url,final File tmpFile) throws TechnicalException {
		return download(url, tmpFile, loggableUrl(url));
	}

	private File download(final String url, final File tmpFile, final String logLabel) throws TechnicalException {
		try {
			logger.info("Downloading resource from {} to {}", logLabel, tmpFile);
			FileUtils.copyURLToFile(new URL(url), tmpFile, connectionTimeout, readTimeout);
			return tmpFile;
		} catch (Exception e) {
			throw new TechnicalException("Cannot download resource " + logLabel, redactedCause(e));
		}
	}

	/**
	 * Returns the name of the file caching {@code url}: the hexadecimal SHA-256 of the full URL. No fragment of the
	 * URL is kept, so credentials it carries never appear in the cache folder, and distinct URLs never share a file.
	 *
	 * @param url remote resource URL
	 * @return 64 lowercase hexadecimal characters
	 */
	public static String cacheFileName(final String url) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(url.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 is mandatory on every Java platform
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	/**
	 * Returns the file caching {@code url} in {@code folder}, named by {@link #cacheFileName(String)}.
	 * <p>
	 * Cached files used to be named after the URL itself ({@link IdHelper#getHashedName(String)}), which
	 * exposed API keys in the folder listing. When no hashed copy exists yet, a copy left under that former name is
	 * moved to the hashed name, keeping its modification date so the refresh period still applies; a leftover
	 * former temporary file is deleted.
	 *
	 * @param folder cache folder
	 * @param url remote resource URL
	 * @return the cache file, which may not exist
	 */
	public static File resolveCacheFile(final String folder, final String url) {
		File cacheFile = new File(folder + File.separator + cacheFileName(url));
		if (!cacheFile.exists()) {
			File legacyFile = legacyCacheFile(folder, url);
			if (legacyFile.isFile() && !legacyFile.renameTo(cacheFile)) {
				logger.warn("Cannot move a cached copy named after its URL to {}", cacheFile.getName());
			}
		}
		deleteLegacyCacheFiles(folder, url);
		return cacheFile;
	}

	/**
	 * Deletes the files named after {@code url} that the former naming scheme left in {@code folder}.
	 *
	 * @param folder cache folder
	 * @param url remote resource URL
	 */
	private static void deleteLegacyCacheFiles(final String folder, final String url) {
		File legacyFile = legacyCacheFile(folder, url);
		File legacyTmpFile = new File(folder + File.separator + TMP_PREFIX + legacyFile.getName());
		for (File file : new File[] {legacyFile, legacyTmpFile}) {
			if (file.isFile()) {
				FileUtils.deleteQuietly(file);
			}
		}
	}

	private static File legacyCacheFile(final String folder, final String url) {
		return new File(folder + File.separator + IdHelper.getHashedName(url));
	}

	/**
	 * Returns how a cached resource appears in diagnostic logs: its log label when set, otherwise the redacted URL
	 * of {@link #loggableUrl(String)}, followed by the cache file name so that the line can be matched to the file.
	 *
	 * @param conf cached resource configuration
	 * @return text safe to log
	 */
	static String loggableResource(CacheResourceConfig conf) {
		String label = conf.getLogLabel() != null ? conf.getLogLabel() : loggableUrl(conf.getUrl());
		return label + " [" + cacheFileName(conf.getUrl()) + "]";
	}

	/**
	 * Reduces a URL to its scheme, host and port before diagnostic logging: remote resource URLs can carry
	 * credentials as path segments (Effinity, Awin), query values (MaxMind, signed URLs) or user information.
	 *
	 * @param url remote resource URL
	 * @return redacted URL, or {@code null} when no URL was supplied
	 */
	public static String loggableUrl(String url) {
		if (url == null) {
			return null;
		}
		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException e) {
			return REDACTED;
		}
		if (uri.getScheme() == null || uri.getHost() == null) {
			return REDACTED;
		}
		StringBuilder ret = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
		if (uri.getPort() >= 0) {
			ret.append(':').append(uri.getPort());
		}
		boolean hasPath = uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !"/".equals(uri.getRawPath());
		if (hasPath || uri.getRawQuery() != null || uri.getRawFragment() != null) {
			ret.append('/').append(REDACTED);
		}
		return ret.toString();
	}

	/**
	 * Rebuilds a download failure without its message: JDK messages quote the URL or the path it points to (for
	 * example {@code Server returned HTTP response code: 403 for URL: ...}), and the exception ends up in caller logs.
	 * Only the HTTP status code, when the message gives one, is kept.
	 *
	 * @param e download failure
	 * @return an exception carrying the class name, the HTTP status code and the stack trace of {@code e}, without
	 *         cause
	 */
	static IOException redactedCause(final Exception e) {
		StringBuilder message = new StringBuilder(e.getClass().getName());
		Matcher status = e.getMessage() == null ? null : HTTP_STATUS.matcher(e.getMessage());
		if (status != null && status.find()) {
			message.append(" (HTTP ").append(status.group(1)).append(')');
		}
		IOException redacted = new IOException(message.toString());
		redacted.setStackTrace(e.getStackTrace());
		return redacted;
	}
	
	
	/**
	 * TODO : Should mutualize
	 * @param user
	 * @param password
	 * @param url
	 * @param dest
	 * @return
	 */
	public void downloadTo( String user, String password, String url, File dest ){
		restTemplate.execute(
			    url,
			    HttpMethod.POST,
			    new RequestCallback() {
			        @Override
			        public void doWithRequest(ClientHttpRequest request) throws IOException {
			        	if (user != null && password != null) {
			        		request.getHeaders().add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":"+password) .getBytes()));			        		
			        	}
			        }
			    },
			    new ResponseExtractor<ResponseEntity<Void>>() {
			        @Override
			        public ResponseEntity<Void> extractData(ClientHttpResponse response) throws IOException {
			            FileOutputStream fos = new FileOutputStream(dest);
			        	IOUtils.copy(response.getBody(), fos);
			        	fos.close();
						return null;
			        }
			    }
			);

	}

}
