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
 * Service in charge of external resources handling, cached files storage,
 * and retrieval.
 *
 * @author goulven
 */
public class ResourceService
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    private final String remoteCachingFolder;

    /**
     * Constructs a ResourceService with the specified caching folder.
     *
     * @param resourceFolder the caching directory path
     */
    public ResourceService(final String resourceFolder)
    {
        super();
        this.remoteCachingFolder = resourceFolder;
        
        // Ensure cache base directory exists
        final File cacheFolder = new File(remoteCachingFolder);
        if (!cacheFolder.exists())
        {
            cacheFolder.mkdirs();
        }
    }

    /**
     * Returns an input stream to the cached resource file.
     *
     * @param r the resource reference
     * @return a buffered InputStream reading the cached file
     * @throws FileNotFoundException if the cached file does not exist
     * @throws IOException if an error occurs reading the file
     */
    public InputStream getResourceFileStream(final Resource r) throws FileNotFoundException, IOException
    {
        return IOUtils.toBufferedInputStream(new FileInputStream(getCacheFile(r)));
    }

    /**
     * Retrieves the file handle in the caching directory structure for a Resource,
     * ensuring its parent directory structure is dynamically created on-demand.
     *
     * @param r the resource reference
     * @return the cached File handle
     */
    public File getCacheFile(final Resource r)
    {
        File file = new File(remoteCachingFolder + File.separator + r.folderHashPrefix() + File.separator + r.getCacheKey());
        File parent = file.getParentFile();
        if (parent != null && !parent.exists())
        {
            if (!parent.mkdirs())
            {
                LOGGER.error("Failed to create parent directory for resource cache file: {}", parent.getAbsolutePath());
            }
        }
        return file;
    }

    /**
     * Retrieves the file handle in the caching directory structure for a specific hash,
     * ensuring its parent directory structure is dynamically created on-demand.
     *
     * @param hash the cache key or hash string
     * @return the cached File handle
     * @throws ValidationException if the hash formatting is invalid
     */
    public File getCacheFile(final String hash) throws ValidationException
    {
        File file = new File(remoteCachingFolder + File.separator + Resource.folderHashPrefix(hash) + File.separator + hash);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists())
        {
            if (!parent.mkdirs())
            {
                LOGGER.error("Failed to create parent directory for resource cache file of hash: {}", parent.getAbsolutePath());
            }
        }
        return file;
    }

    /**
     * Returns the root caching folder path.
     *
     * @return root caching folder path
     */
    public String getRemoteCachingFolder()
    {
        return remoteCachingFolder;
    }
}