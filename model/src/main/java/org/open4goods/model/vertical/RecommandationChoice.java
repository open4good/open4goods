package org.open4goods.model.vertical;

public record RecommandationChoice(
                String name,
                String queryFragment,
                Boolean defaultChoice,
                Integer divid) {

        public RecommandationChoice {
                defaultChoice = defaultChoice != null ? defaultChoice : Boolean.FALSE;
                divid = divid != null ? divid : 2;
        }

}
