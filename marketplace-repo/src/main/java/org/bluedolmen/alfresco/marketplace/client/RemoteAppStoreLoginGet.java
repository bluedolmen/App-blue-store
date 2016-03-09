package org.bluedolmen.alfresco.marketplace.client;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRuntime;

/**
 * Check whether the user is already authenticated through the current session
 * @author bpajot
 *
 */
public class RemoteAppStoreLoginGet extends DeclarativeWebScript {
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
    	final HttpServletRequest httpServletRequest = WebScriptServletRuntime.getHttpServletRequest(req);
    	final HttpSession session = httpServletRequest.getSession(true);
        final Map<String, Object> result = new HashMap<String, Object>(1);
    	
    	if (null != session) {
    		
    		final String loginTicket = appStoreRemoteManager.getTicket(session);
    		if (StringUtils.isNotEmpty(loginTicket)) {
    			result.put("ticket", loginTicket);
    			
                final String username = appStoreRemoteManager.getSessionUser(session);
                result.put("username", username);
    		}
            
    	}
        
        return result;
		
	}
	
	private AppStoreRemoteManager appStoreRemoteManager;
	
	public void setAppStoreRemoteManager(AppStoreRemoteManager appStoreRemoteManager) {
		this.appStoreRemoteManager = appStoreRemoteManager;
	}
	
}
