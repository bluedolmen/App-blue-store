package org.bluedolmen.alfresco.marketplace.deployer;

import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.bluedolmen.marketplace.commons.module.ModuleDescription;
import org.bluedolmen.marketplace.commons.module.ModuleId;

public interface Deployer {
	
	public static final String OVERRIDE_EXISTING = "override-existing";
	public static final String REMOVE_COMPLETELY = "remove-completely";
	
	public static class DeployerException extends Exception {

		private static final long serialVersionUID = -9175687719754231560L;
		private ModuleDescription moduleDescription;
		
		public DeployerException(Throwable t) {
			super(t);
		}
		
		public DeployerException(String message) {
			super(message);
		}
		
		public DeployerException(String message, Throwable t) {
			super(message, t);
		}
		
		public DeployerException setModuleDescription(ModuleDescription moduleDescription) {
			this.moduleDescription = moduleDescription;
			return this;
		}
		
		@Override
		public String getMessage() {
			
			final String message = super.getMessage();
			if (null == moduleDescription) return message;
			
			return "[" + ModuleId.toFullId(moduleDescription) + "] " + message;
			
		}
		
	}
	
	String getPackaging();

	void deploy(NodeRef moduleNode, Map<String, ? extends Object> options) throws DeployerException;
	
	void undeploy(NodeRef moduleNode, Map<String, ? extends Object> options) throws DeployerException;
	
}
