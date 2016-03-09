package org.bluedolmen.alfresco.marketplace.client;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bluedolmen.alfresco.webscripts.WebScriptHelper;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRuntime;

public class RemoteAppStoreLoginPost extends DeclarativeWebScript {
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final JSONObject parameters = WebScriptHelper.getParametersAsJSON(req);
		
		final String username = WebScriptHelper.getCheckedParameter(parameters, "username", String.class);
        if (username == null || username.length() == 0) {
            throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Username not specified");
        }
        
        final String password = WebScriptHelper.getCheckedParameter(parameters, "password", String.class);
        if (password == null) {
            throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, "Password not specified");
        }
        
    	final HttpServletRequest httpServletRequest = WebScriptServletRuntime.getHttpServletRequest(req);
    	final HttpSession session = httpServletRequest.getSession(true);

        final String loginTicket = login(session, username, password);
        if (null == loginTicket) {
        	throw new WebScriptException("appstore.remote-login.failure");
        }
        
        final Map<String, Object> result = new HashMap<String, Object>(1);
        result.put("ticket", loginTicket);
        
        return result;
		
	}
	
	private String login(HttpSession session, String username, String password) {
		
		try {
			
			return appStoreRemoteManager.login(session, username, password);
			
		} catch (Throwable t) {
			throw new WebScriptException("appstore.remote-login.failure", t, t.getMessage());
		}
		
	}
	
	private AppStoreRemoteManager appStoreRemoteManager;
	
	public void setAppStoreRemoteManager(AppStoreRemoteManager appStoreRemoteManager) {
		this.appStoreRemoteManager = appStoreRemoteManager;
	}
	
}
