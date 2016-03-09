package org.bluedolmen.alfresco.marketplace.modulebuilder;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.modulebuilder.AttachedResourceUtils.AttachedResource;
import org.bluedolmen.alfresco.marketplace.utils.HttpSessionUtils;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptSession;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class AttachedResourcesGet extends DeclarativeWebScript {
	
	private static final Log logger = LogFactory.getLog(AttachedResourcesGet.class);

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final WebScriptServletRequest webScriptServletRequest = HttpSessionUtils.getWebScriptServletRequest(req);
		final WebScriptSession session = webScriptServletRequest.getRuntime().getSession();
		
		final Match serviceMatch = webScriptServletRequest.getServiceMatch();
		final Map<String, String> templateVars = serviceMatch.getTemplateVars();		
		final String classification = templateVars.get("classification");
		
		final List<AttachedResource> attachedResources = AttachedResourceUtils.getSessionResources(session, classification);
		
		final List<Map<String, String>> mResources = new ArrayList<Map<String, String>>(); 
		
		for (AttachedResource resource : attachedResources) {
			
			final Map<String, String> properties = new HashMap<String, String>();
			properties.put("name", resource.fileName);
			if (null != resource.file) {
				properties.put("size", new Long(resource.file.length()).toString());
			}
			
			mResources.add(properties);
			
		}
		
		final Map<String, Object> model = new HashMap<String, Object>();
		model.put("resources", mResources);
		
		return model;
		
	}

}
