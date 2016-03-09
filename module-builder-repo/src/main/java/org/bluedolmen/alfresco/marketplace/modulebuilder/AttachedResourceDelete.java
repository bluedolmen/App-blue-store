package org.bluedolmen.alfresco.marketplace.modulebuilder;


import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.utils.HttpSessionUtils;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WebScriptSession;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class AttachedResourceDelete extends AbstractWebScript {
	
	private static final Log logger = LogFactory.getLog(AttachedResourceDelete.class);

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		final WebScriptServletRequest webScriptServletRequest = HttpSessionUtils.getWebScriptServletRequest(req);
		final WebScriptSession webScriptSession = webScriptServletRequest.getRuntime().getSession();
		
		final Match serviceMatch = req.getServiceMatch();
		final Map<String, String> templateVars = serviceMatch.getTemplateVars();
		
		final String name = templateVars.get("name");		
		if (StringUtils.isBlank(name)) {
			throw new WebScriptException(Status.STATUS_PRECONDITION_FAILED, "You have to provide the name of the resource to delete");
		}
		
		final String classification = templateVars.get("classification");
		AttachedResourceUtils.deleteSessionContent(webScriptSession, classification, name);
		
		res.setStatus(Status.STATUS_NO_CONTENT);

	}

}
