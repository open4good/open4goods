package org.open4goods.model.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Validable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
/**
 * Resources represents documents, images, binaries , etc.. associated with a product or a DataFragment.
 * They are also indexed externaly (in a dedicated index) at the UI level, to be able to handle efficient caching, resizeing, mimetype detection and conversions...
 * @author goulven
 *
 */
public class Resource  implements Validable {

	private static final Logger logger = LoggerFactory.getLogger(Resource.class);

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String providerName;

	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long timeStamp;

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String cacheKey;

	/**
	 * The text, ot the title associated with this resource
	 */
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String text;

	/**
	 * From ResourceTagDictionary
	 */
	@Field(index = false, store = false, type = FieldType.Keyword)

	private Set<String> tags = new HashSet<>();


	public Resource() {
		super();
	}

	public Resource(String url) {
		super();

		if (url.startsWith("//")) {
			url = "http:" + url;
		}
		setUrl(url);
	}

	@Override
	public String toString() {
		return  getUrl();
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof Resource) {
			final Resource o = (Resource) obj;
			return getUrl().equals(o.getUrl());
		}

		return false;
	}

	public void addTag(final String datasourceName) {
		tags.add(datasourceName);

	}

	@Override
	public int hashCode() {
		return getUrl().hashCode();
	}


	@Override
	public void validate() throws ValidationException {
		try {
			new URL(getUrl());
		} catch (final MalformedURLException e) {
			throw new ValidationException("invalid URL : " + getUrl());
		}

	}




	public String nameFromUrl() {


		final int from = getUrl().lastIndexOf('/');
		final int to = getUrl().indexOf('?', from);

		if (from != -1 && to != -1 && from < to) {
			String ret = getUrl().substring(from+1, to);
			int to2 = ret.indexOf('?');
			if (-1 != to2) {
				ret = ret.substring(0,to2-1);
			}
			return ret;
		} else {
			logger.warn("Cannot extract nice name from url {}",getUrl());
			return getUrl();
		}

	}
	public static String folderHashPrefix(final String hash) {
		//NOTE(gof) : performances
		int length = hash.lastIndexOf(".");
		if (-1 == length) {
			length=hash.length();
		}



		return hash == null ? null : hash.substring(length-3,length-2).toUpperCase() + "/" + hash.substring(length-2,length-1).toUpperCase() +"/" + hash.substring(length-1,length).toUpperCase()+"/";
	}

	public String folderHashPrefix() {
		return folderHashPrefix(cacheKey);
	}


	//////////////////////////////////
	// Getters / Setters
	/////////////////////////////////



	public String getCacheKey() {
		return cacheKey;
	}

	public void setCacheKey(final String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public static Logger getLogger() {
		return logger;
	}

	public Set<String> getTags() {
		return tags;
	}


	public void setTags(final Set<String> tags) {
		this.tags = tags;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public Long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}




}
