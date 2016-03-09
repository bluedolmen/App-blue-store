package org.bluedolmen.alfresco.marketplace.client;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.alfresco.service.cmr.repository.NodeRef;
import org.bluedolmen.marketplace.commons.module.Version;

public interface ModuleService {
	
	public enum DeploymentState {
		
		UNAVAILABLE,
		PREPARING,
		DOWNLOADING,
		PROCESSING,
		FAILURE,
		SUCCESS;
		
		public boolean done() {
			return equals(FAILURE) || equals(SUCCESS);
		}
		
	}
	
	public interface Worker {
		
		DeploymentState getDeploymentState();
		
		String getMessage();
		
		Throwable getFailureReason();
		
	}
	
	/**
	 * Get the installed module node or <code>null</code> if it does not exist
	 * 
	 * @param moduleId
	 * @return
	 */
	NodeRef getModuleNode(String moduleId);

	
	Worker getWorker(String moduleId);
	
	public static enum ServerUpdateMode {
		DISABLED,
		ENABLED,
		FORCED
	}
	
	public static class DeployConfig {
		
		public DeployConfig(ServerUpdateMode serverUpdateMode, boolean forceLocalUpdate, boolean overrideExisting){
			
			this.serverUpdateMode = null == serverUpdateMode ? ServerUpdateMode.ENABLED : serverUpdateMode;
			this.forceLocalUpdate = forceLocalUpdate;
			this.overrideExisting = overrideExisting;
			
		};
		
		/**
		 * The way we will connect to the AppStore
		 */
		public final ServerUpdateMode serverUpdateMode;
		
		/**
		 * Force update even if the installed version seems up-to-date
		 */
		public final boolean forceLocalUpdate;
		
		/**
		 * Override existing material while deploying the module
		 */
		public final boolean overrideExisting;
		
		public HttpSession session;
		
		DeployConfig setSession(HttpSession session) {
			
			this.session = session;
			return this;
			
		}
		
		HttpSession getSession() {
			return session;
		}
				
	}
	
	/**
	 * Deploy the given module (-id).
	 * <p>
	 * The module has to be available in the local repository.
	 * 
	 * @param moduleId
	 *            The module as a full-id String
	 * @param version
	 *            The version that should be installed, <code>null</code> for
	 *            the latest
	 * @param deployConfig
	 *            Configuration of the deployment
	 */
	void deployModule(String moduleId, Version version, DeployConfig deployConfig);
	
	/**
	 * Undeploy an installed module.
	 * 
	 * @param moduleId
	 */
	void undeployModule(String moduleId);
	
	/**
	 * Check whether a module (id) is installed.
	 * 
	 * @param moduleId
	 * @return
	 */
	boolean isInstalled(String moduleId);
	
	/**
	 * Get the set of the installed modules (ids)
	 * @return
	 */
	List<NodeRef> getInstalledModuleNodes();
	
	/**
	 * Tells whether this client can install or not modules
	 * @return
	 */
	boolean browseOnlyMode();
	
}
