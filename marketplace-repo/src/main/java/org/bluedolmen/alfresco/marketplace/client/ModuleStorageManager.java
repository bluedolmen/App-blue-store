package org.bluedolmen.alfresco.marketplace.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.model.MarketPlaceModel;
import org.bluedolmen.alfresco.marketplace.utils.FileFolderUtil;
import org.bluedolmen.alfresco.marketplace.utils.FileFolderUtilImpl;
import org.bluedolmen.alfresco.marketplace.utils.ModuleManager;
import org.bluedolmen.marketplace.commons.module.ModuleDescription;
import org.bluedolmen.marketplace.commons.module.ModuleDescriptionFactory;
import org.bluedolmen.marketplace.commons.module.ModuleId;
import org.bluedolmen.marketplace.commons.module.Version;

/**
 * This class is providing the necessary material to manage the storage of the
 * modules on the <emph>client</emph> side.
 * 
 * @author pajot-b
 * 
 */
public class ModuleStorageManager {
	
	private static final Log logger = LogFactory.getLog(ModuleStorageManager.class);

	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private ContentService contentService;
	private RetryingTransactionHelper retryingTransactionHelper;
	private ModuleManager moduleManager;
	private FileFolderUtil fileFolderUtil;
	private AppStoreRemoteManager appStoreRemoteManager;
	private Repository repositoryHelper;
	
	private NodeRef storageRootNode;
	private String storagePath = "app:company_home/app:dictionary/app:appstore";
	
	/**
	 * Get the root-storage node. The implementation may
	 * decide to create the node if it does not yet exist.
	 * <p>
	 * Note: An implementation may decide not to provide a single
	 * root-node.
	 * 
	 * @return
	 */
	public NodeRef getRootStorageNode() {
		
		if (null == storageRootNode) {
			cacheRepositoryRootNode(true);
		}
		
		return storageRootNode;
		
	}
	
	public NodeRef getStoredModuleNode(String moduleId) {
		
		final NodeRef rootStorageNode = getRootStorageNode();
		if (null == rootStorageNode) return null;
		
		// Normal users should be allowed to list children
		final List<ChildAssociationRef> childrenAssocs = nodeService.getChildAssocsByPropertyValue(rootStorageNode, MarketPlaceModel.PROP_FULLID, moduleId);
		if (childrenAssocs.isEmpty()) return null;
		
		return childrenAssocs.get(0).getChildRef();
		
	}
		
	public NodeRef downloadModule(HttpSession session, String moduleId, Version version) throws IOException {
		
		File tempFile = null;
		try {
			
			tempFile = appStoreRemoteManager.downloadToTemporary(session, moduleId, version /* may be null for the latest */);
			
			return storeModule(tempFile);
			
		}
		finally {
			if (null != tempFile) {
				tempFile.delete();
			}
		}
		
	}
	
	public void removeStoredModule(String moduleId) {
		
		final NodeRef currentModuleNode = getStoredModuleNode(moduleId);
		if (null == currentModuleNode) {
			throw new IllegalArgumentException("The module '" + moduleId + "' is not stored");
		}
		
		
	}
	
	void removeStoredModule(NodeRef moduleNode) {
		
		nodeService.removeAspect(moduleNode, ContentModel.ASPECT_UNDELETABLE);
		nodeService.deleteNode(moduleNode);
		
	}
	
	
	/**
	 * Update the installed state of the module-node.
	 * 
	 * @param moduleNode
	 */
	void updateInstalledState(final NodeRef moduleNode, final boolean installed) {
		
		final RetryingTransactionCallback<Void> callback = new RetryingTransactionCallback<Void>() {
			@Override
			public Void execute() throws Throwable {
				
				if (installed) {
					final Map<QName, Serializable> properties = new HashMap<QName, Serializable>(2);
					properties.put(MarketPlaceModel.PROP_INSTALLED_BY, AuthenticationUtil.getFullyAuthenticatedUser());
					properties.put(MarketPlaceModel.PROP_INSTALLED_ON, new Date());
					
					if (!nodeService.hasAspect(moduleNode, MarketPlaceModel.ASPECT_INSTALLED)) {
						nodeService.addAspect(moduleNode, MarketPlaceModel.ASPECT_INSTALLED, properties);
					}
					else {
						nodeService.addProperties(moduleNode, properties);
					}
					
				}
				else {
					nodeService.removeAspect(moduleNode, MarketPlaceModel.ASPECT_INSTALLED);
					nodeService.removeAspect(moduleNode, ContentModel.ASPECT_UNDELETABLE); // associated to the mkp:installed aspect
				}
				
				return null;
				
			}
		};
		
		runTransactionOperationAsSystem(callback);		
		
		
	}	
	
	/*
	 * Package private
	 */
	NodeRef storeModule(final File file) throws IOException {

		final ModuleDescription moduleDescription = ModuleDescriptionFactory.createModuleDescription(file);
		final String moduleId = ModuleId.toFullId(moduleDescription);
		
		final NodeRef currentModuleNode = getStoredModuleNode(moduleId);
		
		if (null != currentModuleNode) { // an existing module can be found, it is an update
			
			final Version currentVersion = moduleManager.getVersion(currentModuleNode);
			final Version newVersion = moduleDescription.getVersion();
			
			if (currentVersion.compareTo(newVersion) > 0) {
			
				if (logger.isDebugEnabled()) {
					logger.debug(
						String.format(
							"During update of module '%s', forcing update to version '%s' (current '%s')",
							moduleId,
							newVersion,
							currentVersion
						)
					);
				}
				
			}

			updateModule(currentModuleNode, new FileInputStream(file));
			return currentModuleNode;
			
		}
		
		return storeModule(new FileInputStream(file), moduleId);
		
	}
	
	private void updateModule(final NodeRef moduleNode, final InputStream content) {
		
		moduleManager.checkModule(moduleNode);
		
		final RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>() {
			@Override
			public NodeRef execute() throws Throwable {
				
				final ContentWriter writer = contentService.getWriter(moduleNode, ContentModel.PROP_CONTENT, true);
				writer.putContent(content);
				
				return moduleNode;
				
			}
		};
		
		runTransactionOperationAsSystem(callback);

	}
	
	/**
	 * Store the module content in the repository given the provided file-name.
	 * <p>
	 * The input-stream will be unconditionally closed at the end of the
	 * process.
	 * 
	 * @param inputStream
	 * @param moduleFileName
	 * @return
	 */
	private NodeRef storeModule(final InputStream inputStream, final String moduleFileName) {
		
		final NodeRef rootStorageNode = getRootStorageNode();
		if (null == rootStorageNode) {
			throw new IllegalStateException("Cannot get the root-node for storing the module.");
		}
		
		final RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>() {
			@Override
			public NodeRef execute() throws Throwable {
				
				final NodeRef moduleNode = moduleManager.createModule(rootStorageNode, inputStream, moduleFileName);
				serviceRegistry.getOwnableService().setOwner(moduleNode, AuthenticationUtil.getSystemUserName()); // take ownership
				
				return moduleNode;
				
			}
		};
		
		return runTransactionOperationAsSystem(callback);
		
	}
	
	private void cacheRepositoryRootNode(boolean createIfNotExist){
		
		final NodeRef rootHomeNode = repositoryHelper.getRootHome();		
		final List<String> pathElements = FileFolderUtilImpl.splitToPathElements(storagePath);
		
		storageRootNode = fileFolderUtil.checkPathExists(rootHomeNode, pathElements);
		
		if (null == storageRootNode && !createIfNotExist) return;
		
		final UserTransaction userTransaction = RetryingTransactionHelper.getActiveUserTransaction();
		
		if (
			null == userTransaction ||
			!TxnReadState.TXN_READ_WRITE.equals(AlfrescoTransactionSupport.getTransactionReadState())
		) {
			logger.warn("The transaction is marked as read-only. Do not create the storage root-node yet.");
			return;
		}
		
		// Create it as a super user
		
		final RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>() {
			
			@Override
			public NodeRef execute() throws Throwable {
				
				NodeRef rootNodeRef = fileFolderUtil.checkPathExists(rootHomeNode, pathElements);
				
				if (null == rootNodeRef) {
										
					rootNodeRef = fileFolderUtil.createPathTarget(rootHomeNode, pathElements);
					
					serviceRegistry.getOwnableService().setOwner(rootNodeRef, AuthenticationUtil.getSystemUserName());
					
				}
				
				return rootNodeRef;
			}
		};
		
		storageRootNode = runTransactionOperationAsSystem(callback);
		
	}
	
	private <T> T runTransactionOperationAsSystem(final RetryingTransactionCallback<T> callback) {
		
		final RunAsWork<T> runAsWork = new RunAsWork<T>() {
			@Override
			public final T doWork() throws Exception {
				return retryingTransactionHelper.doInTransaction(callback, false /* readOnly */, false /* requiresNew */);
			}

		};
		
		return runAsSystem(runAsWork);
		
	}
	
	private <T> T runAsSystem(RunAsWork<T> runAsWork) {
		
		return AuthenticationUtil.runAs(runAsWork, AuthenticationUtil.getSystemUserName());
		
	}
	
	public void setModuleManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
		this.fileFolderUtil = moduleManager.getFileFolderUtil();
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.contentService = serviceRegistry.getContentService();
	}
	
	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}	
	
	public void setTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
		this.retryingTransactionHelper = retryingTransactionHelper;
	}
	
	public void setAppStoreRemoteManager(AppStoreRemoteManager appStoreRemoteManager) {
		this.appStoreRemoteManager = appStoreRemoteManager;
	}
	
}
