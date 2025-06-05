package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;

public record PrefixedAttrText(String prefix, List<String> attrs) {

        public PrefixedAttrText() {
                this(null, new ArrayList<>());
        }

	public String getPrefix() {
		return prefix;
	}


	public List<String> getAttrs() {
		return attrs;
	}

	
	 
      
      
}