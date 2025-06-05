package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.Localisable;

public record VerticalSubset(
                String id,
                String group,
                List<SubsetCriteria> criterias,
                String image,
                Localisable<String, String> url,
                Localisable<String, String> caption,
                Localisable<String, String> title,
                Localisable<String, String> description) {

        public VerticalSubset() {
                this(null, null, new ArrayList<>(), null,
                                new Localisable<>(), new Localisable<>(), new Localisable<>(), new Localisable<>());
        }

	
	
	@Override
	public int hashCode() {
		return Objects.hash(id, group, criterias, image, url, title, description);
	}

	// equals method
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		VerticalSubset other = (VerticalSubset) obj;
		return Objects.equals(id, other.id) && Objects.equals(group, other.group) && Objects.equals(criterias, other.criterias) && Objects.equals(image, other.image) && Objects.equals(url, other.url) && Objects.equals(title, other.title) && Objects.equals(description, other.description);
	}
	    
	    
	    
        public List<SubsetCriteria> getCriterias() {
                return criterias;
        }

        public String getImage() {
                return image;
        }

        public Localisable<String, String> getTitle() {
                return title;
        }

        public Localisable<String, String> getDescription() {
                return description;
        }

        public String getId() {
                return id;
        }

        public Localisable<String, String> getUrl() {
                return url;
        }

        public String getGroup() {
                return group;
        }

        public Localisable<String, String> getCaption() {
                return caption;
        }
	
	

}
