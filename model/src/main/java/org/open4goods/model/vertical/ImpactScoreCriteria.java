package org.open4goods.model.vertical;

import org.open4goods.model.Localisable;

import com.fasterxml.jackson.annotation.JsonMerge;

public record ImpactScoreCriteria(
                String key,
                @JsonMerge Localisable<String, String> description,
                @JsonMerge Localisable<String, String> title) {

        public ImpactScoreCriteria() {
                this(null, new Localisable<>(), new Localisable<>());
        }

	public String getKey() {
		return key;
	}


	public Localisable<String, String> getDescription() {
		return description;
	}


	public Localisable<String, String> getTitle() {
		return title;
	}

	
	
	
}
