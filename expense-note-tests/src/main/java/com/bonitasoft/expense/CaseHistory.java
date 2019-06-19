package com.bonitasoft.expense;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CaseHistory {
	
	private Long caseId;
	private Map<String, Serializable> caseInput;
	private Date startDate;
	private Timeline timeline = new Timeline();
	private ObjectMapper mapper = new ObjectMapper();
	
	public CaseHistory(long caseId) {
		this.caseId = caseId;
	}
	
	public Long getCaseId() {
		return caseId;
	}
	
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	
	public void setCaseInput(Map<String, Serializable> caseInput) {
		this.caseInput = caseInput;
	}
	
	public Map<String, Serializable> getCaseInput() {
		return caseInput;
	}

	public void addStep(Step step) {
		timeline.addStep(step);
	}

	public Timeline getTimeline() {
		return timeline;
	}
	
	public String prettyPrint() throws JsonProcessingException {
		return mapper.writeValueAsString(this);
	}
	
}
