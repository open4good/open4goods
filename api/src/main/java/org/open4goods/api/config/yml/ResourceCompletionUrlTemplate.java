package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.data.ResourceTag;

public class ResourceCompletionUrlTemplate {
    private String url;
    private String datasourceName;
    private String language;
    private List<ResourceTag> tags = new ArrayList<>();

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

	public List<ResourceTag> getTags() {
		return tags;
	}

	public void setTags(List<ResourceTag> tags) {
		this.tags = tags;
	}
}
