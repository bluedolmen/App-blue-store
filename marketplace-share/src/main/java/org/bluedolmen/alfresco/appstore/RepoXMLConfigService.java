package org.bluedolmen.alfresco.appstore;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.CachingDateFormat;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONObject;
import org.springframework.extensions.config.ConfigImpl;
import org.springframework.extensions.config.ConfigSection;
import org.springframework.extensions.config.ConfigSource;
import org.springframework.extensions.config.evaluator.Evaluator;
import org.springframework.extensions.config.xml.elementreader.ConfigElementReader;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.ServletUtil;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.site.AuthenticationUtil;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.Response;

public class RepoXMLConfigService extends org.springframework.extensions.config.xml.XMLConfigService {
	
	private static final String CONFIGS_STATE_GET = "/bluedolmen/api/configs";
	private static final String CONFIGS_CONTENT_GET = "/bluedolmen/api/configs/content";	
	private static final SimpleDateFormat dateFormat = CachingDateFormat.getDateFormat(CachingDateFormat.FORMAT_FULL_GENERIC, true /* lenient */);
	
	private static final Log logger = LogFactory.getLog(RepoXMLConfigService.class);

	/**
	 * The cached shared config stores the config retrieved by a previous
	 * connection and which is still valid while the modification date is older
	 * than the one stored in the local config.
	 * 
	 * This mechanism has to be changed if we consider a config can change w.r.t
	 * different connecting users.
	 */
	private LocalConfig cachedSharedConfig = null;
	private ConnectorService connectorService = null;
	
	/**
	 * This cache stores the last-modified date retrieved in the current cache
	 * scope. In the scope, the config on the repository is not checked again;
	 * hence the modifications in the repository config will not be available.
	 * 
	 * The cache scope is either bound to request context (short live-time) or
	 * to the session.
	 */
	private LRUMap lastModifiedDateCache = new LRUMap(100);
	private int cacheHits = 0;
	private int rqNb = 0;
	private boolean deserveGlobalConfig = true;
	private boolean bindCacheToSession = false;
	
	final ThreadLocal<LocalConfig> configs = new ThreadLocal<RepoXMLConfigService.LocalConfig>() {
		
		@Override
		protected LocalConfig initialValue() {
			return getNewLocalConfig();
		}		
		
	};
	
	public RepoXMLConfigService(ConfigSource configSource) {
		super(configSource);
	}	
	
	void releaseLocalConfig() {
		configs.remove();
	}
	
	private LocalConfig getNewLocalConfig() {
		
		final Date lastModifiedDate = getLastModifiedDate();
		
		if (null != cachedSharedConfig && null != lastModifiedDate && null != cachedSharedConfig.lastModified) {
			if (lastModifiedDate.compareTo(cachedSharedConfig.lastModified) <= 0) return cachedSharedConfig;
		}
		
		final LocalConfig localConfig = newLocalConfigInstance();
		if (null != localConfig) {
			localConfig.lastModified = lastModifiedDate;
			cachedSharedConfig = localConfig;
		}
		
		return localConfig;
		
	}
	
	protected Date getLastModifiedDate() {
		
		rqNb++;
		
		final String cacheId = getCacheId();
		if (null != cacheId) {
			if (lastModifiedDateCache.containsKey(cacheId)) {
				cacheHits++;
				return (Date) lastModifiedDateCache.get(cacheId);
			}
		}
			
		
		final Response response = callAlfresco(CONFIGS_STATE_GET, "Unable to retrieve form configs from Alfresco");
		if (null == response) return null;
		
        logger.info("Successfully retrieved configs information from Alfresco.");
        
        /*
         * JSON response content :
         * {
         *   lastModified : "2014-10-29T18:45:59.422+01:00",
         *   configs : [
         *     {
         *       "nodeRef": "workspace://SpacesStore/73729655-ec10-4e4e-8c81-d27ef310a1f6", 
         *       "name": "test-custom-config.xml",
         *       "modifiedOn": "2014-10-29T16:19:58.185+01:00"
         *     }
         *   ]
         * }
         * 
         */
        
        try {
        	
	        // extract response
	        final JSONObject json = new JSONObject(response.getResponse());
	        
	        final String lastModified_ = json.getString("lastModified");
	        final Date lastModified = getDate(lastModified_);
	        
	        // Store the date in the cache
	        if (null != cacheId) {
	        	lastModifiedDateCache.put(cacheId, lastModified);
	        }
	        
	        return lastModified;
	        
        } catch (Exception e) {
        	logger.warn("Cannot retrieve the last-modified date of the repository config from Aflresco", e);
		}
        
        return null;
        
	}
	
	private String getCacheId() {
		
		if (bindCacheToSession) {
			
			final HttpSession session = ServletUtil.getSession(false);
			if (null == session) return null;
			
			return session.getId();
			
		}
		else {
			
			final RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();
			if (null == requestContext) return null;
			
			return requestContext.getId();
			
		}
		
	}
	
	private Date getDate(String date) {
		
		if (StringUtils.isBlank(date)) return null;
		
        try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			logger.warn("Error while parsing date", e);
		}
        
        return null;
	}
	
	
	protected LocalConfig newLocalConfigInstance() {
		
		final Element remoteElements = retrieveRepositoryConfigElements();
		if (null == remoteElements) {
			return null;
		}
		
		return new LocalConfig(remoteElements);
		
	}
	
	protected class LocalConfig {
		
		Date lastModified;
		final ConfigImpl globalConfig;
		final Map<String, List<ConfigSection>> sectionsByArea;
		final List<ConfigSection> sections;
		
		private LocalConfig(Element configElements) {
			
            // Get the current configuration from the ConfigService - we don't want to permanently pollute
            // the standard configuration with additions from the modules...
			if (deserveGlobalConfig) {
				this.globalConfig = new ConfigImpl((ConfigImpl) RepoXMLConfigService.super.getGlobalConfigImpl()); // Make a copy of the current global config
			}
			else {
				this.globalConfig = new ConfigImpl(); // fake one -- it will not be used
			}
            
            // Initialise these with the config service values...
            this.sectionsByArea = new HashMap<String, List<ConfigSection>>(RepoXMLConfigService.super.getSectionsByArea()); 
            this.sections = new ArrayList<ConfigSection>(RepoXMLConfigService.super.getSections());
            
			parseConfigExtensions(configElements);
			
		}
		
	    private void parseConfigExtensions(Element configElements) {
	    	
            // Set up containers for our request specific configuration - this will contain data taken from the evaluated modules...
            final Map<String, ConfigElementReader> parsedElementReaders = new HashMap<String, ConfigElementReader>();
            final Map<String, Evaluator> parsedEvaluators = new HashMap<String, Evaluator>();
            final List<ConfigSection> parsedConfigSections = new ArrayList<ConfigSection>();
            
            // Parse and process the parses configuration...
            final String currentArea = parseFragment(configElements, parsedElementReaders, parsedEvaluators, parsedConfigSections);
            
            for (final Map.Entry<String, Evaluator> entry : parsedEvaluators.entrySet()) {
                // add the evaluators to the config service
                parsedEvaluators.put(entry.getKey(), entry.getValue());
            }
            
            for (final Map.Entry<String, ConfigElementReader> entry : parsedElementReaders.entrySet()) {
                // add the element readers to the config service
                parsedElementReaders.put(entry.getKey(), entry.getValue());
            }
            
            for (final ConfigSection section : parsedConfigSections) {
                // Update local configuration with our updated data...
                addConfigSection(section, currentArea, globalConfig, sectionsByArea, sections);
            }
            
	    }
	    
	}
	
	private Element retrieveRepositoryConfigElements() {
		
		final Response response = callAlfresco(CONFIGS_CONTENT_GET, "Unable to retrieve the configuration content");
		if (null == response) return null;
		
		final InputStream input = response.getResponseStream();
		
		try {
			
			final SAXReader reader = new SAXReader();
			final Document document = reader.read(input);
			final Element rootElement = document.getRootElement();

			if (!"alfresco-config".equals(rootElement.getName())){
				throw new DocumentException("The root-element of the XML config-file must be '<alfresco-config>'");
			}
			
			return rootElement;
			
		} catch (DocumentException e) {
			logger.warn("Cannot parse the configuration-content as XML.", e);
		}
		finally {
			IOUtils.closeQuietly(input);
		}
		
		return null;
		
	}
	
	
	private Response callAlfresco(String serviceUrl, String errorMessage) {
		
		final RequestContext rc = ThreadLocalRequestContext.getRequestContext();
		if (null == rc) return null;
		
		final String userId = rc.getUserId();
		if (userId == null || AuthenticationUtil.isGuest(userId)) {
			logger.debug("Ignoring request for the unauthenticated or guest user.");
			return null;
		}
		
		Connector connector;
		try {
			connector = connectorService.getConnector("alfresco", userId, ServletUtil.getSession());
		} catch (ConnectorServiceException e) {
			throw new AlfrescoRuntimeException("Cannot retrieve a valid connector w.r.t. current user/session.", e);
		}
        
        final Response response = connector.call(serviceUrl);
        if (response.getStatus().getCode() != Status.STATUS_OK) {
        	logger.error(errorMessage + ": " + response.getStatus().getCode());
        	return null;
        }
        
        return response;
		
	}
	

	@Override
	public void destroy() {
		super.destroy();
		cachedSharedConfig = null;
		lastModifiedDateCache.clear();
	}
		
	@Override
	protected ConfigImpl getGlobalConfigImpl() {
		
		if (deserveGlobalConfig) {
			final LocalConfig localConfig = configs.get();
			if (null != localConfig) {
				return localConfig.globalConfig;
			}			
		}
		
		return super.getGlobalConfigImpl();
		
	}
	
	@Override
	public List<ConfigSection> getSections() {
		
		final LocalConfig localConfig = configs.get();
		if (null != localConfig) {
			return localConfig.sections;
		}
		
		return super.getSections();
		
	}
	
	@Override
	public Map<String, List<ConfigSection>> getSectionsByArea() {
		
		final LocalConfig localConfig = configs.get();
		if (null != localConfig) {
			return localConfig.sectionsByArea;
		}
		
		return super.getSectionsByArea();
		
	}
	
	public void setConnectorService(ConnectorService connectorService) {
		this.connectorService = connectorService;
	}
	
	public void setDeserveGlobalConfig(boolean deserveGlobalConfig) {
		this.deserveGlobalConfig = deserveGlobalConfig;
	}
	
	public void setBindCacheToSession(boolean bindCacheToSession) {
		this.bindCacheToSession = bindCacheToSession;
	}

}
