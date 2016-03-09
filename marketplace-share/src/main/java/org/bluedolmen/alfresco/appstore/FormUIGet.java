package org.bluedolmen.alfresco.appstore;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractMessageHelper;
import org.springframework.extensions.webscripts.Container;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;


public class FormUIGet extends org.alfresco.web.scripts.forms.FormUIGet {
	
	private static final Log logger = LogFactory.getLog(FormUIGet.class);
	private RepositoryMessagesHelper messagesHelper;
	private TemplateMethodModelEx messageMethodField = null;
	
	@Override
	public void init(Container container, Description description) {
		
		super.init(container, description);
		
	}
	
	@Override
	protected Map<String, Object> createTemplateParameters(WebScriptRequest req, WebScriptResponse res, Map<String, Object> customParams) {
		
		final Map<String, Object> params = super.createTemplateParameters(req, res, customParams);
        params.put("message", getMessageMethod());     // for compatibility with repo templates
        params.put("msg", getMessageMethod());         // short form for presentation webscripts

        return params;
		
	}
	
	private TemplateMethodModelEx getMessageMethod() {
		
		if (null == messageMethodField) {
			messageMethodField = new MessageMethod(this);
		}
		
		return messageMethodField;
		
	}
	
	@Override
	protected String retrieveMessage(String messageKey, Object... args) {
		
		final String message = messagesHelper.getMessage(messageKey, args);
		if (null != message) {
			return message;
		}
		
		return super.retrieveMessage(messageKey, args);
		
	}
	
	public void setRepositoryMessagesHelper(RepositoryMessagesHelper messagesHelper) {
		this.messagesHelper = messagesHelper;
	}
	
	/**
	 * This is a clearly non efficient way of overriding the original
	 * MessageMethod from Alfresco. Due to the pattern used (using
	 * final methods), we have to duplicate the logic in here.
	 * 
	 * @author bpajot
	 *
	 */
	public class MessageMethod extends AbstractMessageHelper implements TemplateMethodModelEx {
		
		org.springframework.extensions.webscripts.MessageMethod delegate = null;
		
	    public MessageMethod(WebScript webscript) {
	        super(webscript);
	    	delegate = new org.springframework.extensions.webscripts.MessageMethod(webscript);
	    }
	    
	    /**
	     * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
	     */
	    public Object exec(List args) throws TemplateModelException {
	    	
	        int argSize = args.size();
	        if (0 == argSize) return "";
	        
            final Object arg0 = args.get(0);
            if (!(arg0 instanceof TemplateScalarModel)) return "";
            
            final String key = ((TemplateScalarModel)arg0).getAsString();
            if (null == key) return "";
            
            final Object[] params = buildParams(args);
            final String message = messagesHelper.getMessage(key, params);
            if (null != message) return message;
            
            return delegate.exec(args);
	        
	    }
	    
	    private Object[] buildParams(List args) throws TemplateModelException {
	    	
	    	final int argSize = args.size();
	    	final Object[] params = new Object[argSize - 1];
	    	
	    	for (int i = 1; i < argSize; i++) {	
	    		params[i-1] = getTemplateArgValue(args.get(i));
	    	}
	    	
	    	return params;
	    	
	    }
	    
	    private Object getTemplateArgValue(Object value) throws TemplateModelException {
	    	
            if (value instanceof TemplateScalarModel) return ((TemplateScalarModel) value).getAsString();
            else if (value instanceof TemplateNumberModel) return ((TemplateNumberModel) value).getAsNumber();
            else if (value instanceof TemplateDateModel) return ((TemplateDateModel) value).getAsDate();
            else return "";

	    }
	}	
	
}
