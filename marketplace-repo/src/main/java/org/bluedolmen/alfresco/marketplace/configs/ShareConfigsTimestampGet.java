package org.bluedolmen.alfresco.marketplace.configs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class ShareConfigsTimestampGet extends DeclarativeWebScript {
	
	private static final Log logger = LogFactory.getLog(ShareConfigsTimestampGet.class);
	
	private ConfigRepositoryHelper configRepositoryHelper;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final Map<String, Object> model = new HashMap<String, Object>(1);
		
		final Date lastModifiedDate = configRepositoryHelper.getConfigHomeLastModifiedDate();
		if (null != lastModifiedDate) { 
			cache.setLastModified(lastModifiedDate);
			model.put("lastModified", lastModifiedDate);
		}
		
		return model;
		
	}
			
	public void setConfigRepositoryHelper(ConfigRepositoryHelper configRepositoryHelper) {
		this.configRepositoryHelper = configRepositoryHelper;
	}
	
}
