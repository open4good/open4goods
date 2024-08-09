package org.open4goods.services;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.CacheResourceConfig;
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
 * @author goulven
 *

 * TODO(0.25,P3,test) : unit tests
 */
public class RemoteFileCachingService {


	private final Map<String,CacheResourceConfig> configs = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(RemoteFileCachingService.class);

	private final String resourceFolder;
	
	// TODO : Inject
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

	
	
	// TODO : Mutualize with CsvIndexationWorker
    public  void decompressGzipFile(File gzipFile, File newFile) {
        try {
            FileInputStream fis = new FileInputStream(gzipFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            int len;
            while((len = gis.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
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