package com.bonitasoft.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.bpm.contract.ContractViolationException;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance;
import org.bonitasoft.engine.bpm.flownode.FlowNodeExecutionException;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.UserTaskNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessActivationException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessExecutionException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bonitasoft.engine.api.ProcessAPI;

public class ExpenseNoteIT extends TestConfiguration {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void should_not_accept_a_negative_amount() throws Exception {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", -20);
		expenseInput.put("reason", "negative amount");
		inputs.put("expenseInput", (Serializable) expenseInput);

		// Expected
		expectedException.expect(ContractViolationException.class);
		expectedException.expectMessage("Amount should be greather than 0");

		// When
		processAPI.startProcessWithInputs(user("walter.bates").getId(), processId("Expense report"), inputs);
	}

	@Test
	public void should_not_accept_a_empty_reason() throws Exception {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", 20);
		expenseInput.put("reason", "");
		inputs.put("expenseInput", (Serializable) expenseInput);

		// Expected
		expectedException.expect(ContractViolationException.class);
		expectedException.expectMessage("A reason must be given");

		// When
		processAPI.startProcessWithInputs(user("walter.bates").getId(), processId("Expense report"), inputs);
	}

	@Test
	public void should_wait_for_manager_validation_when_creating_an_expense_note() throws Exception {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", 20);
		expenseInput.put("reason", "Food");
		inputs.put("expenseInput", (Serializable) expenseInput);

		// When
		User user = user("walter.bates");
		ProcessInstance processInstance = processAPI.startProcessWithInputs(user.getId(), processId("Expense report"),
				inputs);

		// Then
		HumanTaskInstance taskInstance = (HumanTaskInstance) apiTestSPUtil.waitForUserTaskAndGetIt(processInstance,
				"Validate expense");
		assertThat(taskInstance).isNotNull();
		assertThat(taskInstance.getAssigneeId()).isEqualTo(user.getManagerUserId());
	}

	@Test
	public void should_wait_for_financial_agent_validation_when_expense_note_is_above_authorized_manager_threshold()
			throws Exception {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", 888);
		expenseInput.put("reason", "Plane");
		inputs.put("expenseInput", (Serializable) expenseInput);

		// When
		User user = user("walter.bates");
		ProcessInstance processInstance = processAPI.startProcessWithInputs(user.getId(), processId("Expense report"),
				inputs);
		HumanTaskInstance taskInstance = (HumanTaskInstance) apiTestSPUtil.waitForUserTaskAndGetIt(processInstance,
				"Validate expense");

		expenseInput = new HashMap<>();
		expenseInput.put("validated", true);
		inputs.put("expenseInput", (Serializable) expenseInput);
		processAPI.executeUserTask(user.getManagerUserId(), taskInstance.getId(), inputs);

		// Then
		taskInstance = apiTestSPUtil.waitForUserTaskAndGetIt(processInstance, "Validate expense (FA)");
		assertThat(taskInstance).isNotNull();

		User financialAgent = user("virginie.jomphe");
		assertThat(processAPI.getPendingHumanTaskInstances(financialAgent.getId(), 0, 1,
				ActivityInstanceCriterion.DEFAULT)).extracting("name").contains(taskInstance.getName());
	}

	@Test
	public void should_wait_for_financial_director_validation_when_expense_note_is_above_authorized_financial_agent_threshold()
			throws Exception {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", 2500);
		expenseInput.put("reason", "Full fun");
		inputs.put("expenseInput", (Serializable) expenseInput);

		// When
		User user = user("walter.bates");
		ProcessInstance processInstance = processAPI.startProcessWithInputs(user.getId(), processId("Expense report"),
				inputs);
		HumanTaskInstance taskInstance = (HumanTaskInstance) apiTestSPUtil.waitForUserTaskAndGetIt(processInstance,
				"Validate expense");

		expenseInput = new HashMap<>();
		expenseInput.put("validated", true);
		inputs.put("expenseInput", (Serializable) expenseInput);
		processAPI.executeUserTask(user.getManagerUserId(), taskInstance.getId(), inputs);

		// Then
		taskInstance = apiTestSPUtil.waitForUserTaskAndGetIt(processInstance, "Validate expense (FD)");
		assertThat(taskInstance).isNotNull();

		User financialDirector = user("zachary.williamson");
		assertThat(processAPI.getPendingHumanTaskInstances(financialDirector.getId(), 0, 1,
				ActivityInstanceCriterion.DEFAULT)).extracting("name").contains(taskInstance.getName());
	}

	@Test
	public void should_execute_payment_activity_when_expense_is_validated() throws Exception {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", 20);
		expenseInput.put("reason", "Food");
		inputs.put("expenseInput", (Serializable) expenseInput);

		// When
		User user = user("walter.bates");
		ProcessInstance processInstance = processAPI.startProcessWithInputs(user.getId(), processId("Expense report"),
				inputs);
		HumanTaskInstance taskInstance = (HumanTaskInstance) apiTestSPUtil.waitForUserTaskAndGetIt(processInstance,
				"Validate expense");
		expenseInput = new HashMap<>();
		expenseInput.put("validated", true);
		inputs.put("expenseInput", (Serializable) expenseInput);
		processAPI.executeUserTask(user.getManagerUserId(), taskInstance.getId(), inputs);

		// Then
		ArchivedActivityInstance paymentActivity = apiTestSPUtil
				.waitForFinalArchivedActivity("Payment", processInstance).getResult();
		assertThat(paymentActivity).isNotNull();
	}

	@Test
	public void should_retrieve_specific_case_data() throws Exception {
		// long c1 = runBasicCase(10, "Transport");
		long c2 = runBasicCase(30, "Meal");
		// long c3 = runBasicCase(80, "Conf");

		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		CaseHistory c2CaseHistory = new CaseHistoryFactory(processAPI).createCaseHistory(c2);
		assertThat((Map<String, Serializable>) c2CaseHistory.getCaseInput().get("expenseInput"))
				.contains(entry("amount", 30), entry("reason", "Meal"));
		System.out.println(c2CaseHistory.prettyPrint());
	}

	private long runBasicCase(int amount, String reason)
			throws UserNotFoundException, ProcessDefinitionNotFoundException, ProcessActivationException,
			ProcessExecutionException, ContractViolationException, SearchException, Exception,
			UserTaskNotFoundException, FlowNodeExecutionException {
		// Given
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		Map<String, Serializable> inputs = new HashMap<>();
		Map<String, Serializable> expenseInput = new HashMap<>();
		expenseInput.put("amount", amount);
		expenseInput.put("reason", reason);
		inputs.put("expenseInput", (Serializable) expenseInput);

		// When
		User user = user("walter.bates");
		ProcessInstance processInstance = processAPI.startProcessWithInputs(user.getId(), processId("Expense report"),
				inputs);
		HumanTaskInstance taskInstance = (HumanTaskInstance) apiTestSPUtil.waitForUserTaskAndGetIt(processInstance,
				"Validate expense");
		expenseInput = new HashMap<>();
		expenseInput.put("validated", true);
		inputs.put("expenseInput", (Serializable) expenseInput);
		processAPI.executeUserTask(user.getManagerUserId(), taskInstance.getId(), inputs);

		// Then
		ArchivedActivityInstance paymentActivity = apiTestSPUtil
				.waitForFinalArchivedActivity("Payment", processInstance).getResult();
		assertThat(paymentActivity).isNotNull();
		return processInstance.getId();
	}

}
