package org.bluedolmen.alfresco.marketplace.utils;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NOT USED YET!
 * 
 * @author bpajot
 *
 */
public class JavascriptExecuterHelper {
	
    // Logger
    private static final Log logger = LogFactory.getLog(JavascriptExecuterHelper.class);
       
	private PersonService personService;
	private AuthenticationService authenticationService;
	private NodeService nodeService;
	private ScriptService scriptService;
	private RetryingTransactionHelper retryingTransactionHelper;
	
	private Repository repositoryHelper;
	
	public void executeInTransaction(final ScriptLocation scriptLocation) {
		
		executeInTransaction(scriptLocation, null);
		
	}
	
	public void executeInTransaction(final ScriptLocation scriptLocation, final Map<String, Object> model) {
		
		if (null == scriptLocation) {
			throw new NullPointerException("Invalid null script-location");
		}
		
		doInTransaction(new RetryingTransactionCallback<Void>() {

			@Override
			public Void execute() throws Throwable {
				
				final Map<String, Object> model_ = buildModel(model);
				scriptService.executeScript(scriptLocation, model_);				
				return null;
				
			}
			
		});
		
	}
	

	public void executeInTransaction(final String scriptContent) {
		
		executeInTransaction(scriptContent, null);
		
	}
	
	public void executeInTransaction(final String scriptContent, final Map<String, Object> model) {
		
		if (StringUtils.isBlank(scriptContent)) return;
		
		doInTransaction(new RetryingTransactionCallback<Void>() {

			@Override
			public Void execute() throws Throwable {
				
				final Map<String, Object> model_ = buildModel(model);
				scriptService.executeScriptString(scriptContent, model_);				
				return null;
				
			}
			
		});
		
	}
	
	private void doInTransaction(RetryingTransactionCallback<Void> callback) {
		
		retryingTransactionHelper.doInTransaction(callback);
		
	}
    
    public Map<String, Object> buildModel(final Map<String, Object> extraModelProperties) {
    	
		// get the references we need to build the default scripting data-model
		final String userName = authenticationService.getCurrentUserName();
		final NodeRef personRef = personService.getPerson(userName);
		final NodeRef homeSpaceRef = (NodeRef) nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
		final NodeRef companyHomeRef = getCompanyHome();

		// the default scripting model provides access to well known objects and
		// searching facilities - it also provides basic
		// create/update/delete/copy/move services
		final Map<String, Object> model = scriptService.buildDefaultModel(
			personRef,
			companyHomeRef, 
			homeSpaceRef, 
			null, /* scriptRef */ 
			companyHomeRef, /* documentRef */ 
			companyHomeRef /* spaceRef */
		);
		
		if (null != extraModelProperties) {
			model.putAll(extraModelProperties);
		}
        
		return model;
		
    }

	/**
	 * Gets the company home node
	 * 
	 * @return the company home node ref
	 */
	private NodeRef getCompanyHome() {
		
		return repositoryHelper.getCompanyHome();

	}
	
    
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}


	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setScriptService(ScriptService scriptService) {
		this.scriptService = scriptService;
	}

	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.setAuthenticationService(serviceRegistry.getAuthenticationService());
		this.setPersonService(serviceRegistry.getPersonService());
		this.setNodeService(serviceRegistry.getNodeService());
		this.setScriptService(serviceRegistry.getScriptService());
	}

	public void setRetryingTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
		this.retryingTransactionHelper = retryingTransactionHelper;
	}
	
    
}

