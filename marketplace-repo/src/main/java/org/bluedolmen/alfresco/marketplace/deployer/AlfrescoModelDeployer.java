package org.bluedolmen.alfresco.marketplace.deployer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.repo.web.scripts.rule.AbstractRuleWebScript;
import org.alfresco.repo.web.scripts.rule.ruleset.RuleRef;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bluedolmen.alfresco.marketplace.utils.JavascriptExecuterHelper;
import org.bluedolmen.alfresco.marketplace.utils.XPathUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

public class AlfrescoModelDeployer extends DirectMappingDeployer {
	
	private static final ThreadLocal<Yaml> yaml = new ThreadLocal<Yaml>() {
		@Override
		protected Yaml initialValue() {
			return new Yaml();
		}
	};
	
	private static final String RULES_FOLDER_NAME = "rules";
	private static final String MODELS_FOLDER_NAME = "models";
	private static final String SCRIPTS_FOLDER_NAME = "scripts";
	private static final String CONFIGS_FOLDER_NAME = "configs";
	
	private static final String XPATH_PROTOCOL = "xpath://";
	private static final int XPATH_PROTOCOL_LENGTH = XPATH_PROTOCOL.length();
	private static final String MODULE_PROTOCOL = "module://"; 
	private static final int MODULE_PROTOCOL_LENGTH = MODULE_PROTOCOL.length();
	
	private static final String KEY_RULES = "rules";
	private static final String KEY_OWNING_NODE = "owningNode";
	private static final String KEY_NODEREF = "nodeRef";
	private static final String KEY_TITLE = "title";
	
	private static final String YAML_EXTENSION = "yml";
	private static final String JSON_EXTENSION = "json";
	
	private RuleService ruleService;
	private RuleParser ruleParser;
	private SearchService searchService;
	private NamespaceService namespaceService;
	private ScriptService scriptService;
	private JavascriptExecuterHelper javascriptExecuterHelper;

	@Override
	public void afterPropertiesSet() throws Exception {
		
		if (null == resourceMapping) {
			resourceMapping = new HashMap<String, String>();
		}
		resourceMapping.put(MODELS_FOLDER_NAME, "/app:dictionary/app:models");
		resourceMapping.put(SCRIPTS_FOLDER_NAME, "/app:dictionary/app:scripts");
		resourceMapping.put(CONFIGS_FOLDER_NAME, "/app:dictionary/app:share_configs");
		
		excludedRootResources.add(RULES_FOLDER_NAME); // ignore rules folder
		
		this.ruleParser = new RuleParser();
		this.ruleService = serviceRegistry.getRuleService();
		this.searchService = serviceRegistry.getSearchService();
		this.namespaceService = serviceRegistry.getNamespaceService();
		
		super.afterPropertiesSet();
		
	}
	
	@Override
	protected DeployWorker getDeployWorker(NodeRef moduleNode, Map<String, ? extends Object> options) {
		return new AlfrescoDeployerWorker(moduleNode, true == (Boolean) options.get(Deployer.OVERRIDE_EXISTING));
	}
	
	protected class AlfrescoDeployerWorker extends DeployWorker {
		
		private final List<JSONObject> deferredRuleDefinitions = new ArrayList<JSONObject>();
		private final List<RuleRef> createdRules = new ArrayList<RuleRef>();
		private final List<NodeRef> deferredModelNodes = new ArrayList<NodeRef>();
		private Map<String, Object> moduleConfig;
	
		protected AlfrescoDeployerWorker(final NodeRef moduleNode, boolean override) {
			
			super(moduleNode, override);
			
			AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
				@Override
				public void beforeCommit(boolean readOnly) {
					setDeferredModelNodesActive();
				}
			});
			
		}
		
		private void setDeferredModelNodesActive() {
			
			for (final NodeRef node : deferredModelNodes) {
				final QName type = nodeService.getType(node);
				if (type.equals(ContentModel.TYPE_DICTIONARY_MODEL)) {
					setModelActive(node);						
				}
			}
			
			for (final String initScriptName : getInitScriptNames()) {
				launchInitScript(initScriptName);
			}
			
		}
		
		private void setModelActive(NodeRef node) {
			nodeService.setProperty(node, ContentModel.PROP_MODEL_ACTIVE, true);
		}
		
		@SuppressWarnings("unchecked")
		private List<String> getInitScriptNames() {
			
			if (null == moduleConfig) return Collections.emptyList();
			final Object object = moduleConfig.get("initScripts");
			if (object instanceof String) return Collections.singletonList((String) object);
			
			if (object instanceof List<?>) return Collections.checkedList((List<String>) object, String.class);
			
			return Collections.emptyList();
			
		}
		
		private void launchInitScript(String initScriptName) {
			
			final NodeRef scriptNode = getRepositoryTarget(initScriptName);
			if (null == scriptNode) {
				logger.warn("Cannot find the deployed resource '" + initScriptName + "' for script initialization.");
				return;
			}
			
			final Map<String, Object> model = javascriptExecuterHelper.buildModel(null);
			scriptService.executeScript(scriptNode, ContentModel.PROP_CONTENT, model);
			
		}
		
		@Override
		protected NodeRef importEntry(JarEntry jarEntry) throws Exception {
			
			final String name = jarEntry.getName();
			final String rootFolderName = extractRootFolderName(name);
			if (RULES_FOLDER_NAME.equals(rootFolderName) && !jarEntry.isDirectory()) {
				storeRule(jarEntry);
			}
			
			final NodeRef node = super.importEntry(jarEntry);
			if (null == node) return null;
			
			/*
			 * Model nodes have to be processed in a deferred manner because we have no
			 * guarantee that the node is specialized to cm:dictionaryModel at this point
			 * (live testing shows a inconsistent behavior)
			 */
			if (MODELS_FOLDER_NAME.equals(rootFolderName) && !jarEntry.isDirectory()) {
				deferredModelNodes.add(node);
			}
			
			return node;
			
		}
		
		@Override
		protected void processModuleConfig(Map<String, Object> moduleConfig) {
			this.moduleConfig = moduleConfig;
			super.processModuleConfig(moduleConfig);
		}
		
		/**
		 * Store rule for deferred processing
		 * 
		 * @param jarEntry
		 * @throws IOException 
		 * @throws JSONException 
		 */
		private void storeRule(JarEntry jarEntry) throws IOException, JSONException {
			
			final String ruleContent = IOUtils.toString(jarInputStream);
			final String entryName = jarEntry.getName();
			final String extensionName = FilenameUtils.getExtension(entryName);
			
			JSONObject ruleDefinition = null;
			if (YAML_EXTENSION.equalsIgnoreCase(extensionName)) {
				ruleDefinition = getRuleDefinitionFromYamlContent(ruleContent);
			}
			else if (JSON_EXTENSION.equalsIgnoreCase(extensionName)) {
				ruleDefinition = new JSONObject(ruleContent);
			}
			else {
				logger.warn("Ignoring rule definition with extension '" + extensionName + "'");
				return;
			}

			deferredRuleDefinitions.add(ruleDefinition);
			
		}
		
		private JSONObject getRuleDefinitionFromYamlContent(String ruleContent) throws JSONException {
			
			@SuppressWarnings("unchecked")
			final Map<String, Object> yamlRuleDefinition = (Map<String, Object>) yaml.get().load(ruleContent);

			/*
			 * (bpajot) A simple new JSONObject(yamlRuleDefinition) is normally
			 * able to do the job in recent version of the json library. However
			 * considering the use of an Alfresco v.4.0.x, the library does not
			 * perform the job recursively leading to a JSON parsing error
			 * hereafter. We thus perform a small recursive function to do the
			 * job whatever the library is.
			 */
						
			final Object jsonObject = buildJSONObjectFromJava(yamlRuleDefinition);
			if (!(jsonObject instanceof JSONObject)) {
				throw new AlfrescoRuntimeException("The rule-definition is not formatted as expected (no JSONObject produced)");
			}
			
			return (JSONObject) jsonObject;
			
		}
		
		private Object buildJSONObjectFromJava(Object object) throws JSONException {
			
			if (null == object) return null;
			
			if (object instanceof Map<?,?>) {
				
				final Map<?,?> map = (Map<?,?>) object;
				final JSONObject json = new JSONObject();
				
				final Iterator<?> keysIterator = map.keySet().iterator();
				while (keysIterator.hasNext()) {
					
					final String key = (String) keysIterator.next();
					final Object value = buildJSONObjectFromJava(map.get(key));
					json.put(key, value);
					
				}
				
				return json;
				
			}
			else if (object instanceof List<?>) {
				
				final List<?> list = (List<?>) object;
				final JSONArray json = new JSONArray();
				
				final Iterator<?> listIterator = list.iterator();
				while (listIterator.hasNext()) {
					
					final Object arrayElement = listIterator.next();
					final Object value = buildJSONObjectFromJava(arrayElement);
					
					json.put(value);
					
				}
				
				return json;
				
			}
			else {
				return object;
			}
			
		}
		
		@Override
		protected void processDeferredEntries() throws Exception {
			
			for (JSONObject ruleDefinition : deferredRuleDefinitions) {
				createdRules.addAll(importRule(ruleDefinition));
			}

			super.processDeferredEntries();
			
		}
		
		protected List<RuleRef> importRule(JSONObject ruleDefinition) throws JSONException, IOException, DeployerException {
			
			final JSONObject jsonRule = (JSONObject) resolveVariables(ruleDefinition);
			final Rule rule = buildRule(jsonRule);
			
			final List<NodeRef> ruleTargets = getRuleTargets(jsonRule);
			final List<RuleRef> ruleRefs = new ArrayList<RuleRef>();
			
			for(NodeRef ruleTarget : ruleTargets) {
				ruleService.saveRule(ruleTarget, rule);
				final FileInfo owningFileInfo = fileFolderService.getFileInfo(ruleTarget);
				final RuleRef ruleRef = new RuleRef(rule, owningFileInfo);
				ruleRefs.add(ruleRef);
			}

			return ruleRefs;
			
		}
		
		protected List<NodeRef> getRuleTargets(JSONObject jsonRule) throws JSONException {
			
			final Object owningNodeDefinition = jsonRule.get(KEY_OWNING_NODE);
			if (!(owningNodeDefinition instanceof JSONObject)) {
				throw new IllegalStateException("Cannot process owning-node definitions other than JSONObject for now, check your definition.");
			}
			final String owningNode_ = ((JSONObject) owningNodeDefinition).getString(KEY_NODEREF); 
			final List<NodeRef> owningNodes = NodeRef.getNodeRefs(owningNode_);
			
			return owningNodes;
			
		}
		
		protected Rule buildRule(JSONObject jsonRule) throws IOException, JSONException {
			
			// Maybe a YAML or a JSON file
			return ruleParser.parseJsonRule(jsonRule);
			
		}
		
		protected Object resolveVariables(Object json) throws JSONException, DeployerException {
			
			if (null == json) return null;
			
			if (json instanceof JSONObject) {
				
				final JSONObject jsonObject = (JSONObject) json;
				final Iterator<?> keysIterator = jsonObject.keys();
				while (keysIterator.hasNext()) {
					
					final String key = (String) keysIterator.next();
					final Object value = jsonObject.get(key);
					if (null == value) continue;
					
					final Object resolvedValue = resolveVariables(value);
					if (!value.equals(resolvedValue)) {
						jsonObject.put(key, resolvedValue);
					}
					
				}
				
			}
			else if (json instanceof JSONArray) {
				
				final JSONArray jsonArray = (JSONArray) json;
				final JSONArray jsonArrayResult = new JSONArray();
				
				for (int i = 0, len = jsonArray.length(); i < len; i++) {
					
					final Object value = jsonArray.get(i);
					final Object resolvedValue = resolveVariables(value);
					
					jsonArrayResult.put(resolvedValue);
					
				}
				
				return jsonArrayResult;
				
			}
			else if (json instanceof String) {
				
				final String jsonString = (String) json;
				return resolveString(jsonString);
				
			}
			
			return json;
			
		}
		
		// TODO: Improve architecture to externalize the resolution e.g. using Javascript
		protected String resolveString(String string) throws DeployerException {
			
			if (StringUtils.isBlank(string)) return string;
			
			if (string.startsWith(XPATH_PROTOCOL)) {
				return resolveXPathValue(string.substring(XPATH_PROTOCOL_LENGTH), false /* allowEmpty */);
			}
			else if (string.startsWith(MODULE_PROTOCOL)) {
				return resolveModulePathValue(string.substring(MODULE_PROTOCOL_LENGTH));
			}
			
			return string;
			
		}
		
		protected String resolveXPathValue(String xpathValue, boolean allowEmpty) throws DeployerException {
			
			if (StringUtils.isBlank(xpathValue)) return "";
			
			xpathValue = XPathUtils.getXPathEquivalentPath(xpathValue);
			
			final NodeRef companyHome = repositoryHelper.getCompanyHome();
			final List<NodeRef> targetNodes = searchService.selectNodes(companyHome, xpathValue, null, namespaceService, false);
			
			if (targetNodes.isEmpty() && !allowEmpty) {
				throw new DeployerException(String.format("The xpath path referenced by '%s' does not match any repository matching node.", xpathValue));
			}
			
			final Iterator<NodeRef> iterator = targetNodes.iterator();
			final StringBuilder resolvedValue = new StringBuilder();
			while (iterator.hasNext()) {
				
				final NodeRef nodeRef = iterator.next();
				resolvedValue.append(nodeRef.toString());
				
				if (iterator.hasNext()) resolvedValue.append(",");
				
			}
						
			return resolvedValue.toString();
			
		}
		
		protected String resolveModulePathValue(String moduleValue) throws DeployerException {
			
			if (StringUtils.isBlank(moduleValue)) return "";
			
			final NodeRef repositoryNode = getRepositoryTarget(moduleValue);
			if (null == repositoryNode) {
				throw new DeployerException(String.format("The module path referenced by '%s' does not match any repository matching node.", moduleValue));
			}
			
			return repositoryNode.toString();
			
		}
		
		@Override
		protected Map<String, Object> buildReportAsMap() {
			
			Map<String, Object> report = super.buildReportAsMap();
			if (null == report) {
				report = new HashMap<String, Object>();
			}
			
			final List<Object> rules = new ArrayList<Object>();
			for (final RuleRef ruleRef : createdRules) {
				
				final Rule rule = ruleRef.getRule();
				final FileInfo fileInfo = ruleRef.getOwningFileInfo();
				
				final Map<String, String> entry_ = new HashMap<String, String>(2);
				entry_.put(KEY_OWNING_NODE, fileInfo.getNodeRef().toString());
				entry_.put(KEY_TITLE, rule.getTitle());
				final NodeRef ruleNodeRef = rule.getNodeRef();
				if (null != ruleNodeRef) {
					entry_.put(KEY_NODEREF, ruleNodeRef.toString());
				}
				rules.add(entry_);
			}
			
			report.put(KEY_RULES, rules);
			
			return report;
			
		}
		
	}
	
//	private static Pattern VARIABLE_PATTERN = Pattern.compile("$\\{([^\\}])\\}");
//	
//	private static String resolveVariables(String input, Map<String, String> variablesDictionary) {
//		
//		if (null == variablesDictionary) return input;
//		
//		String output = input;
//		
//		final Matcher m = VARIABLE_PATTERN.matcher(input);
//		while (m.find()) {
//			final String variableName = m.group(1);
//			if (variablesDictionary.containsKey(variableName)) {
//				final String variableValue = variablesDictionary.get(variableName); 
//				output = output.replaceAll(Pattern.quote("${" + variableName + "}"), variableValue);
//			}
//		}
//		
//		return output;
//		
//	}
//
//	
	
	@Override
	protected UndeployWorker getUndeployWorker(NodeRef moduleNode, Map<String, ? extends Object> options) {
		
		return new AlfrescoUndeployWorker(moduleNode);
		
	}
	
	private class AlfrescoUndeployWorker extends UndeployWorker {
		
		protected AlfrescoUndeployWorker(NodeRef moduleNode) {
			super(moduleNode);
		}

		@Override
		protected void execute() throws DeployerException {
			
			super.execute(); // Has to read the report before to be available
			
			removeInstalledRules();
			
		}
		
		private void removeInstalledRules() throws DeployerException {
			
			try {
				
				final JSONArray rules = (JSONArray) report.get(KEY_RULES);
				if (null == rules) return;
				
				for (int i = 0, len = rules.length(); i < len; i++) {
					
					final JSONObject rule = rules.getJSONObject(i);
					final String nodeRef_ = rule.getString(KEY_NODEREF);
					if (StringUtils.isEmpty(nodeRef_)) continue;

					final String ruleTitle = rule.getString(KEY_TITLE);
					final String owningNode = rule.getString(KEY_OWNING_NODE);
					removeRule(new NodeRef(owningNode), new NodeRef(nodeRef_), ruleTitle);
					
				}
				
			} catch (JSONException e) { // This is not considered as a failure
//				throw new DeployerException("Unexpected formatting of rules deploymenent model.");
				logger.warn("Ignoring rules removing of module '"  + moduleName + "', the rules deployment model is not corret.", e);
			}
			
		}
		
		private void removeRule(NodeRef owningNode, NodeRef ruleNode, String ruleTitle) {
			
			if (!nodeService.exists(owningNode)) {
				ignoreEntry(owningNode.toString(), "rule", StringUtils.isNotBlank(ruleTitle) ? ruleTitle : "?undefined?", "The owning-node does not exist anymore");
				return;
			}
			
			if (!nodeService.exists(ruleNode)) {
				ignoreEntry(ruleNode.toString(), "rule", StringUtils.isNotBlank(ruleTitle) ? ruleTitle : "?undefined?", "The rule does not exist anymore");
				return;
			}
			
			final Rule rule = ruleService.getRule(ruleNode);
			ruleService.removeRule(owningNode, rule);
			
		}
				
	}
	
	/**
	 * A Hack to retrieve the JSON parser from the webscript
	 * 
	 * @author pajot-b
	 *
	 */
	private final class RuleParser extends AbstractRuleWebScript {
		
		private RuleParser() {
			this.setActionService(serviceRegistry.getActionService());
			this.setDictionaryService(serviceRegistry.getDictionaryService());
			this.setFileFolderService(serviceRegistry.getFileFolderService());
			this.setNamespaceService(serviceRegistry.getNamespaceService());
			this.setNodeService(serviceRegistry.getNodeService());
			this.setRuleService(serviceRegistry.getRuleService());			
		}
		
		protected Rule parseJsonRule(JSONObject jsonRule) throws JSONException {
			return super.parseJsonRule(jsonRule);
		};
		
	}

}
