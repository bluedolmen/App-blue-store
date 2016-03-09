package org.bluedolmen.alfresco.marketplace.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileFolderServiceType;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.lang.StringUtils;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * @author pajot-b
 *
 */
public final class FileFolderUtilImpl implements FileFolderUtil {
	
    private static final String I18N_PREFIX = "path.filename.";	

	private Repository repositoryHelper;    
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private NamespaceService namespaceService;
	
    private Set<QName> checkedFolderTypes = new HashSet<QName>();
    
    public FileFolderUtil getClassLoaderAwareFileFolderUtil(ClassLoader classLoader) {
    	
    	if (null == classLoader) {
    		throw new NullPointerException("The provided classLoader is null");
    	}
    	
    	return new ClassLoaderAwareFileFolderUtil(classLoader);
    	
    }
    
    public final class ClassLoaderAwareFileFolderUtil implements FileFolderUtil {
    	
    	private final I18NResolver resolver;
    	private final ClassLoader classLoader;
    	private final Map<String, ResourceBundle> resourceBundles = new HashMap<String, ResourceBundle>();
    	
    	private ClassLoaderAwareFileFolderUtil(ClassLoader classLoader) {
    		
    		this.classLoader = classLoader;
    		
    		this.resolver = new I18NResolver() {
				
				@Override
				public String getMessage(String key) {

					for (final Entry<String, ResourceBundle> entry : resourceBundles.entrySet()) {
						
						final String bundleName = entry.getKey();
						ResourceBundle resourceBundle = entry.getValue();
						
						if (null == resourceBundle) {
							resourceBundle = ResourceBundle.getBundle(bundleName, Locale.getDefault(), ClassLoaderAwareFileFolderUtil.this.classLoader);
							resourceBundles.put(bundleName, resourceBundle);
						}
						
						if (resourceBundle.containsKey(key)) {
							return resourceBundle.getString(key);
						}
						
					}
					
					return AlfrescoI18NResolver.INSTANCE.getMessage(key);
					
				}
				
			};
			
    	}
    	
    	public void registerBundle(String bundleName) {
    		
    		resourceBundles.put(bundleName, null); // erase any pre-computed ResourceBundle
    		
    	}

		@Override
		public NodeRef createPathTarget(NodeRef parentNodeRef, List<String> pathElements) {
			return getOrCreatePathTarget(parentNodeRef, pathElements, ContentModel.TYPE_FOLDER);
		}

		@Override
		public NodeRef checkPathExists(NodeRef parentNodeRef, List<String> pathElements) {
			return getOrCreatePathTarget(parentNodeRef, pathElements, null);
		}

		@Override
		public NodeRef getOrCreatePathTarget(NodeRef parentNodeRef, List<String> pathElements, QName folderTypeQName) {
			return getOrCreatePathTarget(parentNodeRef, pathElements, folderTypeQName, null);
		}

		@Override
		public NodeRef getOrCreatePathTarget(NodeRef parentNodeRef, List<String> pathElements, QName folderTypeQName, NodeRef relativeRootNode) {
			return FileFolderUtilImpl.this.getOrCreatePathTarget(parentNodeRef, pathElements, folderTypeQName, relativeRootNode, resolver);
		}
    	
    }
    
	/**
	 * Get the path elements as a valid and modifiable list of strings (
	 * {@link Vector}).
	 * 
	 * @param path
	 * @return
	 */
    public static final Vector<String> splitToPathElements(String path) {
    	
    	final Vector<String> vector= new Vector<String>(Arrays.asList(path.split("/"))); // The path is then modifiable
    	
    	final Iterator<String> it = vector.iterator();
    	while (it.hasNext()) {
    		final String pathElement = it.next();
    		if (StringUtils.isBlank(pathElement)) it.remove();
    	} // safely remove any empty blank elements
    	
    	return vector;
    	
    }
    
    public NodeRef createPathTarget(NodeRef parentNodeRef, List<String> pathElements) {
    	
    	return getOrCreatePathTarget(parentNodeRef, pathElements, ContentModel.TYPE_FOLDER);
    	
    }
    
    public NodeRef checkPathExists(NodeRef parentNodeRef, List<String> pathElements) {
    	
    	return getOrCreatePathTarget(parentNodeRef, pathElements, null);
    	
    }
    
    /**
     * @param parentNodeRef
     * @param pathElements
     * @param folderTypeQName May be null in order not to create the target
     * @return
     */
    public NodeRef getOrCreatePathTarget(
		NodeRef parentNodeRef, 
		List<String> pathElements,
        QName folderTypeQName
    ) {
    	return getOrCreatePathTarget(parentNodeRef, pathElements, folderTypeQName, null);
    }
    
    public NodeRef getOrCreatePathTarget(
		NodeRef parentNodeRef, 
		List<String> pathElements,
        QName folderTypeQName,
        NodeRef relativeRootNode
    ) {
    	return getOrCreatePathTarget(parentNodeRef, pathElements, folderTypeQName, relativeRootNode, AlfrescoI18NResolver.INSTANCE);
    }
    
	/**
	 * This method enables the user to create a folder path in the repository
	 * given a parent-node. The type of the created folders can be provided.
	 * <p>
	 * 
	 * 
	 * @param parentNodeRef
	 * @param pathElements Path elements, has to be non-null
	 * @param folderTypeQName
	 *            May be null in order not to create the target. Most usually
	 *            type <code>Folder</code> of {@link ContentModel}.
	 * @return
	 */
    public NodeRef getOrCreatePathTarget(
		NodeRef parentNodeRef, 
		List<String> pathElements,
        QName folderTypeQName,
        NodeRef relativeRootNode,
        I18NResolver resolver
    ) {
    	
        if (pathElements.isEmpty()) return parentNodeRef;
        
        if (null != folderTypeQName) {
	        // make sure that the folder is correct
        	checkProvidedTypeIsFolder(folderTypeQName);
        }

        NodeRef currentParentRef = parentNodeRef;
        // just loop and create if necessary
        for (final String pathElement : pathElements) {
        	
        	final QName assocQName = QName.createQName(
    			(-1 == pathElement.indexOf(QName.NAMESPACE_PREFIX) ? NamespaceService.CONTENT_MODEL_PREFIX + QName.NAMESPACE_PREFIX : "") + pathElement, 
    			namespaceService
        	);
        	
            // does it exist?
            // Navigation should not check permissions
            final NodeRef nodeRef = getChildAsSystem(currentParentRef, assocQName);
            if (null != nodeRef) { // it exists
            	currentParentRef = nodeRef;
            	continue;
            }
            	
        	if (null == folderTypeQName) return null; // we were asked not to create the folder
        	
			// not present - make it
			// If this uses the public service it will check create permissions
        	final Map<QName, Serializable> properties = new HashMap<QName, Serializable>(1);
        	final String fileName = getFileName(currentParentRef, assocQName, null == relativeRootNode ? parentNodeRef : relativeRootNode, resolver);
        	properties.put(ContentModel.PROP_NAME, fileName);
        	
        	final ChildAssociationRef childAssocRef = nodeService.createNode(
    			currentParentRef, 
    			ContentModel.ASSOC_CONTAINS, 
    			assocQName, 
    			folderTypeQName, 
    			properties
        	);
        	
        	currentParentRef = childAssocRef.getChildRef();
        	
        }
        
        return currentParentRef;
        
    }
    
    private void checkProvidedTypeIsFolder(QName folderTypeQName) {
    	
    	if (!checkedFolderTypes.contains(folderTypeQName)) {
    		
            final FileFolderService fileFolderService = serviceRegistry.getFileFolderService();
	        boolean isFolder = fileFolderService.getType(folderTypeQName) == FileFolderServiceType.FOLDER;
	        if (!isFolder) {
	            throw new IllegalArgumentException("Type is invalid to make folders with: " + folderTypeQName);
	        }
	        checkedFolderTypes.add(folderTypeQName);
    		
    	}
    	
    }
    
    public static interface I18NResolver {
    	
    	String getMessage(String key);
    	
    }
    
    private static final class AlfrescoI18NResolver implements I18NResolver {
    	
    	private static I18NResolver INSTANCE = new AlfrescoI18NResolver();

		@Override
		public String getMessage(String key) {
			return I18NUtil.getMessage(key);
		}
    	
    }
    
    private String getFileName(NodeRef parentNode, QName assocQName, NodeRef relativeRootNode, I18NResolver resolver) {
    	
    	// Look for an absolute path first
    	final String qnamePath = nodeService.getPath(parentNode).toPrefixString(namespaceService) + '/' + assocQName.toPrefixString(namespaceService);
    	String fileName = resolver.getMessage(I18N_PREFIX + qnamePath);
    	if (!StringUtils.isBlank(fileName)) return fileName;
    	
    	// Look for a relative path to the root-node
    	if (null != relativeRootNode) {
	    	final String rootQNamePath = nodeService.getPath(relativeRootNode).toPrefixString(namespaceService);
	    	final String relativeQNamePath = StringUtils.removeStart(qnamePath, rootQNamePath + "/");
	    	fileName = resolver.getMessage(I18N_PREFIX + relativeQNamePath);
	    	if (!StringUtils.isBlank(fileName)) return fileName;
    	}
    			
    	// Look for the simple association QName
    	fileName = resolver.getMessage(I18N_PREFIX + assocQName.toPrefixString(namespaceService));
    	if (!StringUtils.isBlank(fileName)) return fileName;
    	
    	// Then it is the local-name of the association    	
    	return assocQName.getLocalName();
    	
    }
    
    private NodeRef getChildAsSystem(final NodeRef parentNode, final QName assocName) {
    	
        return AuthenticationUtil.runAs(
        		
        		new RunAsWork<NodeRef>() {

					@Override
					public NodeRef doWork() throws Exception {
						
		        		final List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(
		        				parentNode, 
		        				RegexQNamePattern.MATCH_ALL, // May be more restricted
		        				assocName
		        		);
		        		if (childAssocs.isEmpty()) return null;
		        		
		        		return childAssocs.listIterator().next().getChildRef();
		        		
					}
				},
				
        		AuthenticationUtil.getSystemUserName()
        );
    	
    }
    
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.namespaceService = serviceRegistry.getNamespaceService();
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		if (null != this.repositoryHelper) {
			throw new IllegalStateException("The repository-helper can only be initialized once");
		}
		this.repositoryHelper = repositoryHelper;
	}

	public Repository getRepositoryHelper() {
		return this.repositoryHelper;
	}
	
}
