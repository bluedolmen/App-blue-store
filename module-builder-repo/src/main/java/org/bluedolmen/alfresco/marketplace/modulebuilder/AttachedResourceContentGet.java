package org.bluedolmen.alfresco.marketplace.modulebuilder;

import java.io.IOException;
import java.util.Map;

import org.alfresco.repo.web.scripts.content.StreamContent;
import org.apache.commons.lang.StringUtils;
import org.bluedolmen.alfresco.marketplace.modulebuilder.AttachedResourceUtils.AttachedResource;
import org.bluedolmen.alfresco.marketplace.utils.HttpSessionUtils;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WebScriptSession;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class AttachedResourceContentGet extends StreamContent {

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		final WebScriptServletRequest webScriptServletRequest = HttpSessionUtils.getWebScriptServletRequest(req);
		final WebScriptSession webScriptSession = webScriptServletRequest.getRuntime().getSession();
		
		AttachedResource resource = null;
		
		final Match serviceMatch = req.getServiceMatch();
		final Map<String, String> templateVars = serviceMatch.getTemplateVars();
		final String classification = templateVars.get("classification");
		
		final String name = templateVars.get("name");
		if (StringUtils.isBlank(name)) {
			resource = AttachedResourceUtils.getFirstSessionResource(webScriptSession, classification);
		}
		else {
			resource = AttachedResourceUtils.getSessionResourceByName(webScriptSession, classification, name);
		}
		
		streamContent(webScriptServletRequest, res, resource.file);
		
	}
	
	
}
