package org.open4goods.commons.services;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.exceptions.TechnicalException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.CacheResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
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
 * @author goulven
 *

 * TODO(P3,test) : unit tests
 */
public class RemoteFileCachingService {


	private final Map<String,CacheResourceConfig> configs = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(RemoteFileCachingService.class);

	private static final int BUFFER_SIZE = 2048;

	private final String resourceFolder;
	
	// TODO(p3,design) : Inject
	private RestTemplate restTemplate = new RestTemplate();


	public RemoteFileCachingService(final String resourceFolder) {
		super();
		this.resourceFolder = resourceFolder;
		final File rFolder = new File(resourceFolder);
		rFolder.mkdirs();
	}


	public void cacheResource(final CacheResourceConfig resourceConfig) {
		configs.put(resourceConfig.getUrl(), resourceConfig);
	}


	public File getResource(final String url) throws InvalidParameterException {

		File resource = new File(resourceFolder+File.separator+IdHelper.getHashedName(url));

		CacheResourceConfig conf = configs.get(url);
		if (null == conf) {
			logger.info("No cache config, caching with default config",url );
			conf = new CacheResourceConfig();
			conf.setUrl(url);
			configs.put(url, conf);
		}

		try {
			if (!resource.exists()) {
				logger.info("Resource {} does not exists, will download it",url);
				resource = retrieve(conf);
			} else if ((System.currentTimeMillis() - resource.lastModified()) > (conf.getRefreshInDays() * 24 *3600 * 1000)) {
				logger.warn("Resource {} is outdated, will replace it",url);
				resource = retrieve(conf);
			}
		} catch (final Exception e) {
			logger.error("Resource {} cannot be retrieved : {}",url,e.getMessage() );
		}

		return resource;

	}



	private File retrieve(final CacheResourceConfig conf) throws TechnicalException {

		try {
			File tmpFile = new File(resourceFolder+File.separator+"tmp-"+IdHelper.getHashedName(conf.getUrl()));
			File destFile = new File(resourceFolder+File.separator+IdHelper.getHashedName(conf.getUrl()));

			tmpFile = download(conf.getUrl(), tmpFile);



			if (conf.getUnzip()) {
				destFile = unzip(tmpFile,destFile,conf.getExtractedFileName());
			} else {
				tmpFile.renameTo(destFile);
			}

			FileUtils.deleteQuietly(tmpFile);
			
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
     * @throws IOException If an I/O error occurs during file download or creation.
     */
    public File downloadToTmpFile(String url, String safeName) throws IOException {
        // Create a temporary file with a normalized safe name
        File destFile = File.createTempFile("csv", IdHelper.normalizeFileName(safeName) + ".csv");

        logger.info("Downloading CSV for '{}' from '{}' to '{}'", safeName, url, destFile.getAbsolutePath());

        try {
            // Handle HTTP/HTTPS URLs
            if (url.startsWith("http")) {
                FileUtils.copyURLToFile(new URL(url), destFile);
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
        } catch (IOException e) {
            // Log the error and rethrow the exception for the caller to handle
            logger.error("Failed to download file from '{}' to '{}'", url, destFile.getAbsolutePath(), e);
            throw e;
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


		try {
			logger.info("Downloading resource  from {} to {}", url, tmpFile);
			FileUtils.copyURLToFile(new URL(url), tmpFile);
			return tmpFile;
		} catch (Exception e) {
			throw new TechnicalException("Cannot download resource " + url  + " : " + e.getMessage());
		}
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