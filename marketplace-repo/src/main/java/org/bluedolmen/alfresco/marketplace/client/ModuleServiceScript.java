package org.bluedolmen.alfresco.marketplace.client;

import java.io.Serializable;
import java.util.List;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.bluedolmen.alfresco.marketplace.client.ModuleService.DeploymentState;
import org.bluedolmen.alfresco.marketplace.client.ModuleService.Worker;
import org.mozilla.javascript.Scriptable;

public class ModuleServiceScript extends BaseScopableProcessorExtension {

	private AppStoreRemoteManager appStoreRemoteManager;
	private ServiceRegistry serviceRegistry;
	private ModuleService moduleService;
	
	private final ValueConverter valueConverter = new ValueConverter();
	
	public Scriptable getInstalledModuleNodes() {
		
		final List<NodeRef> installedModules = moduleService.getInstalledModuleNodes();
		return (Scriptable) valueConverter.convertValueForScript(serviceRegistry, getScope(), null, (Serializable) installedModules);
		
	}
	
	public DeploymentState getDeploymentState(String moduleId) {
		
		final Worker worker = moduleService.getWorker(moduleId);
		if (null == worker) {
			if (moduleService.isInstalled(moduleId)) return DeploymentState.SUCCESS;
			else return DeploymentState.UNAVAILABLE;
		}
		
		return worker.getDeploymentState();
		
	}
	
	public String getAppStoreUrl() {
		
		return appStoreRemoteManager.getAppStoreBaseURI();
		
	}
	
	public boolean isBrowseOnly() {
		
		return moduleService.browseOnlyMode();
		
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setModuleService(ModuleService moduleService) {
		this.moduleService = moduleService;
	}
	
	public void setAppStoreRemoteManager(AppStoreRemoteManager appStoreRemoteManager) {
		this.appStoreRemoteManager = appStoreRemoteManager;
	}
		
}
