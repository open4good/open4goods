package org.open4goods.model.vertical;

import java.util.HashMap;
import java.util.Map;

public record ImpactScoreTexts(
                Map<String, String> criteriasAnalysis,
                String purpose,
                String criticalReview,
                String availlableDatas) {

        public ImpactScoreTexts() {
                this(new HashMap<>(), null, null, null);
        }
        public Map<String, String> getCriteriasAnalysis() {
                return criteriasAnalysis;
        }
        public String getPurpose() {
                return purpose;
        }
        public String getCriticalReview() {
                return criticalReview;
        }
        public String getAvaillableDatas() {
                return availlableDatas;
        }
	
	
	
}
