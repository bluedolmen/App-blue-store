package org.bluedolmen.alfresco.marketplace.forms;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class FormUIConfigsGet extends DeclarativeWebScript {
	
	@SuppressWarnings("unused")
	private ServiceRegistry serviceRegistry;
	private RetryingTransactionHelper retryingTransactionHelper;
	private Repository repositoryHelper;
	private SearchService searchService;
	private NodeService nodeService;
	private NamespaceService namespaceService;
	private String configPath = "app:company_home/app:dictionary/app:share_configs";

	// TODO: Not compliant with tenant
	private NodeRef configRef = null;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		
		final Map<String, Object> model = new HashMap<String, Object>();
		final List<NodeRef> configFileRefs = getConfigFileRefs();
		
		model.put("configFiles", configFileRefs);
		final Date lastModified = getLastModified(configFileRefs);
		if (null != lastModified) {
			cache.setLastModified(lastModified);
		}
		
		return model;
		
	}
	
	protected Date getLastModified(List<NodeRef> files) {
		
		Date lastModified = null;
		
		for (NodeRef nodeRef : files) {
			final Date modified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
			if (null == lastModified || modified.after(lastModified)) {
				lastModified = modified;
			}
		}
		
		return lastModified;
		
	}
	
	protected List<NodeRef> getConfigFileRefs() {
		
		final NodeRef configHome = getConfigHome(false /* mustExist */);
		if (null == configHome) return Collections.emptyList();
		
		// Use the current logged user rights
		return searchService.selectNodes(
				configHome, 
				"*//.[like(@cm:name,'*-config.xml')]", 
				null /* parameters */, 
				namespaceService, 
				false /* followAllParentLinks */
		);
		
	}
	
	protected NodeRef getConfigHome(final boolean mustExist) {
        
        if (configRef == null || !nodeService.exists(configRef)) {
        	
            configRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            	
                public NodeRef doWork() throws Exception {
                	
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {
                    	
                        public NodeRef execute() throws Exception {
                        	
                            final List<NodeRef> refs = searchService.selectNodes(repositoryHelper.getRootHome(), configPath, null, namespaceService, false);
                            if (refs.isEmpty()) {                            	
                            	if (mustExist) {
                            		throw new IllegalStateException("Invalid config home path: " + configPath + " - found: " + refs.size());
                            	}
                            	return null;
                            }
                            
                            return refs.get(0);
                            
                        }
                        
                    }, true);
                    
                }
                
            }, AuthenticationUtil.getSystemUserName());
            
        }
        
        return configRef;
		
	}
	
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.searchService = serviceRegistry.getSearchService();
		this.nodeService = serviceRegistry.getNodeService();
		this.namespaceService = serviceRegistry.getNamespaceService();
		this.retryingTransactionHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
	}
	
	public void setRepositoryHelper(Repository repository) {
		this.repositoryHelper = repository;
	}
	
}
