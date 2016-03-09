package org.bluedolmen.alfresco.marketplace.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public final class UndeployModulePost extends ModuleWebscript {
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final Map<String, Object> model = new HashMap<String, Object>();
		
		final String moduleId = getModuleIdParam(req, status);
		if (null == moduleId) return model; // error status already set
		
		moduleService.undeployModule(moduleId);
		
		status.setCode(Status.STATUS_OK);
		
		return model;
		
	}


}
