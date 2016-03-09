package org.bluedolmen.alfresco.marketplace.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.rating.RatingService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.tagging.TaggingService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.alfresco.marketplace.model.MarketPlaceModel;
import org.bluedolmen.marketplace.commons.module.Author;
import org.bluedolmen.marketplace.commons.module.ModuleDescription;
import org.bluedolmen.marketplace.commons.module.ModuleDescriptionFactory;
import org.bluedolmen.marketplace.commons.module.Requirement;
import org.bluedolmen.marketplace.commons.module.Version;
import org.joda.money.Money;

/**
 * Low level common operations dedicated to module management in the Alfresco
 * repository.
 * 
 * @author pajot-b
 * 
 */
public class ModuleManager {

	public static final String TAG_COST_FREE = "#free";
	static final String KEY_CURRENT_MODULE_DESCRIPTION = "mp.currentModuleDescription";
	public static final String FIVE_STAR_SCHEME_NAME = "fiveStarRatingScheme";
    
    private static final Log logger = LogFactory.getLog(ModuleManager.class);

	private NodeService nodeService;
	private ContentService contentService;
    private RatingService ratingService;
    private TaggingService taggingService;
	private FileFolderUtil fileFolderUtil;
	
	public static Map<QName, Serializable> createAlfrescoProperties(ModuleDescription moduleDescription, boolean includeName) {
		
		final Map<QName, Serializable> properties = new HashMap<QName, Serializable>(4);
		
		properties.put(MarketPlaceModel.PROP_NAME, moduleDescription.getName());
		properties.put(MarketPlaceModel.PROP_GROUPID, moduleDescription.getGroupId());
		properties.put(MarketPlaceModel.PROP_ID , moduleDescription.getModuleId());
		properties.put(MarketPlaceModel.PROP_PACKAGING, moduleDescription.getPackaging());
		properties.put(MarketPlaceModel.PROP_CATEGORY, moduleDescription.getCategory());
		
		final Author author = moduleDescription.getAuthor();
		if (null != author) {
			properties.put(MarketPlaceModel.PROP_AUTHOR, author.getUsername());
		}
		
		final String description = moduleDescription.getDescription();
		if (StringUtils.isNotBlank(description)) {
			properties.put(ContentModel.PROP_DESCRIPTION, description);
		}
		
		final Money cost = moduleDescription.getCost();
		if (null != cost) {
			properties.put(MarketPlaceModel.PROP_COST, cost.toString());
		}
		
		if (includeName) {
			properties.put(ContentModel.PROP_NAME, moduleDescription.getImplicitFileName());
		}
		
		return properties;
		
	}
	
	public class NodeRefModuleDescription implements ModuleDescription {
		
		private static final long serialVersionUID = 5186031361766189918L;
		private final NodeRef moduleNode;
		private final Map<QName, Serializable> properties;
		private List<String> screenshotIds = null;
		
		private NodeRefModuleDescription(NodeRef moduleNode) {
			
			if (null == moduleNode) {
				throw new NullPointerException("The provided module-node is null");
			}

			this.moduleNode = moduleNode;
			properties = nodeService.getProperties(moduleNode);
			
		}
		
		void reload() {
			properties.clear();
			properties.putAll(nodeService.getProperties(moduleNode));
			screenshotIds = null;
		}
		
		@Override
		public Author getAuthor() {
			
			final String authorName = (String) properties.get(MarketPlaceModel.PROP_AUTHOR);
			if (null == authorName) return null;
			
			final Author author = new Author();
			author.setUsername(authorName);
			return author;
			
		}
		
		@Override
		public Money getCost() {
			
			final String cost = (String) properties.get(MarketPlaceModel.PROP_COST);
			if (StringUtils.isBlank(cost)) return null;
		
			return Money.parse(cost);
			
		}
		
		@Override
		public String getDescription() {
			return (String) properties.get(ContentModel.PROP_DESCRIPTION);
		}
		
		@Override
		public String getGroupId() {
			return (String) properties.get(MarketPlaceModel.PROP_GROUPID);
		}
		
		@Override
		public String getImplicitFileName() {
			return (String) properties.get(ContentModel.PROP_NAME);
		}
		
		@Override
		public InputStream getLogo() {
			
			final NodeRef thumbnailNode = getModuleThumbnailNode(moduleNode);
			if (null == thumbnailNode) return null;
			
			final ContentReader reader = contentService.getReader(thumbnailNode, ContentModel.PROP_CONTENT);
			return reader.getContentInputStream();
			
		}
		
		@Override
		public String getModuleId() {			
			return (String) properties.get(MarketPlaceModel.PROP_ID);
		}
		
		@Override
		public String getName() {
			return (String) properties.get(MarketPlaceModel.PROP_NAME);
		}
		
		@Override
		public String getPackaging() {
			return (String) properties.get(MarketPlaceModel.PROP_PACKAGING);
		}
		
		@Override
		public BigDecimal getRating() {
			
			float rating = ratingService.getAverageRating(moduleNode, FIVE_STAR_SCHEME_NAME);
			if (-1. != rating) {
				return BigDecimal.valueOf(rating);
			}
			
			final Float rating_ = (Float) properties.get(MarketPlaceModel.PROP_RATING);
			return BigDecimal.valueOf(null != rating_ ? rating_ : -1);
			
		}

		@Override
		public Version getVersion() {
			final String version_ = (String) properties.get(MarketPlaceModel.PROP_VERSION);
			return Version.fromString(version_);
		}
		
		@Override
		public List<String> getScreenshotIds() {
			
			if (null == screenshotIds) {
				screenshotIds = new ArrayList<String>();
				final List<ChildAssociationRef> screenshotChildren = nodeService.getChildAssocs(moduleNode, MarketPlaceModel.ASSOC_SCREENSHOT, RegexQNamePattern.MATCH_ALL);
				for (final ChildAssociationRef screenshotChild : screenshotChildren) {
					screenshotIds.add(screenshotChild.getQName().getLocalName());
				}
				screenshotIds = Collections.unmodifiableList(screenshotIds);
			}
			
			return screenshotIds;
			
		}
		
		@Override
		public InputStream getScreenshot(String screenshotId) {
			
			final NodeRef screenshotNode = getScreenshotNode(moduleNode, screenshotId);
			if (null == screenshotNode) return null;
			
			final ContentReader reader = contentService.getReader(screenshotNode, ContentModel.PROP_CONTENT);
			return reader.getContentInputStream();
			
		}
		
		@Override
		public String getCategory() {
			return (String) properties.get(MarketPlaceModel.PROP_CATEGORY);
		}

		@Override
		public List<String> getTags() {
			return taggingService.getTags(moduleNode);
		}
		
		@Override
		public String getLicenseFileName() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public InputStream getLicenseFile() {
			throw new UnsupportedOperationException();
		}
			
		@Override
		public Requirement getRequirement(String type, String id) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public List<Requirement> getRequirements(String type, String id) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public List<Requirement> getRequires() {
			throw new UnsupportedOperationException();
		}

		
	}
	
	/**
	 * Get the module description w.r.t. meta-data properties of the node.
	 * <p>
	 * The resulting ModuleDescription can thus be incomplete, however the
	 * fundamental properties should be available (groupId, moduleId, version -
	 * if available -, name, description)
	 * 
	 * TODO: We should probably provide an implementation of
	 * {@link ModuleDescription} with a nodeRef instead of this...
	 * 
	 * @param moduleNode
	 * @return
	 */
	public ModuleDescription getModuleDescriptionFromProperties(NodeRef moduleNode) {
		
		if (null == moduleNode) {
			throw new NullPointerException("The provided module-node is null");
		}

		return new NodeRefModuleDescription(moduleNode);
		
	}
	
	public void checkModule(final NodeRef moduleNode) {
		
		if (null == moduleNode) {
			throw new NullPointerException("The provided node is null");
		}
		
		if (!nodeService.exists(moduleNode)) {
			throw new AlfrescoRuntimeException(String.format("The node '%s' does not exist", moduleNode));
		}
		
		if (!nodeService.hasAspect(moduleNode, MarketPlaceModel.ASPECT_MODULE)) {
			throw new AlfrescoRuntimeException(String.format("The node '%s' is not a valid module", moduleNode));
		}
		
	}
	
	public JarInputStream getModuleInputStream(final NodeRef moduleNode) throws IOException {
		
		if (null == moduleNode) {
			throw new NullPointerException("The provided nodeRef is null");
		}
		
		final ContentReader contentReader = contentService.getReader(moduleNode, ContentModel.PROP_CONTENT);
		if (!contentReader.exists()) return null;
		
		final InputStream inputStream = contentReader.getContentInputStream();
		return new JarInputStream(inputStream);
		
	}
	
	public static class InvalidModule extends AlfrescoRuntimeException {

		private static final long serialVersionUID = 7252555805518473053L;
		private final NodeRef nodeRef;
		
		public InvalidModule(NodeRef nodeRef, String message) {
			super(String.format("[%s] " + message, nodeRef));
			this.nodeRef = nodeRef;
		}
		
		public InvalidModule(NodeRef nodeRef, String message, Throwable t) {
			super(String.format("[%s] " + message, nodeRef), t);
			this.nodeRef = nodeRef;
		}
		
		public NodeRef getNodeRef() {
			return nodeRef;
		}
		
	}
	
	/**
	 * Get the module description from a given node (content).
	 * 
	 * @param moduleNodeRef
	 * @return
	 */
	public ModuleDescription getModuleDescriptionFromContent(NodeRef moduleNodeRef) throws InvalidModule {
		
		final ContentReader reader = contentService.getReader(moduleNodeRef, ContentModel.PROP_CONTENT);
		final InputStream inputStream = reader.getContentInputStream();
		
		if (null == inputStream) {
			throw new InvalidModule(moduleNodeRef, "The node does not define any content or it is not accessible.");
		}
		
		try {
			return ModuleDescriptionFactory.createModuleDescription(inputStream);
		} catch (IOException e) {
			throw new InvalidModule(moduleNodeRef, "Cannot get the module-description from the current node");
		}
		finally {
			IOUtils.closeQuietly(inputStream);
		}
		
	}
	
//	/**
//	 * Get the packaging of a module stored in the repository.
//	 * <p>
//	 * The method mainly relies on properties of the meta-data but may check the
//	 * module-content to get the piece of information if it cannot do otherwise.
//	 * 
//	 * @param moduleNode
//	 * @return
//	 */
//	public String getPackaging(NodeRef moduleNode) {
//		
//		if (!nodeService.hasAspect(moduleNode, MarketPlaceModel.ASPECT_MODULE_DESCRIPTION)) {
//			throw new InvalidModule(moduleNode, "The module-description is not set on the node");
//		}
//		
//		String packaging = (String) nodeService.getProperty(moduleNode, MarketPlaceModel.PROP_PACKAGING);
//		if (StringUtils.isBlank(packaging)) { // Probably not possible
//			logger.warn(String.format("Node '%s' does not define a correct packaging property w.r.t. the module specification.", moduleNode));
//			final ModuleDescription moduleDescription = getModuleDescriptionFromContent(moduleNode);
//			packaging = moduleDescription.getPackaging();
//		}
//		
//		if (StringUtils.isBlank(packaging)) {
//			throw new InvalidModule(moduleNode, "The node does not define a valid packaging.");
//		}
//		
//		return packaging;
//		
//	}
	
	/**
	 * Get the version of a module stored in the repository.
	 * <p>
	 * The method mainly relies on properties of the meta-data but may check the
	 * module-content to get the piece of information if it cannot do otherwise.
	 * 
	 * @param moduleNode
	 * @return
	 */
	public Version getVersion(NodeRef moduleNode) {

		checkModule(moduleNode);
		
		final String version_ = (String) nodeService.getProperty(moduleNode, MarketPlaceModel.PROP_VERSION);
		Version version = null;
		if (!StringUtils.isBlank(version_)) { // Probably not possible
			
			version = new Version(version_);
			
		}
		else {
			
			logger.warn(String.format("Node '%s' does not define a correct version property w.r.t. the module specification.", moduleNode));
			final ModuleDescription moduleDescription = getModuleDescriptionFromContent(moduleNode);
			version = moduleDescription.getVersion();
			
			if (null == version) {
				throw new InvalidModule(moduleNode, "The node does not define a valid packaging.");
			}
			
		}
		
		return version;
		
	}
	
	/**
	 * Create a module in the parent directory as a common file with the <code>Module</code> aspect.
	 * 
	 * @param parentRef
	 * @param content
	 * @param moduleDescription
	 * @return
	 */
	public NodeRef createModule(NodeRef parentRef, InputStream content, ModuleDescription moduleDescription) {
		
		if (null == moduleDescription) {
			throw new NullPointerException("The provided module-description is invalid. User the file-name variant if you cannot get the complete module-description.");
		}

		if (null != moduleDescription) {
			AlfrescoTransactionSupport.bindResource(KEY_CURRENT_MODULE_DESCRIPTION, moduleDescription);
		}
		
		final String moduleFileName = moduleDescription.getImplicitFileName();
		
		return createModule(parentRef, content, moduleFileName);
				
	}
	
	/**
	 * Create a module given the parent, the input content and the filename.
	 * <p>
	 * This method is low-level: Using it without checking the existence of a
	 * previous version of the module may lead to some kind of duplicate-child
	 * exceptions w.r.t. the rules that may execute underneath.
	 * 
	 * @param parentRef
	 * @param content
	 *            The stream of the input content. The resource will be closed
	 *            unconditionally at the end of the process
	 * @param moduleFileName
	 * @return
	 */
	public NodeRef createModule(NodeRef parentRef, InputStream content, String moduleFileName) {
		
		if (StringUtils.isBlank(moduleFileName)) {
			throw new IllegalArgumentException("The module id has to be a non-null and non-blank string");
		}
		
		final NodeRef moduleNodeRef = nodeService.createNode(
				parentRef, 
				ContentModel.ASSOC_CONTAINS, /* assocTypeQName */
				QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, moduleFileName),/* assocQName */ 
				ContentModel.TYPE_CONTENT, /* nodeTypeQName */
				null
		).getChildRef();
		
		// Write the content
		final ContentWriter writer = contentService.getWriter(moduleNodeRef, ContentModel.PROP_CONTENT, true);
		writer.putContent(content);
		
		nodeService.addAspect(moduleNodeRef, MarketPlaceModel.ASPECT_MODULE, null); // will trigger the rule to update the properties

		return moduleNodeRef;
		
	}
	
	public void updateAttachedResources(NodeRef moduleNode, ModuleDescription moduleDescription) {
		
		updateTags(moduleNode, moduleDescription);
		updateLogo(moduleNode, moduleDescription);
		updateScreenshots(moduleNode, moduleDescription);
		
	}
	
	public void updateTags(NodeRef moduleNode, ModuleDescription moduleDescription) {
		
		final List<String> tags = new ArrayList<String>(moduleDescription.getTags());
		
		final Money cost = moduleDescription.getCost();
		if (null == cost || cost.isZero()) {
			tags.add(TAG_COST_FREE);
		}
		
		taggingService.setTags(moduleNode, tags);
		
	}
	
	/**
	 * Update the module-node extracted from the content.
	 * <p>
	 * May user the provided module-description for performance/specific reasons.
	 * 
	 * @param moduleNode
	 * @param moduleDescription
	 */
	public void updateLogo(NodeRef moduleNode, ModuleDescription moduleDescription) {
		
		if (null == moduleDescription) {
			moduleDescription = getModuleDescriptionFromContent(moduleNode);
		}
		
		// Write the logo if one can be found
		final InputStream logoInput = moduleDescription.getLogo();
			
		if (null == logoInput) return;
		
		/*
		 * We create the child as a Thumbnail by easiness:
		 * This type is not included in super-type queries and is not archived.
		 * This type however needs a mandatory property for the content 
		 */
		
		
		final NodeRef logoNodeRef = getOrCreateThumbnailNode(moduleNode);
		final ContentWriter writer = contentService.getWriter(logoNodeRef, ContentModel.PROP_CONTENT, true);
		writer.putContent(logoInput);
			
	}
	
	private NodeRef getOrCreateThumbnailNode(NodeRef moduleNode) {
		
		final NodeRef thumbnailNode = getModuleThumbnailNode(moduleNode);
		if (null != thumbnailNode) return thumbnailNode;
		
		// Then create it
		final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
		properties.put(ContentModel.PROP_CONTENT_PROPERTY_NAME, ContentModel.PROP_CONTENT);
		return nodeService.createNode(
			moduleNode, 
			MarketPlaceModel.ASSOC_THUMBNAIL, 
			QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "thumbnail"), 
			ContentModel.TYPE_THUMBNAIL,
			properties
		).getChildRef();
		
	}
	
	public NodeRef getModuleThumbnailNode(NodeRef moduleNode) {
		
		final List<ChildAssociationRef> thumbnailChildren = nodeService.getChildAssocs(
			moduleNode, 
			MarketPlaceModel.ASSOC_THUMBNAIL, 
			RegexQNamePattern.MATCH_ALL
		);
		if (thumbnailChildren.isEmpty()) return null;
		
		return thumbnailChildren.get(0).getChildRef();

	}
	
	public void updateScreenshots(NodeRef moduleNode, ModuleDescription moduleDescription) {
		
		if (null == moduleDescription) {
			moduleDescription = getModuleDescriptionFromContent(moduleNode);
		}
		
		removeExistingScreenshots(moduleNode);
		
		for (final String screenshotId : moduleDescription.getScreenshotIds()) {			
			createScreenshotChild(moduleNode, moduleDescription, screenshotId);
		}
			
	}
	
	private void removeExistingScreenshots(NodeRef moduleNode) {
		
		final List<ChildAssociationRef> screenshotChildren = nodeService.getChildAssocs(moduleNode, MarketPlaceModel.ASSOC_SCREENSHOT, RegexQNamePattern.MATCH_ALL);
		for (final ChildAssociationRef screenshotChild : screenshotChildren) {
			nodeService.removeChildAssociation(screenshotChild);
		}
		
	}
	
	private void createScreenshotChild(NodeRef moduleNode, ModuleDescription moduleDescription, String screenshotId) {
		
		final InputStream input = moduleDescription.getScreenshot(screenshotId);
		if (null == input) return;
		
		final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
		properties.put(ContentModel.PROP_CONTENT_PROPERTY_NAME, ContentModel.PROP_CONTENT);
		
		final NodeRef screenshotNode = nodeService.createNode(
			moduleNode, 
			MarketPlaceModel.ASSOC_SCREENSHOT,
			getScreenshotAssociationQName(screenshotId),
			ContentModel.TYPE_THUMBNAIL,
			properties
		).getChildRef();
		
		final ContentWriter writer = contentService.getWriter(screenshotNode, ContentModel.PROP_CONTENT, true);
		writer.putContent(input);
		
	}
	
	public NodeRef getScreenshotNode(NodeRef moduleNode, String screenshotId) {
		
		final List<ChildAssociationRef> screenshotChildren = nodeService.getChildAssocs(moduleNode, MarketPlaceModel.ASSOC_SCREENSHOT, getScreenshotAssociationQName(screenshotId));
		if (screenshotChildren.isEmpty()) return null;
		
		return screenshotChildren.get(0).getChildRef();
		
	}
	
	private QName getScreenshotAssociationQName(String screenshotId) {
		
//		if (screenshotId.contains("/")) {
//			final int slashIndex = screenshotId.lastIndexOf('/');
//			screenshotId = screenshotId.substring(slashIndex + 1);
//		}
//		if (StringUtils.isBlank(screenshotId)) {
//			throw new IllegalArgumentException("The screenshot-id cannot be empty nor be ended by a '/' character.");
//		}
			
		return QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, screenshotId);
		
	}
	
	/**
	 * Update module-node properties using the extracted module-description
	 * <p>
	 * The file-name may be updated too
	 * 
	 * @param moduleNodeRef
	 * @param updateAttachedResources
	 */
	public void updateModuleProperties(NodeRef moduleNodeRef, boolean updateAttachedResources) {
		
		final ModuleDescription moduleDescription = getCachedModuleDescription(moduleNodeRef);
		
		final Map<QName, Serializable> properties = ModuleManager.createAlfrescoProperties(moduleDescription, true);
		properties.put(MarketPlaceModel.PROP_VERSION, moduleDescription.getVersion().toString());
		nodeService.addProperties(moduleNodeRef, properties);
		
		if (updateAttachedResources) {
			updateAttachedResources(moduleNodeRef, moduleDescription);
		}

	}
	
	/**
	 * Get the module-description from the transaction if possible else extract it from the node
	 * 
	 * @param moduleNodeRef
	 * @return
	 */
	private ModuleDescription getCachedModuleDescription(NodeRef moduleNodeRef) {
		
		// Try using the one stored in the transaction
		final ModuleDescription moduleDescription = AlfrescoTransactionSupport.<ModuleDescription>getResource(KEY_CURRENT_MODULE_DESCRIPTION);
		if (null != moduleDescription) return moduleDescription;
		
		return getModuleDescriptionFromContent(moduleNodeRef);
		
	}
	
	public FileFolderUtil getFileFolderUtil() {
		return fileFolderUtil;
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.contentService = serviceRegistry.getContentService();
		this.nodeService = serviceRegistry.getNodeService();
		this.ratingService = serviceRegistry.getRatingService();
		this.taggingService = serviceRegistry.getTaggingService();
	}
	
	public void setFileFolderUtil(FileFolderUtil fileFolderUtil) {
		this.fileFolderUtil = fileFolderUtil;
	}
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
	
}
