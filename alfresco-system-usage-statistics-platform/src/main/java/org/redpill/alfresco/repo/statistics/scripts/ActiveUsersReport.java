package org.redpill.alfresco.repo.statistics.scripts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import org.redpill.alfresco.repo.statistics.service.ActiveUsers;
import org.redpill.alfresco.repo.statistics.service.UserLoginDetails;

public class ActiveUsersReport extends DeclarativeWebScript {

	private ActiveUsers activeUsers;

	public void setActiveUsers(ActiveUsers activeUsers) {
		this.activeUsers = activeUsers;
	}

	@Override
	protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache) {
		Map<String, Object> model = new HashMap<String, Object>();

		Map<String, List<UserLoginDetails>> activeUsersByZone;
		try {
			activeUsersByZone = activeUsers.getActiveUsersByZone();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		int size = activeUsersByZone.get(ActiveUsers.INTERNAL_USERS).size();
		Map<String, Object> internalModel = new HashMap<String, Object>();

		internalModel.put("users", activeUsersByZone.get(ActiveUsers.INTERNAL_USERS));
		internalModel.put("recordsReturned", size);
		internalModel.put("totalRecords", size);
		internalModel.put("startIndex", 0);
		internalModel.put("pageSize", size);

		size = activeUsersByZone.get(ActiveUsers.EXTERNAL_USERS).size();
		Map<String, Object> externalModel = new HashMap<String, Object>();

		externalModel.put("users", activeUsersByZone.get(ActiveUsers.EXTERNAL_USERS));
		externalModel.put("recordsReturned", size);
		externalModel.put("totalRecords", size);
		externalModel.put("startIndex", 0);
		externalModel.put("pageSize", size);

		model.put(ActiveUsers.INTERNAL_USERS, internalModel);
		model.put(ActiveUsers.EXTERNAL_USERS, externalModel);

		return model;
	}

}
