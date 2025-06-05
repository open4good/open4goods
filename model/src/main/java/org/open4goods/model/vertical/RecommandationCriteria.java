package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;

public record RecommandationCriteria(
                List<RecommandationChoice> choices,
                String panelType) {

        public RecommandationCriteria() {
                this(new ArrayList<>(), "success");
        }



	public List<RecommandationChoice> getChoices() {
		return choices;
	}

        public String getPanelType() {
                return panelType;
        }




}
