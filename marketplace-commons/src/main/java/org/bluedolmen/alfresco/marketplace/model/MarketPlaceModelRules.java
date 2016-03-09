package org.bluedolmen.alfresco.marketplace.model;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.bluedolmen.alfresco.marketplace.utils.ModuleManager;
import org.bluedolmen.marketplace.commons.module.ModuleId;
import org.springframework.beans.factory.InitializingBean;

public class MarketPlaceModelRules implements InitializingBean {
	
	private PolicyComponent policyComponent;
	private NodeService nodeService;
	private ModuleManager moduleManager;

	@Override
	public void afterPropertiesSet() throws Exception {
		
        policyComponent.bindClassBehaviour(
        	OnUpdatePropertiesPolicy.QNAME, 
    		MarketPlaceModel.ASPECT_MODULE_DESCRIPTION, 
    		new JavaBehaviour(this, "onUpdateModuleProperties")
        );
        
        policyComponent.bindClassBehaviour(
        	OnAddAspectPolicy.QNAME,
        	MarketPlaceModel.ASPECT_MODULE_DESCRIPTION,
        	new JavaBehaviour(this, "onAddModuleDescription")
        );
		
        policyComponent.bindClassBehaviour(
        	OnAddAspectPolicy.QNAME,
        	MarketPlaceModel.ASPECT_MODULE,
        	new JavaBehaviour(this, "onAddModuleAspect")
        );
        
	}
	
	public void onUpdateModuleProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
		
		updateFullId(nodeRef);
		
	}
	
	public void onAddModuleDescription(NodeRef nodeRef, QName aspectTypeQName) {
		
		updateFullId(nodeRef);
		
	}
	
	public void onAddModuleAspect(NodeRef nodeRef, QName aspectTypeQName) {
		
		moduleManager.updateModuleProperties(nodeRef, false /* updateLogo */);
		
	}
	
	
	
	private void updateFullId(NodeRef nodeRef) {
		
		final String currentFullId = (String) nodeService.getProperty(nodeRef, MarketPlaceModel.PROP_FULLID);
		final String computedFullId = getFullId(nodeRef);
		
		if (computedFullId.equals(currentFullId)) return; // no need to update the full-id
		
		nodeService.setProperty(nodeRef, MarketPlaceModel.PROP_FULLID, computedFullId);
		
	}
	
	private String getFullId(NodeRef nodeRef) {
		
		final String groupId = (String) nodeService.getProperty(nodeRef, MarketPlaceModel.PROP_GROUPID);
		final String moduleId = (String) nodeService.getProperty(nodeRef, MarketPlaceModel.PROP_ID);
		
		return ModuleId.toFullId(groupId, moduleId);
		
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setModuleManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
	}

}
