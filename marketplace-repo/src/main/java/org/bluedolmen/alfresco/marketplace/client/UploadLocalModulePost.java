package org.bluedolmen.alfresco.marketplace.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;

public final class UploadLocalModulePost extends DeclarativeWebScript {
	
	private ModuleStorageManager moduleStorageManager;
	
	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final Map<String, Object> model = new HashMap<String, Object>();
		
		// Try first a direct installation from a potential provided module-content
		try {
			final File moduleContent = retrieveModuleContent(req);
			if (null != moduleContent) {
				
				final NodeRef moduleNode = moduleStorageManager.storeModule(moduleContent);
				model.put("moduleNode", moduleNode.toString());
				
				return model;
			}
		}
		catch (IOException e) {
			throw new WebScriptException("Cannot install the module with the given content", e);
		}
		
		return model;
		
	}
	
	private File retrieveModuleContent(WebScriptRequest req) throws IOException {
		
		final Object formReq = req.parseContent();
		if (formReq == null || !(formReq instanceof FormData)) {
			return null;
		}
		
		final FormData formData = (FormData) formReq;
		final FormField[] formFields = formData.getFields();
		
		for (int i = 0, len = formFields.length; i < len; i++) {
			final FormField field = formFields[i];
			if (field == null) continue; 
			
			final String normalizedFieldName = field.getName().toLowerCase();
			
			if ("module".equals(normalizedFieldName) && field.getIsFile()) {
				final File tempFile = TempFileProvider.createTempFile("deploy-module", ".jar");
				IOUtils.copy(field.getInputStream(), new FileOutputStream(tempFile));
				return tempFile;
			}
			
		}
		
		return null;
		
	}


	public void setModuleStorageManager(ModuleStorageManager moduleStorageManager) {
		this.moduleStorageManager = moduleStorageManager;
	}
	
}
