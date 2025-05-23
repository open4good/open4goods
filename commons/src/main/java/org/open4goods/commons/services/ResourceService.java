package org.open4goods.commons.services;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This service is in charge of external resources handling (caching, metadatas, ...)
 * @author goulven
 *

 */
public class ResourceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

	private final String remoteCachingFolder;


	public ResourceService(final String resourceFolder) {
		super();
		remoteCachingFolder = resourceFolder;

		// Creating folder structure
		initCacheFolders();
	}



	/**
	 * Initialize the folders layout
	 */
	public void initCacheFolders() {
		final File cacheFolder = new File(remoteCachingFolder);
		boolean ret = cacheFolder.mkdirs();

		LOGGER.info("Generating uidMap folders, make take a while");
		final String[] prefixes = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G",
				"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
		for (final String l1 : prefixes) {
			for (final String l2 : prefixes) {
				for (final String l3 : prefixes) {
					ret = new File(cacheFolder.getAbsolutePath() + File.separator + l1 + File.separator + l2 + File.separator+l3+File.separator).mkdirs();
					if (!ret) {
						LOGGER.info("Skipping uidMap folders generation, they already exists.");
						return;
					}
				}
			}
		}
	}


	/**
	 * Return an inputStream to the cached file
	 * @param r
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public InputStream getResourceFileStream(final Resource r) throws FileNotFoundException, IOException {
		return IOUtils.toBufferedInputStream(new FileInputStream(getCacheFile(r)));

	}


	public File getCacheFile(final Resource r) {
		return new File(remoteCachingFolder + File.separator+ r.folderHashPrefix() + File.separator+ r.getCacheKey());
	}


	public File getCacheFile(final String hash) throws ValidationException {
		return new File(remoteCachingFolder + File.separator + Resource.folderHashPrefix(hash) + File.separator + hash);
	}




	public String getRemoteCachingFolder() {
		return remoteCachingFolder;
	}





}