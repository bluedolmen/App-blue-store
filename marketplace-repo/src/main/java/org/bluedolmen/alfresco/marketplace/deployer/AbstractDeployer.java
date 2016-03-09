package org.bluedolmen.alfresco.marketplace.deployer;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.bluedolmen.alfresco.marketplace.utils.FileFolderUtil;
import org.bluedolmen.alfresco.marketplace.utils.ModuleManager;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractDeployer implements Deployer, InitializingBean {
	
	protected ModuleManager moduleManager;
	protected FileFolderUtil fileFolderUtil;
	protected DeployerFactory deployerFactory;
	protected ServiceRegistry serviceRegistry;
	protected Repository repositoryHelper;

	@Override
	public void afterPropertiesSet() throws Exception {
		
		if (null == deployerFactory) {
			throw new IllegalStateException("The deployer factory is not defined");
		}
		
		deployerFactory.register(this);
		
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}

	public void setDeployerFactory(DeployerFactory deployerFactory) {
		this.deployerFactory = deployerFactory;
	}
	
	public void setModuleManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
		this.fileFolderUtil = moduleManager.getFileFolderUtil();
	}
	
}
