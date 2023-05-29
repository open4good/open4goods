package org.open4goods.services;
import java.io.File;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Resource;
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
					ret = new File(cacheFolder.getAbsolutePath() + "/" + l1 + "/" + l2 + "/"+l3+"/").mkdirs();
					if (!ret) {
						LOGGER.info("Skipping uidMap folders generation, they already exists.");
						return;
					}
				}
			}
		}
	}

	public File getCacheFile(final Resource r) {
		return new File(remoteCachingFolder + "/" + r.folderHashPrefix() + "/" + r.getCacheKey());
	}

	public File getTranslatedCacheFile(final Resource r) throws ValidationException {
		return new File(remoteCachingFolder + "/" + r.folderHashPrefix() + "/" + r.getCacheKey() + "_ORIGINAL.png");
	}

	public File getThumbnailCacheFile(final Resource r, final Integer height) throws ValidationException {
		return new File(
				remoteCachingFolder + "/" + r.folderHashPrefix() + "/" + r.getCacheKey() + "_" + height + ".png");
	}

	public File getCacheFile(final String hash) throws ValidationException {
		return new File(remoteCachingFolder + "/" + Resource.folderHashPrefix(hash) + "/" + hash);
	}

	public File getTranslatedCacheFile(final String hash) throws ValidationException {
		return new File(remoteCachingFolder + "/" + Resource.folderHashPrefix(hash) + "/" + hash + "_ORIGINAL.png");
	}

	public File getThumbnailCacheFile(final String hash, final Integer height) throws ValidationException {
		return new File(
				remoteCachingFolder + "/" + Resource.folderHashPrefix(hash) + "/" + hash + "_" + height + ".png");
	}



	public String getRemoteCachingFolder() {
		return remoteCachingFolder;
	}





}