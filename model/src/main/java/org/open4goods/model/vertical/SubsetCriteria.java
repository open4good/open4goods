package org.open4goods.model.vertical;

public record SubsetCriteria(String field, SubsetCriteriaOperator operator, String value) {

        public SubsetCriteria() {
                this(null, null, null);
        }

        @Override
        public String toString() {
                return field + " " + operator + " " + value;
        }

        public String getField() {
                return field;
        }

        public SubsetCriteriaOperator getOperator() {
                return operator;
        }

        public String getValue() {
                return value;
        }
	
	
	
}
