package org.open4goods.services.googlesearch.dto;

import java.util.Objects;

/**
 * Data Transfer Object representing an individual search result.
 */
public class GoogleSearchResult {

    private final String title;
    private final String link;

    /**
     * Constructs a new GoogleSearchResult.
     *
     * @param title the title of the search result
     * @param link  the URL of the search result
     */
    public GoogleSearchResult(String title, String link) {
        this.title = title;
        this.link = link;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "GoogleSearchResult{" +
                "title='" + title + '\'' +
                ", link='" + link + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, link);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoogleSearchResult)) return false;
        GoogleSearchResult that = (GoogleSearchResult) o;
        return Objects.equals(title, that.title) &&
               Objects.equals(link, that.link);
    }
}
