package com.bonitasoft.expense;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class CaseHistoryBuilder {

	private CaseHistory caseHistory;

	private CaseHistoryBuilder(long caseId) {
		caseHistory = new CaseHistory(caseId);
	}

	public static CaseHistoryBuilder newCaseHistory(long caseId) {
		return new CaseHistoryBuilder(caseId); 
	}
	
	public CaseHistoryBuilder startedAt(Date startDate) {
		caseHistory.setStartDate(startDate);
		return this;
	}
	
	public CaseHistoryBuilder withInputs(Map<String, Serializable> inputs) {
		caseHistory.setCaseInput(inputs);
		return this;
	}
	
	public CaseHistoryBuilder addStep(String name, Date executionDate, Map<String, Serializable> inputs) {
		Step step = new Step(name,executionDate,inputs);
		caseHistory.addStep(step);
		return this;
	}
	
	public CaseHistory create() {
		return caseHistory;
	}
	
}
