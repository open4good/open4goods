package org.open4goods.model.resource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Validable;
import org.open4goods.model.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources represents documents, images, binaries, etc. associated with a
 * product or a DataFragment. They are also indexed externally (in a dedicated
 * index) at the UI level, to be able to handle efficient caching, resizing,
 * mimetype detection and conversions.
 *
 * @author goulven
 */
public class Resource implements Validable
{

    private static final Logger logger = LoggerFactory.getLogger(Resource.class);

    private String url;

    private String mimeType;

    private Long timeStamp;

    private String cacheKey;

    // If true, this media has been tested and is not retained
    private boolean evicted = false;

    // If true, this media has been downloaded and analysed
    private boolean processed = false;

    // A complementary status, on eviction cause, or whatever
    private ResourceStatus status;

    private Long fileSize;

    private String fileName;

    private String extension;

    private String md5;

    private ResourceType resourceType;

    private ImageInfo imageInfo;

    private PdfInfo pdfInfo;

    // The group (similarity based and popularity ranked) this resource belongs to
    // TODO: Move into imageinfo
    private Integer group;

    private String datasourceName;

    /**
     * From ResourceTagDictionary
     */
    private Set<String> tags = new HashSet<>();

    private Set<ResourceTag> hardTags = new HashSet<>();

    /**
     * Default constructor.
     */
    public Resource()
    {
        super();
    }

    /**
     * Constructs a resource with a URL and validates it.
     *
     * @param url the resource URL
     * @throws ValidationException if the URL is null or empty
     */
    public Resource(String url) throws ValidationException
    {
        super();

        if (StringUtils.isBlank(url))
        {
            throw new ValidationException("url cannot be null");
        }
        if (url.startsWith("//"))
        {
            url = "http:" + url;
        }
        setUrl(url);
    }

    /**
     * Gets the first hard tag associated with the resource.
     *
     * @return the first ResourceTag, or null if none
     */
    public ResourceTag firstHardTag()
    {
        return hardTags.stream().findFirst().orElse(null);
    }

    @Override
    public String toString()
    {
        return getUrl();
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj instanceof Resource o)
        {
            return getUrl().equals(o.getUrl());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return getUrl().hashCode();
    }

    @Override
    public void validate() throws ValidationException
    {
        try
        {
            new URL(getUrl());
        }
        catch (final MalformedURLException e)
        {
            throw new ValidationException("invalid URL : " + getUrl());
        }
    }

    /**
     * Determines the best name for the resource from its tags.
     *
     * @return the best name string
     */
    public String bestNameFromTag()
    {
        return tags.stream().filter(e -> !e.contains(".")).findAny().orElse(nameFromUrl());
    }

    /**
     * UI Helper to generate the path for the resource.
     *
     * @return path string
     */
    public String path()
    {
        return path(null);
    }

    /**
     * UI Helper to generate the path for the resource with an optional width limit.
     *
     * @param width the target image width
     * @return path string
     */
    public String path(Integer width)
    {
        StringBuilder sb = new StringBuilder();

        // TODO: share const with the resourcecontroller
        switch (resourceType)
        {
            case IMAGE:
                sb.append("/images/");
                break;
            case PDF:
                sb.append("/pdfs/");
                break;
            case VIDEO:
                sb.append("/videos/");
                break;
            default:
                // TODO: better handling
                return "/404";
        }

        sb.append(fileName);
        sb.append("_");
        sb.append(cacheKey);

        // Handling webp optimized resize
        switch (resourceType)
        {
            case IMAGE:
                if (null != width)
                {
                    sb.append("-").append(width);
                }
                sb.append(".");
                sb.append("webp");
                break;

            default:
                sb.append(".");
                sb.append(extension);
        }

        return sb.toString();
    }

    /**
     * Extracts a file name stem from the resource URL.
     *
     * @return file name stem
     */
    public String nameFromUrl()
    {
        final int from = getUrl().lastIndexOf('/');
        final int to = getUrl().indexOf('?', from);

        if (from != -1)
        {
            final int end = to != -1 && from < to ? to : getUrl().length();
            final String ret = getUrl().substring(from + 1, end);
            if (StringUtils.isNotBlank(ret))
            {
                return ret;
            }
        }

        logger.warn("Cannot extract nice name from url {}", getUrl());
        return cacheKey;
    }

    /**
     * Computes the 3-level folder hierarchy prefix from a hash string.
     *
     * @param hash the cache key or hash string
     * @return folder prefix path, or null if hash is null
     */
    public static String folderHashPrefix(final String hash)
    {
        if (hash == null)
        {
            return null;
        }

        int length = hash.lastIndexOf(".");
        if (-1 == length)
        {
            length = hash.length();
        }

        // Guard against short strings to avoid IndexOutOfBoundsException
        if (length < 3)
        {
            return "UNKNOWN" + File.separator + "UNKNOWN" + File.separator + "UNKNOWN" + File.separator;
        }

        return hash.substring(length - 3, length - 2).toUpperCase() + File.separator
                + hash.substring(length - 2, length - 1).toUpperCase() + File.separator
                + hash.substring(length - 1, length).toUpperCase() + File.separator;
    }

    /**
     * Computes the 3-level folder hierarchy prefix based on the current cache key.
     *
     * @return folder prefix path
     */
    public String folderHashPrefix()
    {
        return folderHashPrefix(cacheKey);
    }

    /**
     * Returns a human readable size representation of the resource file size.
     *
     * @return display size string
     */
    public String humanReadableSize()
    {
        return FileUtils.byteCountToDisplaySize(fileSize);
    }

    public String getCacheKey()
    {
        return cacheKey;
    }

    public void setCacheKey(final String cacheKey)
    {
        this.cacheKey = cacheKey;
    }

    public static Logger getLogger()
    {
        return logger;
    }

    public Set<String> getTags()
    {
        return tags;
    }

    public void setTags(final Set<String> tags)
    {
        this.tags = tags;
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public Long getTimeStamp()
    {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp)
    {
        this.timeStamp = timeStamp;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public boolean isEvicted()
    {
        return evicted;
    }

    public void setEvicted(boolean evicted)
    {
        this.evicted = evicted;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getExtension()
    {
        return extension;
    }

    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5(String md5)
    {
        this.md5 = md5;
    }

    public ResourceType getResourceType()
    {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType)
    {
        this.resourceType = resourceType;
    }

    public ImageInfo getImageInfo()
    {
        return imageInfo;
    }

    public void setImageInfo(ImageInfo imageInfo)
    {
        this.imageInfo = imageInfo;
    }

    public ResourceStatus getStatus()
    {
        return status;
    }

    public void setStatus(ResourceStatus status)
    {
        this.status = status;
    }

    public boolean isProcessed()
    {
        return processed;
    }

    public void setProcessed(boolean processed)
    {
        this.processed = processed;
    }

    public Integer getGroup()
    {
        return group;
    }

    public void setGroup(Integer group)
    {
        this.group = group;
    }

    public String getDatasourceName()
    {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName)
    {
        this.datasourceName = datasourceName;
    }

    public Set<ResourceTag> getHardTags()
    {
        return hardTags;
    }

    public void setHardTags(Set<ResourceTag> hardTags)
    {
        this.hardTags = hardTags;
    }

    public PdfInfo getPdfInfo()
    {
        return pdfInfo;
    }

    public void setPdfInfo(PdfInfo pdfInfo)
    {
        this.pdfInfo = pdfInfo;
    }
}
