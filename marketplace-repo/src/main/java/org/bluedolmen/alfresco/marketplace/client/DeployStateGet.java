package org.bluedolmen.alfresco.marketplace.client;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.bluedolmen.alfresco.marketplace.client.ModuleService.DeploymentState;
import org.bluedolmen.alfresco.marketplace.client.ModuleService.Worker;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public final class DeployStateGet extends ModuleWebscript {

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final Map<String, Object> model = new HashMap<String, Object>();		
		final String moduleId = getModuleIdParam(req, status);
		
		final Worker worker = moduleService.getWorker(moduleId);
		if (null == worker) {
			model.put("state", DeploymentState.UNAVAILABLE.toString());
		}
		else {
			model.put("state", worker.getDeploymentState().toString());
			model.put("stateMessage", worker.getMessage());
			
			final Throwable failureReason = worker.getFailureReason();
			if (null != failureReason) {
				
				model.put("failureReason", failureReason.getLocalizedMessage());
				
				if ("true".equals(req.getParameter("includeStackTrace"))) {
					
					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					failureReason.printStackTrace(new PrintStream(baos));
					model.put("stacktrace", baos.toString());
					
				}
			}
		}
		
		status.setCode(Status.STATUS_OK);
		
		return model;
		
	}
	
	
}
