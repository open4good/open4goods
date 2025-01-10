package org.open4goods.commons.config.yml.ui;

public class SubsetCriteria {

	private String field;
	// Optional owning group name
	private String group;
	private SubsetCriteriaOperator operator;
	private String value;
	
	
	public SubsetCriteria() {
		super();
	}
	public SubsetCriteria(String field, SubsetCriteriaOperator operator, String value) {
		super();
		this.field = field;
		this.operator = operator;
		this.value = value;
	}
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public SubsetCriteriaOperator getOperator() {
		return operator;
	}
	public void setOperator(SubsetCriteriaOperator operator) {
		this.operator = operator;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	
	
}
