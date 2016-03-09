package org.bluedolmen.alfresco.marketplace.modulebuilder;


import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.modulebuilder.AttachedResourceUtils.AttachedResource;
import org.bluedolmen.alfresco.marketplace.utils.HttpSessionUtils;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Format;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WebScriptSession;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;

public class AttachedResourcePost extends AbstractWebScript {
	
	private static final Log logger = LogFactory.getLog(AttachedResourcePost.class);

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		final WebScriptServletRequest webScriptServletRequest = HttpSessionUtils.getWebScriptServletRequest(req);
		
		try  {
			
			saveContent(webScriptServletRequest);
			
		}
		catch (Exception e) {
			
            logger.debug("Exception caught while saving content.", e);
            res.setStatus(Status.STATUS_BAD_REQUEST);
            return;
            
		}
		
		res.setStatus(Status.STATUS_OK);
		
		/*
		 * Output the content as JSON with HTML mimetype.
		 * 
		 * This trick is used by most javascript frameworks to deal with the
		 * historic problem regarding file-upload in an iframe (for multipart
		 * upload).
		 */
		res.setContentType(Format.HTML.mimetype());
		final Writer writer = res.getWriter();
		writer.write("{ \"success\" : true }");
		writer.flush();
		

	}

	protected List<AttachedResource> saveContent(WebScriptServletRequest webScriptServletRequest) throws FileUploadException, IOException {
		
		final Match serviceMatch = webScriptServletRequest.getServiceMatch();
		final Map<String, String> templateVars = serviceMatch.getTemplateVars();		
		final String classification = templateVars.get("classification");
		
		final String replace = webScriptServletRequest.getParameter("replace");
		final FormField formField = webScriptServletRequest.getFileField("content");
		if (null == formField) return null;
		
		final WebScriptSession webScriptSession = webScriptServletRequest.getRuntime().getSession();
		return AttachedResourceUtils.saveSessionContent(webScriptSession, classification, formField.getFilename(), formField.getInputStream(), "true".equalsIgnoreCase(replace));
		
	}
	
//	@SuppressWarnings("unchecked")
//	protected JSONObject buildJSONModel(List<AttachedResource> attachedResources) {
//		
//		final JSONObject model = new JSONObject();
//		
//		if (null != attachedResources) {
//			
//			final JSONArray resources = new JSONArray();
//			model.put("resources", resources);
//			
//			for (AttachedResource attachedResource : attachedResources) {
//				
//				final JSONObject resource = new JSONObject();
//				resource.put("filename", attachedResource.fileName);
//				resources.add(resource);
//				
//				model.put("classification", attachedResource.classification); // last wins			
//				
//			}
//			
//		}
//		
//		return model;
//		
//	}
	

}
