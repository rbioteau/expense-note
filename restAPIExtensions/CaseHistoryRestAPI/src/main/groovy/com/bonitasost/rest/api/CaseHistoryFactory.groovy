package com.bonitasost.rest.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.contract.ContractDefinition;
import org.bonitasoft.engine.bpm.flownode.ArchivedHumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.flownode.UserTaskDefinition;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.exception.ContractDataNotFoundException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;

import com.bonitasoft.engine.api.ProcessAPI;

public class CaseHistoryFactory {

	private ProcessAPI processAPI;

	public CaseHistoryFactory(ProcessAPI processAPI) {
		this.processAPI = processAPI;
	}

	public CaseHistory createCaseHistory(long caseId)
			throws ProcessDefinitionNotFoundException, ArchivedProcessInstanceNotFoundException, SearchException {
		CaseHistoryBuilder caseHistoryBuilder = CaseHistoryBuilder.newCaseHistory(caseId);
		ArchivedProcessInstance archivedProcessInstance = findArchivedProcessInstance(processAPI, caseId);
		ContractDefinition processContract = processAPI
				.getProcessContract(archivedProcessInstance.getProcessDefinitionId());
		Map<String, Serializable> caseInput = getCaseInput(caseId, processContract);
		caseHistoryBuilder.withInputs(caseInput).startedAt(archivedProcessInstance.getStartDate());

		processAPI
				.searchArchivedHumanTasks(new SearchOptionsBuilder(0, Integer.MAX_VALUE)
						.filter(ArchivedHumanTaskInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, caseId)
						.sort(ArchivedHumanTaskInstanceSearchDescriptor.REACHED_STATE_DATE, Order.ASC).done())
				.getResult().stream()
				.each{ archivedTask -> caseHistoryBuilder.addStep(archivedTask.getName(),
						archivedTask.getReachedStateDate(),
						getTaskInput(getContractDefinition(archivedTask.getName(),archivedTask.getProcessDefinitionId()),archivedTask.getSourceObjectId()))}

		return caseHistoryBuilder.create();
	}

	private ContractDefinition getContractDefinition(String name,long processDefinitionId) {
		try {
			DesignProcessDefinition pdef = processAPI.getDesignProcessDefinition(processDefinitionId);
			UserTaskDefinition task = (UserTaskDefinition) pdef.getFlowElementContainer().getActivity(name);
			return task.getContract();
		} catch (ProcessDefinitionNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Map<String, Serializable> getTaskInput(ContractDefinition contract, long taskId) {
		Map<String, Serializable> taskeInput = new HashMap<>();
		contract.getInputs().each { inputDef -> 
				Serializable value = processAPI.getUserTaskContractVariableValue(taskId, inputDef.getName());
				taskeInput.put(inputDef.getName(), value);
		}
		return taskeInput;
	}

	private Map<String, Serializable> getCaseInput(long caseId, ContractDefinition processContract) {
		Map<String, Serializable> caseInput = new HashMap<>();
		processContract.getInputs().each {inputDef -> 
				Serializable value = processAPI.getProcessInputValueAfterInitialization(caseId, inputDef.getName());
				caseInput.put(inputDef.getName(), value);
		}
		return caseInput;
	}

	private ArchivedProcessInstance findArchivedProcessInstance(ProcessAPI processAPI, long caseId)
			throws SearchException, ArchivedProcessInstanceNotFoundException {
		List<ArchivedProcessInstance> result = processAPI.searchArchivedProcessInstances(new SearchOptionsBuilder(0, 99)
				.filter(ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID, caseId).done()).getResult();
		if (result.isEmpty()) {
			throw new ArchivedProcessInstanceNotFoundException(caseId);
		}
		return result.get(0);
	}

}
