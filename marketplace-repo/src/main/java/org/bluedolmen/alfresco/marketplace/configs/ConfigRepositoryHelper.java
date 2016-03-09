package org.bluedolmen.alfresco.marketplace.configs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.CachingDateFormat;
import org.alfresco.util.collections.CollectionUtils;
import org.alfresco.util.collections.Filter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigRepositoryHelper {
	
	private static final Log logger = LogFactory.getLog(ConfigRepositoryHelper.class);
	private static final String ALFRESCO_CONFIG = "alfresco-config";

	private NodeService nodeService;
	private SearchService searchService;
	private Repository repositoryHelper;
	private NamespaceService namespaceService;
	private ContentService contentService;
	private RetryingTransactionHelper retryingTransactionHelper;
	private String configPath = "app:company_home/app:dictionary/app:share_configs";
	
	// TODO: Not compliant with tenant
	private NodeRef configRef = null;
	
	/*
	 * Supposed to be thread-safe
	 */
	private final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();	
	private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
	
	static final SimpleDateFormat dateFormat = CachingDateFormat.getDateFormat(CachingDateFormat.FORMAT_FULL_GENERIC, true /* lenient */);
	
	static Date parseDate(final String date_) {
		
		if (StringUtils.isBlank(date_)) return null;
		
        try {
			return dateFormat.parse(date_);
		} catch (ParseException e) {
			logger.warn("Error while parsing date", e);
		}
        
        return null;
		
	}

	/**
	 * Get the root of the stored config files
	 * 
	 * @param mustExist
	 *            whether this not should exist. If mustExist is true and the
	 *            root-node does not exist, it raises an exception
	 * @return the {@link NodeRef} of the repository root-nod
	 */
	NodeRef getConfigHome(final boolean mustExist) {
        
        if (configRef == null || !nodeService.exists(configRef)) {
        	
            configRef = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
            	
                public NodeRef doWork() throws Exception {
                	
                    return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<NodeRef>() {
                    	
                        public NodeRef execute() throws Exception {
                        	
                            final List<NodeRef> refs = searchService.selectNodes(repositoryHelper.getRootHome(), configPath, null, namespaceService, false);
                            if (refs.isEmpty()) {                            	
                            	if (mustExist) {
                            		throw new IllegalStateException("Invalid config home path: " + configPath + " - found: " + refs.size());
                            	}
                            	return null;
                            }
                            
                            return refs.get(0);
                            
                        }
                        
                    }, true);
                    
                }
                
            }, AuthenticationUtil.getSystemUserName());
            
        }
        
        return configRef;
		
	}
	
	/**
	 * Get the modification date of the configuration root-node. This node must
	 * enforce a pre-computed modification date w.r.t. containing configuration
	 * nodes.
	 * 
	 * @return the date on which the root-node was last modified
	 */
	Date getConfigHomeLastModifiedDate() {
		
		final NodeRef configHome = getConfigHome(false);
		if (null == configHome || !nodeService.exists(configHome)) return null;
		
		return (Date) nodeService.getProperty(configHome, ContentModel.PROP_MODIFIED);
		
	}
	
	/**
	 * Get a list of the config files as {@link NodeRef}
	 * 
	 * @param modifiedAfter
	 *            Filter the list of returned-files to the files modified after
	 *            the given date
	 * @return The list of matching nodes as a list of {@link NodeRef}
	 */
	List<NodeRef> getConfigFileRefs(final Date modifiedAfter) {
		
		return getConfigFileRefs("ends-with(@cm:name,'-config.xml')", modifiedAfter);
		
	}
	
	List<NodeRef> getConfigFileRefs(String searchPattern, final Date modifiedAfter) {
		
		final NodeRef configHome = getConfigHome(false /* mustExist */);
		if (null == configHome) return Collections.emptyList();
		
		if (null != modifiedAfter) {
			final Date lastModifiedDate = getConfigHomeLastModifiedDate();
			if ( null != lastModifiedDate && modifiedAfter.compareTo(lastModifiedDate) >= 0 ) return Collections.emptyList();
		}
		
		// Use the current logged user rights
		// TODO: This search is time-consuming and should probably be changed by a more effective implementation
		final List<NodeRef> nodes = searchService.selectNodes(
			configHome, 
			"*//.[" + searchPattern + "]", 
			null /* parameters */, 
			namespaceService, 
			false /* followAllParentLinks */
		);
		
		if (null == modifiedAfter) return nodes;
		
		return CollectionUtils.<NodeRef>filter(nodes, new Filter<NodeRef>() {
			@Override
			public Boolean apply(NodeRef value) {
				
				final Date modified = (Date) nodeService.getProperty(value, ContentModel.PROP_MODIFIED);
				if (null == modified) return true;
				
				return modified.after(modifiedAfter);
				
			}
		});
		
	}
	
	/**
	 * Get a single merged config composed of the given list of {@link NodeRef}.
	 * <p>
	 * Ignore the config-files that cannot be parsed correctly.
	 * 
	 * @param configRefs
	 *            the list of config refs to dump
	 * @param output
	 *            A non-null {@link OutputStream}. The stream is closed after processing.
	 */
	void dumpMergedConfigs(List<NodeRef> configRefs, OutputStream output) {
		
		if (null == configRefs || null == output) {
			throw new IllegalArgumentException("The list of nodes and the output have to be non-null.");
		}

		try {

			final XMLEventWriter writer = outputFactory.createXMLEventWriter(output);
			
			writer.add(eventFactory.createStartElement("", "", ALFRESCO_CONFIG));
			
			for (final NodeRef configRef : configRefs) {
				parseXMLFile(configRef, writer);
			}
			
			writer.add(eventFactory.createEndElement("", "", ALFRESCO_CONFIG));
			
			writer.close();
			
		}
		catch (XMLStreamException e) {
			throw new AlfrescoRuntimeException("Problem while trying to merge the XML configuration files.", e);
		}
		finally {
			IOUtils.closeQuietly(output);
		}
		
	}
	
	List<NodeRef> getMessageFileRefs(final Date modifiedAfter, Locale locale) {
		
		final List<NodeRef> messageFileRefs = getConfigFileRefs("ends-with(@cm:name,'.properties')", modifiedAfter);
		final Map<String, NodeRef> fileMap = new HashMap<String, NodeRef>();
		
		final String language = locale.getLanguage();
		
		/*
		 * Now we have to extract the resource bundle base-name.
		 */
		for (final NodeRef nodeRef : messageFileRefs) {
			
			final String fileName = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			final int lastDotIndex = fileName.lastIndexOf(".");
			final String baseName = fileName.substring(0, lastDotIndex);
			
			if (!fileName.contains("_")) {
				fileMap.put(baseName, nodeRef);
				continue;
			}
			
			final int lastDashIndex = baseName.lastIndexOf("_");
			final String languageExtension = baseName.substring(lastDashIndex + 1);
			
			if (language.equals(new Locale(languageExtension).getLanguage())) {
				fileMap.put(baseName.substring(0, lastDashIndex), nodeRef);
			}
			
		}
		
		return new ArrayList<NodeRef>(fileMap.values());
		
	}
	
	void dumpMergedMessages(List<NodeRef> messageRefs, OutputStream output) throws IOException {
		
		if (null == messageRefs || null == output) {
			throw new IllegalArgumentException("The list of nodes and the output have to be non-null");
		}
		
		// The retrieved nodes are supposed to be Properties files
		final Properties properties = new Properties();
		
		try {
			
			for(final NodeRef nodeRef : messageRefs) {
				
				final ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
				InputStream input = null;
				try {
					input = reader.getContentInputStream();
					properties.load(input);
				} catch (Exception e) {
					logger.error("Cannot load properties file referred by node '" + nodeRef.toString() + "'");
				}
				finally {
					IOUtils.closeQuietly(input);
				}
				
			}
			
			properties.store(output, "Generated on " + dateFormat.format(new Date()));
			
		}
		finally {
			IOUtils.closeQuietly(output);			
		}
		
	}
	
	private void parseXMLFile(final NodeRef configRef, XMLEventWriter writer) {
		
		final ContentReader contentReader = contentService.getReader(configRef, ContentModel.PROP_CONTENT);
		final InputStream input = contentReader.getContentInputStream();
		try {
			
			final XMLEventReader reader = inputFactory.createXMLEventReader(input);
			final XMLEventReader filteredReader = inputFactory.createFilteredReader(reader, DiscardAlfrescoConfigEventFilter.INSTANCE);
			writer.add(filteredReader);

		} catch (XMLStreamException e) {
			final String fileName = (String) nodeService.getProperty(configRef, ContentModel.PROP_NAME);
			logger.warn("Cannot parse correctly config-file '" + fileName + "'. Ignoring!");
		}
		finally {
			IOUtils.closeQuietly(input);
		}
		
	}
	
	private static class DiscardAlfrescoConfigEventFilter implements EventFilter {
		
		private static final DiscardAlfrescoConfigEventFilter INSTANCE = new DiscardAlfrescoConfigEventFilter(); 
		
		@Override
		public boolean accept(XMLEvent event) {
			
			if (event.isStartDocument() || event.isEndDocument()) {
				return false; // ignore start and end document
			} else if (event.isStartElement()) {
				final StartElement startElement = event.asStartElement();
				if (ALFRESCO_CONFIG.equals(startElement.getName().getLocalPart())) return false;
			}
			else if (event.isEndElement()) {
				final EndElement endElement = event.asEndElement();
				if (ALFRESCO_CONFIG.equals(endElement.getName().getLocalPart())) return false;
			}
			
			return true;
			
		}
		
	}
	
	Date getLastModifiedDate(List<NodeRef> files) {
		
		Date lastModified = getConfigHomeLastModifiedDate();
		
		// TODO: This loop should not be necessary
		for (final NodeRef nodeRef : files) {
			final Date modified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
			if (null == lastModified || modified.after(lastModified)) {
				lastModified = modified;
			}
		}
		
		return lastModified;
		
	}
	

	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.nodeService = serviceRegistry.getNodeService();
		this.searchService = serviceRegistry.getSearchService();
		this.retryingTransactionHelper = serviceRegistry.getTransactionService().getRetryingTransactionHelper();
		this.namespaceService = serviceRegistry.getNamespaceService();
		this.contentService = serviceRegistry.getContentService();
	}
	
	public void setRepositoryHelper(Repository repository) {
		this.repositoryHelper = repository;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}
}
