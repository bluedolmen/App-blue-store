package org.bluedolmen.alfresco.marketplace.client;

import java.util.Map;

import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public abstract class ModuleWebscript extends DeclarativeWebScript {

	protected static final String MODULE_ID_PARAM = "moduleId";
	
	protected ModuleService moduleService;

	protected String getModuleIdParam(WebScriptRequest req, Status status) {
		
		final Match match = req.getServiceMatch();
		final Map<String, String> templateVariables = match.getTemplateVars();
		
		if (!templateVariables.containsKey(MODULE_ID_PARAM)) {
			throw new WebScriptException(Status.STATUS_PRECONDITION_FAILED, "Missing mandatory " + MODULE_ID_PARAM + " parameter.");
		}
		
		return templateVariables.get(MODULE_ID_PARAM);
		
	}
	
	
	public void setModuleService(ModuleService moduleService) {
		this.moduleService = moduleService;
	}

}
