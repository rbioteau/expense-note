package com.bonitasost.rest.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Timeline {

	private List<Step> steps = new ArrayList<>();

	public void addStep(Step step) {
		steps.add(step);
		steps.sort { s1 , s2 -> s1.getExecutionDate().compareTo(s2.getExecutionDate()) }
	}

	public List<Step> getSteps() {
		return steps;
	}
	
}
