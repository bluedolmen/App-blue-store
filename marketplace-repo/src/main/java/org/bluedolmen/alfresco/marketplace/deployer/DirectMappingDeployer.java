package org.bluedolmen.alfresco.marketplace.deployer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.model.MarketPlaceModel;
import org.bluedolmen.alfresco.marketplace.utils.FileFolderUtil;
import org.bluedolmen.alfresco.marketplace.utils.FileFolderUtilImpl;
import org.bluedolmen.alfresco.marketplace.utils.FileFolderUtilImpl.ClassLoaderAwareFileFolderUtil;
import org.bluedolmen.marketplace.commons.module.ModuleDescription;
import org.bluedolmen.marketplace.commons.module.ModuleId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.I18NUtil;
import org.yaml.snakeyaml.Yaml;

public class DirectMappingDeployer extends AbstractDeployer {
	
	protected static final Log logger = LogFactory.getLog(DirectMappingDeployer.class);
	
	protected Map<String, String> resourceMapping;
	protected String packaging;
	
	protected Set<String> excludedRootResources = new HashSet<String>(Arrays.asList(new String[]{"META-INF"}));
	private static final String FILE_MAPPING_PROPERTIES = "META-INF/file-mapping.properties";
	private static final String I18N_MAPPING_PREFIX = "META-INF/messages";
//	private static final String INIT_SCRIPTS_PREFIX = "META-INF/init-scripts";
	private static final String MODULE_CONFIG_YML = "META-INF/module-config.yml";
	
	protected NodeService nodeService;
	protected ContentService contentService;
	protected FileFolderService fileFolderService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		super.afterPropertiesSet();
		
		this.nodeService = serviceRegistry.getNodeService();
		this.contentService = serviceRegistry.getContentService();
		this.fileFolderService = serviceRegistry.getFileFolderService();
		
	}
	
	@Override
	public String getPackaging() {
		return packaging;
	}	
	
	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}
	
	
	private static Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}");
	
	private static String resolveVariables(String input, Map<String, String> variablesDictionary) {
		
		if (null == variablesDictionary) return input;
		
		String output = input;
		
		final Matcher m = VARIABLE_PATTERN.matcher(input);
		while (m.find()) {
			final String variableName = m.group(1);
			if (variablesDictionary.containsKey(variableName)) {
				final String variableValue = variablesDictionary.get(variableName); 
				output = output.replaceAll(Pattern.quote("{" + variableName + "}"), variableValue);
			}
		}
		
		return output;
		
	}
    
	public void setResourceMapping(Map<String, String> resourceMapping) {
		
		this.resourceMapping = resourceMapping;
		
	}
	
	public void setFileNameMapping(String bundleBaseName) {
		I18NUtil.registerResourceBundle(bundleBaseName);
	}
	
	@Override
	public void deploy(NodeRef moduleNode, Map<String, ? extends Object> options) throws DeployerException {
		
		if (null == options) {
			options = Collections.emptyMap();
		}
		
		final DeployWorker worker = getDeployWorker(moduleNode, options);
		try {
			worker.execute();
		} catch (DeployerException e) {
			throw e;
		} catch (Exception e) {
			throw new DeployerException(e);
		}

	}
	
	protected DeployWorker getDeployWorker(NodeRef moduleNode, Map<String, ? extends Object> options) {
		return new DeployWorker(moduleNode, true == (Boolean) options.get(Deployer.OVERRIDE_EXISTING));
	}
	
		
	private static final class NonCloseableInputStreamDecorator extends ProxyInputStream {
		
		public NonCloseableInputStreamDecorator(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public void close() throws IOException {
			// do nothing
		}
		
	}
	
	private static final QName DEPLOYMENT_QNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "deployment");
	@SuppressWarnings("unused")
	private static final QName UNDEPLOYMENT_QNAME = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "undeployment");
	
	/**
	 * Extract the root directory name from the full path name
	 * 
	 * @param name
	 * @return
	 */
	protected static String extractRootFolderName(String name) {
		
		if (!name.contains("/")) return ""; // file at root, the deployer should know what to do (default target)
		final String[] pathElements = name.split(Pattern.quote("/"));
		
		return pathElements[0];
		
	}
	
	protected class DeployWorker {
		
		protected final NodeRef moduleNode;
		protected JarInputStream jarInputStream;
		protected final boolean override;
		private NodeRef rootNode;
		private Map<String, String> resourceMapping;
		private FileFolderUtil fileFolderUtil = DirectMappingDeployer.this.fileFolderUtil;
		private File resourceTempDir;
		
		private Map<String, NodeRef> entries = new LinkedHashMap<String, NodeRef>(10); // keep the order
		
		private final Map<String, String> variablesDictionary = new HashMap<String, String>();
		
		protected DeployWorker(final NodeRef moduleNode, boolean override) {
			this.moduleNode = moduleNode;
			this.resourceMapping = DirectMappingDeployer.this.resourceMapping; // BEFORE computing root-node
			if (null == this.resourceMapping) {
				this.resourceMapping = Collections.emptyMap();
			}
			this.rootNode = getRootNode();
			this.override = override;
			buildVariablesDictionary();
		}
		
		protected NodeRef getRootNode() {
			
			final NodeRef companyHome = repositoryHelper.getCompanyHome();
			
			if (resourceMapping.containsKey("/")) {
				final String rootTargetPath = resourceMapping.get("/");
				final NodeRef rootNode = getTargetResourcePath(rootTargetPath, companyHome, null);
				if (null == rootNode) {
					throw new AlfrescoRuntimeException("The root-node target by path '" + rootTargetPath + "' does not match any valid node.");
				}
			}
			
			return companyHome;
			
		}
		
		/**
		 * Use the resource mapping to get the target Node corresponding to a given
		 * root-folder name.
		 * <p>
		 * If the mapping cannot be found, then the returned node is the root-node
		 * 
		 * @param rootFolderName
		 * @return
		 */
		protected NodeRef getTargetResourcePath(String rootFolderName, Map<String, String> variablesDictionary) {
			
			return getTargetResourcePath(rootFolderName, getRootNode(), variablesDictionary);
			
		}
		
		/**
		 * 
		 * @param rootFolderName
		 * @param rootNode
		 * @return
		 */
		protected NodeRef getTargetResourcePath(String rootFolderName, NodeRef rootNode, Map<String, String> variablesDictionary) {
			
			if (StringUtils.isBlank(rootFolderName)) return rootNode;
			if (!resourceMapping.containsKey(rootFolderName)) return rootNode;
			
			final String targetResourcePath = resourceMapping.get(rootFolderName);
			final String resolvedTargetResourcePath = resolveVariables(targetResourcePath, variablesDictionary);
			final List<String> pathElements = FileFolderUtilImpl.splitToPathElements(resolvedTargetResourcePath);
			
			return fileFolderUtil.getOrCreatePathTarget(rootNode, pathElements, ContentModel.TYPE_FOLDER, getRootNode());
			
		}		
		
		private void buildVariablesDictionary() {
			
			final ModuleDescription moduleDescription = moduleManager.getModuleDescriptionFromProperties(moduleNode);
			variablesDictionary.put("groupId", moduleDescription.getGroupId());
			variablesDictionary.put("moduleId", moduleDescription.getModuleId());
			variablesDictionary.put("packaging", moduleDescription.getPackaging());
			variablesDictionary.put("version", moduleDescription.getVersion().toString());
			
		}
		
		protected Map<String, NodeRef> getEntries() {
			return Collections.unmodifiableMap(entries);
		}
		
		protected NodeRef getRepositoryTarget(String entryName) {
			return entries.get(entryName); 
		}
		
		private Map<String, NodeRef> execute() throws Exception {
			
			try {
				
				openStream();
				parseEntries();
				processDeferredEntries();
				storeInstallationInformation();
				
			}
			finally {
				IOUtils.closeQuietly(jarInputStream);
				executeFinally();
			}
			
			return getEntries();
			
		}
		
		private void openStream() throws IOException {
			
			jarInputStream = moduleManager.getModuleInputStream(moduleNode);
			
		}
		
		private void parseEntries() throws Exception {
			
			JarEntry jarEntry = null;
			
			do {
				
				jarEntry = jarInputStream.getNextJarEntry();
				if (null == jarEntry) break;
				
				final NodeRef newEntry = importEntry(jarEntry);
				if (null == newEntry) continue; // The entry has been skipped
				
				// Store entry for internal mapping purposes and for report processing 
				final String name = jarEntry.getName();
				entries.put(name, newEntry);
				
			} while(true);
			
		}
		
		protected NodeRef importEntry(final JarEntry jarEntry) throws Exception {
			
			final String name = jarEntry.getName();
			final String rootFolderName = extractRootFolderName(name);
			
			if (MODULE_CONFIG_YML.equals(name)) {
				processModuleConfig();
				return null;
			}
			
			if (FILE_MAPPING_PROPERTIES.equals(name)) {
				updateResourceMapping();
				return null;
			}
			
			if (name.startsWith(I18N_MAPPING_PREFIX)) {
				updateI18NConfiguration(name);
			}
			
//			if (name.startsWith(INIT_SCRIPTS_PREFIX)) {
//				processInitScript(name);
//			}
			
			if (isExcluded(rootFolderName)) return null;
			
			final NodeRef targetBaseResourceNode = getTargetResourcePath(rootFolderName, variablesDictionary);
			final boolean mappingOccurred = !this.rootNode.equals(targetBaseResourceNode);
			
			final List<String> pathElements = new ArrayList<String>(Arrays.asList(name.split("/")));
			
			if (mappingOccurred) {
				// A mapping occurred, the first element of the path is no more relevant
				pathElements.remove(0);
				if (pathElements.isEmpty()) return targetBaseResourceNode;
			}
			
			if (jarEntry.isDirectory()) {
				return importDirectory(pathElements, targetBaseResourceNode);
			}
			
			return importFile(pathElements, targetBaseResourceNode);
			
		}
		
		@SuppressWarnings("unchecked")
		protected void processModuleConfig() {
			
			final Yaml yaml = new Yaml();
			final Object object = yaml.load(jarInputStream);
			
			if (!(object instanceof Map<?,?>)) return;
			processModuleConfig((Map<String, Object>) object);
			
		}
		
		protected void processModuleConfig(Map<String, Object> moduleConfig) {
			
			// do nothing there
			
		}
				
		protected void updateResourceMapping() throws IOException {
			
			resourceMapping = new HashMap<String, String>(resourceMapping); // perform a copy first
			
			final Properties properties = new Properties();
			properties.load(jarInputStream); // the stream is not closed after
			
			for (final String propertyName : properties.stringPropertyNames()) {
				final String value = properties.getProperty(propertyName);
				resourceMapping.put(propertyName, value);
			}
			
			this.rootNode = getRootNode();
			
		}
		
		private final Set<String> languages = new HashSet<String>(Arrays.asList(Locale.getISOLanguages()));
		
		protected void updateI18NConfiguration(String name) throws IOException {
			
			final File baseFile = new File(name);
			final String fileName = baseFile.getName(); // get the last part
			final String extension = FilenameUtils.getExtension(fileName);
			if (!"properties".equals(extension)) return;
			
			String baseName = FilenameUtils.getBaseName(fileName);
			// Try to remove language tag
			final int lastUnderscoreIndex = baseName.lastIndexOf("_");
			if (-1 != lastUnderscoreIndex) {
				final String tagName = baseName.substring(lastUnderscoreIndex + 1);
				if (languages.contains(tagName)) {
					baseName = baseName.substring(0, lastUnderscoreIndex);
				}
			}
			
			/**
			 *  Due to the mechanism used, we currently relies on a specific ClassLoader.
			 *  For this, we (unfortunately) have to create a temporary resource file to
			 *  provide a valid URL to a ClassLoaderURL.
			 */
			if (null == resourceTempDir) {
				resourceTempDir = TempFileProvider.getTempDir();
				final ClassLoader cl = new URLClassLoader(new URL[] {resourceTempDir.toURI().toURL()});
				fileFolderUtil = ( (FileFolderUtilImpl) DirectMappingDeployer.this.fileFolderUtil ).getClassLoaderAwareFileFolderUtil(cl);
			}
			
			if ((fileFolderUtil instanceof ClassLoaderAwareFileFolderUtil)) {
				((ClassLoaderAwareFileFolderUtil) fileFolderUtil).registerBundle(baseName);
			}
			
			final File resourceFile = new File(resourceTempDir, fileName);
			final OutputStream output = new FileOutputStream(resourceFile);
			
			try {
				IOUtils.copy(jarInputStream, output);
			}
			finally {
				output.close();
			}
			
		}
		
//		private File initScriptsStorage = null;
//		
//		
//		/*
//		 * The script will be saved in a temporary folder and executed at the
//		 * end of the transaction
//		 */
//		protected void processInitScript(String name) {
//			
//			final File initScriptsStorage = getInitScriptsStorage();
//			
//			final File baseFile = new File(name);
//			final String fileName = baseFile.getName(); // get the last part
//			final String extension = FilenameUtils.getExtension(fileName);
//			if (!"js".equals(extension)) return;
//			
//			if (fileName.endsWith(".lib.js")) return;
//			if (fileName.endsWith(".include.js")) return;
//			
//		}
//		
//		private File getInitScriptsStorage() {
//			
//			if (null == initScriptsStorage) {
//				initScriptsStorage = TempFileProvider.getTempDir();
//			}
//			
//			return initScriptsStorage;
//			
//		}
		
		protected boolean isExcluded(String rootFolderName) {
			
			return excludedRootResources.contains(rootFolderName);
			
		}
		
		private NodeRef importDirectory(List<String> pathElements, final NodeRef rootNode) {
			return fileFolderUtil.createPathTarget(rootNode, pathElements);
		}
		
		private NodeRef importFile(List<String> pathElements, final NodeRef rootNode) {
			
			// Keep the path elements for the parent, saving the last element in a variable
			final String fileName = pathElements.remove(pathElements.size() - 1);
			
			final NodeRef parentNode = pathElements.isEmpty() ? rootNode : fileFolderUtil.createPathTarget(rootNode, pathElements);
			
			
			NodeRef fileRef = fileFolderService.searchSimple(parentNode, fileName);
			if (null != fileRef) {
				if (!override) {
					throw new FileExistsException(parentNode, fileName);
				}
			}
			else {
	            // create content node based on the file name
	            fileRef = fileFolderService.create(parentNode, fileName, ContentModel.TYPE_CONTENT).getNodeRef();
	            
	            final Map<QName, Serializable> titledProps = new HashMap<QName, Serializable>(1, 1.0f);
	            titledProps.put(ContentModel.PROP_TITLE, fileName);
	            nodeService.addAspect(fileRef, ContentModel.ASPECT_TITLED, titledProps);				
			}			
            
            // push the content of the file into the node
            final ContentWriter writer = contentService.getWriter(fileRef, ContentModel.PROP_CONTENT, true);
            writer.guessMimetype(fileName);
            
			/*
			 * Because ContentWriter is closing automatically the input
			 * stream, and we do not want a jar input stream to be closed,
			 * we have to use a "special" input-stream.
			 */
            writer.putContent(new NonCloseableInputStreamDecorator(jarInputStream));
            
            return fileRef;
			
		}
		
		protected void processDeferredEntries() throws Exception {
			// DO nothing there
		}
		
		private void storeInstallationInformation() {
			
			AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {

				@Override
				public NodeRef doWork() throws Exception {
					
					final NodeRef installationDetailsNode = nodeService.createNode(
						moduleNode, 
						MarketPlaceModel.ASSOC_INSTALLATION_DETAILS, 
						DEPLOYMENT_QNAME, 
						ContentModel.TYPE_CONTENT
					).getChildRef();
					
					final String yamlReport = buildReport();
					final ContentWriter writer = contentService.getWriter(installationDetailsNode, ContentModel.PROP_CONTENT, true);
					writer.putContent(yamlReport);
					
					return installationDetailsNode;
				}
				
			}, AuthenticationUtil.getSystemUserName());
			
		}
		
		protected String buildReport() {
			
			final Map<String, Object> report = buildReportAsMap();
			try {
				return new JSONObject(report).toString(4);
			} catch (JSONException e) {
				throw new AlfrescoRuntimeException("Cannot produce the deployment report", e);
			}
			
		}
		
		protected Map<String, Object> buildReportAsMap() {
			
			final Map<String, Object> root = new LinkedHashMap<String, Object>();
			
			root.put("date", ISO8601DateFormat.format(new Date()));
			
			final List<Object> entries = new ArrayList<Object>();
			for (final Entry<String, NodeRef> entry : getEntries().entrySet()) {
				final Map<String, String> entry_ = new HashMap<String, String>(2);
				entry_.put("path", entry.getKey());
				entry_.put("nodeRef", entry.getValue().toString());
				entries.add(entry_);
			}
			
			root.put("entries", entries);
			
			return root;
			
		}
		
		protected void executeFinally() throws Exception {
			
			// clean-up temporary resources
			if (null != resourceTempDir) {
				
				try {
					FileUtils.deleteDirectory(resourceTempDir);
				}
				catch (IOException e) {
					logger.error(e);
				}
				
			}
			
//			if (null != initScriptsStorage) {
//				
//				try {
//					FileUtils.deleteDirectory(initScriptsStorage);
//				}
//				catch (IOException e) {
//					logger.error(e);
//				}
//				
//			}
			
		}
		
 	}

	@Override
	public void undeploy(NodeRef moduleNode, Map<String, ? extends Object> options) throws DeployerException {
		
		if (null == options) {
			options = Collections.emptyMap();
		}
		
		final UndeployWorker worker = getUndeployWorker(moduleNode, options);
		worker.execute();
		
	}
	
	protected UndeployWorker getUndeployWorker(NodeRef moduleNode, Map<String, ? extends Object> options) {
		return new UndeployWorker(moduleNode);
	}
	
	protected class UndeployWorker {
		
		
		protected final NodeRef moduleNode;
		protected final String moduleName;
		protected final ModuleDescription moduleDescription;
		
		protected JSONObject report;
		
		protected UndeployWorker(final NodeRef moduleNode) {
			this.moduleNode = moduleNode;
			
			final ModuleDescription moduleDescription = moduleManager.getModuleDescriptionFromProperties(moduleNode);
			this.moduleDescription = moduleDescription;
			moduleName = null == moduleDescription ? moduleNode.toString() : ModuleId.toFullId(moduleDescription);
			
		}
		
		protected void execute() throws DeployerException {
			
			try {
				readReport();
				removeEntries();				
			} catch (JSONException e) {
				throw new DeployerException("Error while reading the deployment report", e);
			}
			
		}
		
		private void readReport() throws DeployerException, JSONException {
			
			final List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(moduleNode, MarketPlaceModel.ASSOC_INSTALLATION_DETAILS, DEPLOYMENT_QNAME);
			if (childAssocRefs.isEmpty()) {
				throw new DeployerException("Cannot find any valid deployment information, so cannot uninstall.").setModuleDescription(moduleDescription);
			}
			
			final NodeRef deploymentInformationNode = childAssocRefs.get(childAssocRefs.size() - 1 /* last */).getChildRef();
			final ContentReader contentReader = contentService.getReader(deploymentInformationNode, ContentModel.PROP_CONTENT);
			
			final String content = contentReader.getContentString();
			
			this.report = (JSONObject) new JSONObject(content);
			
		}		
		
		
		private void removeEntries() throws JSONException {
			
			if (!report.has("entries")) {
				logger.warn("Cannot find any entries while processing report on module '" + moduleName + "'");
				return;
			}
			
			final JSONArray entries = (JSONArray) report.get("entries");
			
			for (int i = entries.length() - 1; i >= 0; i--) {
				
				final JSONObject entry_ = (JSONObject) entries.get(i);
				final String path = (String) entry_.get("path");
				final String nodeRef = (String) entry_.get("nodeRef");
				if (!NodeRef.isNodeRef(nodeRef)) {
					ignoreEntry(nodeRef, "path", path, "Invalid nodeRef");
					continue;
				}
				
				final NodeRef node = new NodeRef(nodeRef);
				removeEntry(node, path);
				
			}
			
		}
		
		private void removeEntry(NodeRef node, String path) {
			
			if (!fileFolderService.exists(node)) return;
			
			final FileInfo fileInfo = fileFolderService.getFileInfo(node);
			if (fileInfo.isFolder()) {
				// A folder has to be empty for removing
				final List<FileInfo> children = fileFolderService.list(node);
				if (!children.isEmpty()) {
					ignoreEntry(node.toString(), "path", path, "Folder not empty");
					return;
				}
			}
			
			 fileFolderService.delete(node);
			
		}
		
		protected void ignoreEntry(String ignoredNodeRef, String kind, String path, String reason) {
			
			logger.warn("Ignoring entry '" + ignoredNodeRef + "' for [" + kind + "] '" + path + "' on module '" + moduleName + "': " + reason);
			
		}
		
 	}
	
	
}
