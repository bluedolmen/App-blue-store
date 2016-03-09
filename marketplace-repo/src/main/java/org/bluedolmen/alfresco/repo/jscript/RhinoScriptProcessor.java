package org.bluedolmen.alfresco.repo.jscript;

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.utils.XPathUtils;

public class RhinoScriptProcessor extends org.alfresco.repo.jscript.RhinoScriptProcessor {
	
	private static final Log logger = LogFactory.getLog(RhinoScriptProcessor.class);
	
    private static final String PATH_XPATH = "xpath:";
    private static final int PATH_XPATH_LEN = PATH_XPATH.length();
    
    private NamespaceService namespaceService;
    private SearchService searchService;
    private ContentService contentService;
    private Repository repositoryHelper;
    
	@Override
	public String loadScriptResource(String resource) {
		
		if (resource.startsWith(PATH_XPATH)) {
			
			final NodeRef scriptRef = resolveXPathValue(resource);
			if (null == scriptRef) {
				throw new AlfrescoRuntimeException("Unable to find store path (xpath-based location): " + resource);
			}
			
            // load from NodeRef default content property
            try {
                final ContentReader cr = contentService.getReader(scriptRef, ContentModel.PROP_CONTENT);
                if ( null == cr || !cr.exists() ) {
                    throw new AlfrescoRuntimeException("Included Script Node content not found: " + resource);
                }
                
                return cr.getContentString();
            }
            catch (ContentIOException err) {
                throw new AlfrescoRuntimeException("Unable to load included script repository resource: " + resource);
            }
			
		}

		// Fall back on the default behavior
		return super.loadScriptResource(resource);
		
	}
	
	protected NodeRef resolveXPathValue(String xpathValue) {
		
		if (xpathValue.startsWith(PATH_XPATH)) {
			xpathValue = xpathValue.substring(PATH_XPATH_LEN);
		}
		
		xpathValue = XPathUtils.getXPathEquivalentPath(xpathValue);
				
		final NodeRef companyHome = repositoryHelper.getCompanyHome();
		final List<NodeRef> targetNodes = searchService.selectNodes(companyHome, xpathValue, null, namespaceService, false);
		
		if (targetNodes.isEmpty()) return null;

		if (targetNodes.size() > 1) {
			logger.warn("The XPath path referred by '" + xpathValue + "' targets several nodes, only the first will be returned.");
		}
		
		return targetNodes.get(0);
		
	}
	
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		
		this.searchService = serviceRegistry.getSearchService();
		this.namespaceService = serviceRegistry.getNamespaceService();
		this.contentService = serviceRegistry.getContentService();
		
		super.setServiceRegistry(serviceRegistry);
		
	}
	
	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}
	
}
