package org.bluedolmen.alfresco.marketplace.client;

import java.io.IOException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.deployer.Deployer;
import org.bluedolmen.alfresco.marketplace.deployer.Deployer.DeployerException;
import org.bluedolmen.alfresco.marketplace.deployer.DeployerFactory;
import org.bluedolmen.alfresco.marketplace.model.MarketPlaceModel;
import org.bluedolmen.alfresco.marketplace.utils.ModuleManager;
import org.bluedolmen.marketplace.commons.module.ModuleDescription;
import org.bluedolmen.marketplace.commons.module.ModuleId;
import org.bluedolmen.marketplace.commons.module.Version;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.InitializingBean;

public class ModuleServiceImpl implements ModuleService, InitializingBean {
	
	private static final Log logger = LogFactory.getLog(ModuleServiceImpl.class);
	
	@SuppressWarnings("unused")
	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private SearchService searchService;
	private RetryingTransactionHelper retryingTransactionHelper;
	
	private ModuleManager moduleManager;
	private ModuleStorageManager moduleStorageManager;
	private DeployerFactory deployerFactory;
	private AppStoreRemoteManager appStoreRemoteManager;
	
	private final ReentrantLock lock = new ReentrantLock();
	private Map<String, AbstractWorker> jobs = new ConcurrentHashMap<String, ModuleServiceImpl.AbstractWorker>();
	
	private Scheduler scheduler;
	private static final String JOBS_CLEANER_CRON = "0 0/5 * * * ?"; // Every 5 minutes
	
	private boolean browseOnlyMode = false;
	
	/**
	 * Made public for the Job Scheduler to access it
	 */
	void cleanUpJobs() {
		
		lock.lock();
		final long currentTime = System.currentTimeMillis();
		for (final String moduleId : jobs.keySet()) {
			
			final AbstractWorker worker = jobs.get(moduleId);
			if (-1 == worker.expiryDate) continue; // not set yet
			
			if (currentTime >= worker.expiryDate) {
				if (logger.isDebugEnabled()) {
					logger.debug("Removing worker instance from the job map for module '" + moduleId + "'");
				}					
			}
			
		}
		lock.unlock();
		
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		final JobDetail jobDetail = new JobDetail("Module Service Jobs Cleaner", Scheduler.DEFAULT_GROUP, JobsCleaner.class);
		jobDetail.getJobDataMap().put("service", this);
		
		final Trigger trigger = new CronTrigger("moduleService.jobsCleaner", Scheduler.DEFAULT_GROUP, JOBS_CLEANER_CRON);
		scheduler.scheduleJob(jobDetail, trigger);
		
	}	
	
	/**
	 * A job that is in charge of removing jobs worker after a "certain amount" of time.
	 * 
	 * @author bpajot
	 *
	 */
	public static class JobsCleaner implements Job {
		
		@Override
		public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
			
			final ModuleServiceImpl service = (ModuleServiceImpl) jobExecutionContext.getJobDetail().getJobDataMap().get("service");
			service.cleanUpJobs();
			
		}
		
	}
	
	@Override
	public NodeRef getModuleNode(String moduleId) {
		
		return moduleStorageManager.getStoredModuleNode(moduleId);
		
	}
	
	@Override
	public Worker getWorker(String moduleId) {
		
		if (StringUtils.isBlank(moduleId)) {
			throw new IllegalArgumentException("The provided module-id is invalid (must be a non-null and non-empty string).");
		}
		
		return jobs.get(moduleId);
		
	}	
	
	@Override
	public void deployModule(String moduleId, Version version, DeployConfig deployConfig) {

		if (StringUtils.isBlank(moduleId)) {
			throw new IllegalArgumentException("The provided module-id is invalid (must be a non-null and non-empty string).");
		}
		
		if (browseOnlyMode()) {
			throw new IllegalStateException("The appstore is configure as browse-only and cannot be used to install modules as such.");
		}

		final AbstractWorker worker = new DeployWorker(moduleId, version, deployConfig);
		launchDeployWorker(worker);
		
	}
		
	private void checkWorkerInProgress(String moduleId) {
		
		final AbstractWorker worker = jobs.get(moduleId);
		if (null == worker) return;
		
		if (!worker.isDone()) {
			final String message = String.format("The module '%s' is already being processed by another process.", moduleId);
			throw new ConcurrentModificationException(message);
		}
		
	}
	
	private void launchDeployWorker(AbstractWorker worker) {
		
		final String moduleId = worker.getModuleId();
		
		checkWorkerInProgress(moduleId);
		
		lock.lock(); // ensure that this operation won't happen during clean-up of jobs
		jobs.put(moduleId, worker);
		lock.unlock();
		
		new Thread(worker).start();
		
	}
	
	
	private abstract class AbstractWorker extends Observable implements Worker, Runnable {
		
		protected final String moduleId;
		protected final int waitingTimeBeforeCleanupInMs;
		private long expiryDate = -1;
		
		private DeploymentState state;
		private String message;
		private Throwable failureReason;
		private boolean done = false;
		
		private final String runAsUser;
		
		protected AbstractWorker(String moduleId) {
			this(moduleId, 5 * 60 * 1000 /* ms */);
		}
		
		private String getModuleId() {
			return moduleId;
		}
		
		protected AbstractWorker(String moduleId, int waitingTimeBeforeCleanupInMs) {
			this.moduleId = moduleId;
			runAsUser = AuthenticationUtil.getRunAsUser();
			this.waitingTimeBeforeCleanupInMs = waitingTimeBeforeCleanupInMs;			
		}
		
		@Override
		public void run() {
			
			try {
				
				runTransactionAsAuthenticatedUser();
				
			}
			catch (Throwable t) {
				
				logger.error(t); // Log the full error
				
				// First level is supposed to come from the RetryingTransactionHelper which converted the exception as a runtime exception
				final Throwable cause = null != t.getCause() ? t.getCause() : t;
				setFailure(null, cause);
				
			}
			finally {
				
				expiryDate = System.currentTimeMillis() + waitingTimeBeforeCleanupInMs;
				
			}
			
		}
		
		protected void runTransactionAsAuthenticatedUser() {
			
			AuthenticationUtil.runAs(new RunAsWork<Void>() {

				@Override
				public Void doWork() throws Exception {
					
					return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {

						@Override
						public Void execute() throws Throwable {
							
							AlfrescoTransactionSupport.bindListener(new TransactionListener());
							executeImpl(); 
							return null;
							
						}
						
					});
					
				}
				
			}, runAsUser);
			
		}
		
		private class TransactionListener extends TransactionListenerAdapter {
			
			@Override
			public void afterCommit() {
				
				fixState();
				notifyObservers(getDeploymentState());
				done = true;
				
			}
			
			@Override
			public void afterRollback() {
				fixState();
				done = true;
			}
			
			public void beforeCompletion() {
				
			};
			
			private void fixState() {
				
				if (!DeploymentState.FAILURE.equals(getDeploymentState())) {
					setState(DeploymentState.SUCCESS);
				}
				
			}
			
		}
		
		public abstract void executeImpl() throws Exception;
		
		protected void setState(DeploymentState state) {
			this.state = state;
			this.message = null;
		}
		
		protected void setState(DeploymentState state, String message) {
			this.state = state;
			this.message = message;
		}
		
		protected void setFailure(String message, Throwable t) {
			
			if (null != failureReason) return; // already set
			
			final String genericErrorMessage = String.format("Error while deploying module '%s' on state '%s'", moduleId, state);
			this.message = StringUtils.isBlank(message) ? genericErrorMessage : message;
			this.failureReason = t;
			
			state = DeploymentState.FAILURE;
			
			if (t instanceof RuntimeException) {
				throw ( (RuntimeException) t);
			}
			else {
				throw new AlfrescoRuntimeException(genericErrorMessage, t);
			}
			
		}
		
		public DeploymentState getDeploymentState() {
			return state;
		}
		
		public boolean isDone() {
			return done;
		}
		
		public Throwable getFailureReason() {
			return failureReason;
		}
		
		public String getMessage() {
			return message;
		}
	}
	
	private final class StubWorker extends AbstractWorker {

		protected StubWorker(String moduleId) {
			super(moduleId, 3000);
		}
		
		@Override
		public void executeImpl() {
			
			try {
				setState(DeploymentState.PREPARING, "Initialization");
				Thread.sleep(1000);
				setState(DeploymentState.DOWNLOADING, "Downloading from AppStore");
				Thread.sleep(3000);
				setState(DeploymentState.PROCESSING, "Processing to Deployment in the repository using a stub worker.");
				Thread.sleep(3000);
				if (!moduleId.endsWith("success")) {
					throw new AlfrescoRuntimeException("Unexpected error while using the stub worker. This is not a real error, since you're using a stub.");
				}
				
			} catch (InterruptedException e) {
				logger.debug(e);
			}
			
		}
		
	}
	
	private class DeployWorker extends AbstractWorker {
		
		private Version version;
		
		private final DeployConfig deployConfig;
		private final HttpSession session; 
		
		private NodeRef moduleNode;
		private Version currentVersion;
		
		public DeployWorker(String moduleId, Version version, DeployConfig deployConfig) {
			
			super(moduleId);
			
			this.version = version;
			
			if (null == deployConfig.serverUpdateMode) {
				this.deployConfig = new DeployConfig(ServerUpdateMode.ENABLED, deployConfig.forceLocalUpdate, deployConfig.overrideExisting);
			}
			else {
				this.deployConfig = deployConfig;
			}
			
			this.session = deployConfig.getSession();
			
			setState(DeploymentState.PREPARING, "Initialization");
			
		}
		
		@Override
		public void executeImpl() throws DeployerException {
		
			if (null == version) {
				retrieveLatestVersion();
			}
			
			moduleNode = moduleStorageManager.getStoredModuleNode(moduleId);
			currentVersion = null != moduleNode ? moduleManager.getVersion(moduleNode) : null;
			
			if (null != moduleNode && isInstalled(moduleNode)) {
				
				prepareInstalledModule();
				
			}
			
			downloadFromAppStore();
			
			setState(DeploymentState.PROCESSING);
			deployModule(deployConfig.overrideExisting);
			
		}
		
		
		protected NodeRef downloadFromAppStore() {
			
			if (ServerUpdateMode.DISABLED.equals(deployConfig.serverUpdateMode)) return null;
			
			if (null != currentVersion && currentVersion.compareTo(version) >= 0) {
				
				if (!ServerUpdateMode.FORCED.equals(deployConfig.serverUpdateMode)) {
					
					logger.info(
						String.format(
							"During update of module '%s', the current version (%s) is more recent than the updating version (%s)",
							moduleId,
							currentVersion,
							version
						)
					);

					return null;
					
				}
				else {
					
					logger.warn(
						String.format(
							"During update of module '%s', forcing update to version '%s' (current '%s')",
							moduleId,
							version,
							currentVersion
						)
					);
					
				}
				
			}
			
			setState(DeploymentState.DOWNLOADING);
			
			try {
				moduleNode = moduleStorageManager.downloadModule(session, moduleId, version);
			} catch (IOException e) {
				setFailure("Error while downloading module " + moduleId + " from the AppStore.", e);
			}
			
			return moduleNode;
			
		}
		
		protected void retrieveLatestVersion() {
			
			if (ServerUpdateMode.DISABLED.equals(deployConfig.serverUpdateMode)) return;
			
			setState(DeploymentState.PREPARING, "Getting last version from the AppStore.");
			
			try {
				version = appStoreRemoteManager.getLatestVersion(session, moduleId);
			}
			catch (IOException e) {
				logger.error(e);
				setFailure("Error while getting the latest version", e);
			}
			
			if (null == version) {
				setFailure("Cannot retrieve the latest version of the module from the AppStore", null);
			}
			
		}

		protected void prepareInstalledModule() {
			
			// TODO: We should probably do something here to process existing material (uninstall, specific update?)
			
		}
		
		protected void deployModule(boolean overrideExisting) throws DeployerException {
			
			if (null == moduleNode) {
				// Not in the local store, nor in the remote store (or server update is disabled)
				throw new DeployerException(String.format("Cannot get a valid module-node in the local or remote repositories for module '%s'", moduleId));
			}
			
			final ModuleDescription moduleDescription = moduleManager.getModuleDescriptionFromProperties(moduleNode);
			final String packaging = moduleDescription.getPackaging();
			final String moduleId = ModuleId.toFullId(moduleDescription);
			final Deployer deployer = deployerFactory.createDeployer(packaging);
			
			if (null == deployer) {
				throw new AlfrescoRuntimeException(
					String.format("There is no deployer for module '%s' using packaging '%s'", moduleId, packaging)
				);
			}
			
			final Map<String, ? extends Object> options = Collections.singletonMap(Deployer.OVERRIDE_EXISTING, overrideExisting);
			deployer.deploy(moduleNode, options);
			
			moduleStorageManager.updateInstalledState(moduleNode, true /* installed */);
			
		}
		
		
	}

	@Override
	public void undeployModule(String moduleId) {
		
		if (StringUtils.isBlank(moduleId)) {
			throw new IllegalArgumentException("The provided module-id is invalid (must be a non-null and non-empty string).");
		}
		
		if (browseOnlyMode()) {
			throw new IllegalStateException("The appstore is configure as browse-only and cannot be used to undeploiy modules as such.");
		}
		
		checkWorkerInProgress(moduleId);

		AbstractWorker worker = null;
		if (moduleId.startsWith("test.test")) {
			worker = new StubWorker(moduleId);
		}
		else {
			worker = new UndeployWorker(moduleId, false);
		}
		
		lock.lock(); // ensure that this operation won't happen during clean-up of jobs
		jobs.put(moduleId, worker);
		lock.unlock();
		
		new Thread(worker).start();
		
	}
	
	private class UndeployWorker extends AbstractWorker {

		private NodeRef moduleNode;
		private final boolean removeCompletely;
		
		protected UndeployWorker(String moduleId, boolean removeCompletely) {
			super(moduleId);
			this.removeCompletely = removeCompletely;
			setState(DeploymentState.PREPARING, "Initialization");
		}

		@Override
		public void executeImpl() throws Exception {
			
			moduleNode = moduleStorageManager.getStoredModuleNode(moduleId);
			
			setState(DeploymentState.PROCESSING);
			undeployModule();
			
		}
		
		private void undeployModule() throws DeployerException {
			
			final ModuleDescription moduleDescription = moduleManager.getModuleDescriptionFromProperties(moduleNode);
			final String packaging = moduleDescription.getPackaging();
			final String moduleId = ModuleId.toFullId(moduleDescription);
			final Deployer deployer = deployerFactory.createDeployer(packaging);
			
			if (null == deployer) {
				throw new AlfrescoRuntimeException(
					String.format("There is no deployer for module '%s' using packaging '%s'", moduleId, packaging)
				);
			}
			
			final Map<String, ? extends Object> options = Collections.emptyMap();
			deployer.undeploy(moduleNode, options);
			
			moduleStorageManager.updateInstalledState(moduleNode, false /* installed */);
			
			if (removeCompletely) {
				moduleStorageManager.removeStoredModule(moduleNode);
			}
			
		}
		
	}

	/* (non-Javadoc)
	 * @see org.bluedolmen.alfresco.marketplace.client.ModuleService#getInstalledModules()
	 * 
	 */
	@Override
	public List<NodeRef> getInstalledModuleNodes() {
		
		final NodeRef rootStorageNode = moduleStorageManager.getRootStorageNode();
		
		final SearchParameters sp = new SearchParameters();
		sp.addStore(rootStorageNode.getStoreRef());
		sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
		sp.setQueryConsistency(QueryConsistency.TRANSACTIONAL_IF_POSSIBLE);

		final String query = "ASPECT:\"" + MarketPlaceModel.ASPECT_INSTALLED + "\"";
		sp.setQuery(query);
		
		/*
		 * This implementation based on the search service only works because of
		 * the transactional approach.
		 */
        final ResultSet resultSet =searchService.query(sp);
        return resultSet.getNodeRefs();

    /*
   	 * This implementation is ineffective since it parses all the children of the root-node.
   	 * <p>
   	 * A better implementation would use a cache (with invalidation) to get a more effective process
   	 * TODO: Perform a better implementation
   	 */
//		final Set<String> installedModules = new HashSet<String>();
//		
//		final List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(rootStorageNode);
//		for (final ChildAssociationRef childAssociationRef : childAssocRefs) {
//			
//			final NodeRef childNode = childAssociationRef.getChildRef();
//			if (!nodeService.hasAspect(childNode, MarketPlaceModel.ASPECT_INSTALLED)) continue;
//			
//			final String fullId = (String) nodeService.getProperty(childNode, MarketPlaceModel.PROP_FULLID);
//			if (StringUtils.isBlank(fullId)) {
//				logger.warn(String.format("Node '%s' does not define a valid full-id", childNode));
//				continue;
//			}
//			
//			installedModules.add(fullId);
//			
//		}
//		
//		return installedModules;
		
	}
	
	@Override
	public boolean isInstalled(String moduleId) {
		
		final NodeRef moduleNode = getModuleNode(moduleId);
		if (null == moduleNode) return false;

		return isInstalled(moduleNode);
		
	}
	
	private boolean isInstalled(NodeRef moduleNode) {
		
		return null != nodeService.getProperty(moduleNode, MarketPlaceModel.PROP_INSTALLED_ON);
		
	}
	
	@Override
	public boolean browseOnlyMode() {
		return browseOnlyMode;
	}
	
	/*
	 * Spring IoC material
	 */
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = serviceRegistry.getNodeService();
		this.searchService = serviceRegistry.getSearchService();
		this.retryingTransactionHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
	}
	
	public void setModuleManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
	}

	public void setModuleStorageManager(ModuleStorageManager moduleStorageManager) {
		this.moduleStorageManager = moduleStorageManager;
	}
	
	public void setDeployerFactory(DeployerFactory deployerFactory) {
		this.deployerFactory = deployerFactory;
	}
	
	public void setAppStoreRemoteManager(AppStoreRemoteManager appStoreRemoteManager) {
		this.appStoreRemoteManager = appStoreRemoteManager;
	}
	
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	public void setBrowseOnlyMode(boolean browseOnlyMode) {
		this.browseOnlyMode = browseOnlyMode;
	}


}
