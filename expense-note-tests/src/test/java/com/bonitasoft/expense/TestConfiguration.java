package com.bonitasoft.expense;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.test.ClientEventUtil;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.After;
import org.junit.Before;

import com.bonitasoft.engine.APITestSPUtil;
import com.bonitasoft.engine.api.ProcessAPI;

public class TestConfiguration {

	// Connecting API client to Studio runtime
	static {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("server.url", System.getProperty("server.url", "http://localhost:8080"));
		parameters.put("application.name", "bonita");
		APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, parameters);
	}

	protected APITestSPUtil apiTestSPUtil;


	@Before
	public void setUp() throws Exception {
		apiTestSPUtil = new APITestSPUtil();
		apiTestSPUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
		ClientEventUtil.deployCommand(apiTestSPUtil.getSession());
	}

	@After
	public void cleanUp() throws Exception {
		apiTestSPUtil.deleteProcessInstanceAndArchived(getAllProcessDefinitions());
		ClientEventUtil.undeployCommand(apiTestSPUtil.getSession());
		apiTestSPUtil.logoutOnTenant();
	}

	protected long processId(String processName) throws SearchException, ProcessDefinitionNotFoundException {
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		List<ProcessDeploymentInfo> result = processAPI.searchProcessDeploymentInfos(
				new SearchOptionsBuilder(0, 99).filter(ProcessDeploymentInfoSearchDescriptor.NAME, processName)
						.sort(ProcessDeploymentInfoSearchDescriptor.VERSION, Order.ASC).done())
				.getResult();

		if (result.isEmpty()) {
			throw new ProcessDefinitionNotFoundException(
					String.format("No process definition found with name '%s'", processName));
		}
		return result.get(0).getProcessId();
	}

	protected List<ProcessDefinition> getAllProcessDefinitions()
			throws SearchException, ProcessDefinitionNotFoundException {
		ProcessAPI processAPI = apiTestSPUtil.getProcessAPI();
		return processAPI.searchProcessDeploymentInfos(new SearchOptionsBuilder(0, 99).done()).getResult().stream()
				.map(ProcessDeploymentInfo::getId).map(id -> {
					try {
						return processAPI.getProcessDefinition(id);
					} catch (ProcessDefinitionNotFoundException e) {
						e.printStackTrace();
					}
					return null;
				}).filter(Objects::nonNull).collect(Collectors.toList());

	}

	protected User user(String userName) throws UserNotFoundException {
		return apiTestSPUtil.getIdentityAPI().getUserByUserName(userName);
	}
}
