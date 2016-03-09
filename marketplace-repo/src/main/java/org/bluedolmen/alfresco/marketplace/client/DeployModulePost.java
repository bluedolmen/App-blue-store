package org.bluedolmen.alfresco.marketplace.client;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.bluedolmen.alfresco.marketplace.client.ModuleService.DeployConfig;
import org.bluedolmen.alfresco.marketplace.client.ModuleService.ServerUpdateMode;
import org.bluedolmen.alfresco.webscripts.WebScriptHelper;
import org.bluedolmen.marketplace.commons.module.Version;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRuntime;

public final class DeployModulePost extends ModuleWebscript {
	
	private static final String VERSION_PARAM = "version";
	private static final String SERVER_UDPATE_MODE_PARAM = "serverUpdateMode";
	private static final String FORCE_LOCAL_UPDATE_PARAM = "forceLocalUpdate";
	private static final String OVERRIDE_EXISTING_PARAM = "overrideExisting";
	
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final Map<String, Object> model = new HashMap<String, Object>();
		
		final String moduleId = getModuleIdParam(req, status);
		if (null == moduleId) return model; // error status already set
		
		final Version version = getVersion(req);
		final JSONObject parameters = WebScriptHelper.getParametersAsJSON(req);
		
		final String serverUpdateMode_ = WebScriptHelper.getCheckedParameter(parameters, SERVER_UDPATE_MODE_PARAM, String.class);
		final ServerUpdateMode serverUpdateMode = StringUtils.isBlank(serverUpdateMode_) ? ServerUpdateMode.ENABLED : ServerUpdateMode.valueOf(serverUpdateMode_);
		final boolean forceLocalUpdate = StringUtils.equalsIgnoreCase("true", WebScriptHelper.getCheckedParameter(parameters, FORCE_LOCAL_UPDATE_PARAM, String.class));
		final boolean overrideExisting = StringUtils.equalsIgnoreCase("true", WebScriptHelper.getCheckedParameter(parameters, OVERRIDE_EXISTING_PARAM, String.class));
		
		final DeployConfig deployConfig = new DeployConfig(serverUpdateMode, forceLocalUpdate, overrideExisting);

		final HttpServletRequest httpServletRequest = WebScriptServletRuntime.getHttpServletRequest(req);
		final HttpSession session = httpServletRequest.getSession();
		deployConfig.setSession(session);
		
		moduleService.deployModule(moduleId, version, deployConfig);
		
		status.setCode(Status.STATUS_OK);
		
		return model;
		
	}
	
	private Version getVersion(WebScriptRequest req) {
	
		final Match match = req.getServiceMatch();
		final String version = match.getTemplateVars().get(VERSION_PARAM);
		
		return Version.fromString(version);
		
	}
	
}
