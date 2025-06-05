package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public record RecommandationsConfig(@NotNull List<RecommandationCriteria> recommandations) {

        public RecommandationsConfig() {
                this(new ArrayList<>());
        }

        public List<RecommandationCriteria> getRecommandations() {
                return recommandations;
        }




}
