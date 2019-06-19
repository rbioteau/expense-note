package com.bonitasoft.expense;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class Step {
	
	private Date executionDate;
	private Map<String, Serializable> inputs;
	private String name;
	
	public Step(String name, Date executionDate, Map<String, Serializable> inputs) {
		this.name = name;
		this.executionDate = executionDate;
		this.inputs = inputs;
	}
	
	public String getName() {
		return name;
	}
	
	public Map<String, Serializable> getInputs() {
		return inputs;
	}
	
	public Date getExecutionDate() {
		return executionDate;
	}
	
}
