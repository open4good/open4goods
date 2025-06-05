package org.open4goods.model.vertical;

public record RecommandationChoice(
                String name,
                String queryFragment,
                Boolean defaultChoice,
                Integer divid) {

        public RecommandationChoice() {
                this(null, null, Boolean.FALSE, 2);
        }

        public String getName() {
                return name;
        }

        public String getQueryFragment() {
                return queryFragment;
        }

        public Boolean getDefaultChoice() {
                return defaultChoice;
        }

        public Integer getDivid() {
                return divid;
        }





}
