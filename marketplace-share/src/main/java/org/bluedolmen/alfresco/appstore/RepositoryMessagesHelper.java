package org.bluedolmen.alfresco.appstore;

import java.io.InputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.CachingDateFormat;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.ServletUtil;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.site.AuthenticationUtil;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.Response;

public class RepositoryMessagesHelper {
	
	private static final String CONFIGS_TIMESTAMP_GET = "/bluedolmen/api/configs/timestamp";
	private static final String MESSAGES_CONTENT_GET = "/bluedolmen/api/messages/content?locale={locale}";	
	private static final SimpleDateFormat dateFormat = CachingDateFormat.getDateFormat(CachingDateFormat.FORMAT_FULL_GENERIC, true /* lenient */);
	private static final String LAST_MODIFIED_KEY = "_system_lastModified_";
	
	private static final Log logger = LogFactory.getLog(RepositoryMessagesHelper.class);

	private ConnectorService connectorService = null;
	
	/**
	 * This cache stores the last-modified date retrieved in the current cache
	 * scope. In the scope, the messages on the repository is not checked again;
	 * hence the modifications in the repository messages will not be available.
	 * 
	 * The cache scope is either bound to request context (short live-time) or
	 * to the session.
	 */
	private LRUMap lastModifiedDateCache = new LRUMap(100);
	private int cacheHits = 0;
	private int rqNb = 0;
	private Map<Locale, Properties> cachedMessages = new ConcurrentHashMap<Locale, Properties>();
	
	private boolean bindCacheToSession = false;

	
	public String getMessage(String key, Object... args) {
		
		final Locale locale = I18NUtil.getLocale();
		final Properties properties = getMessages(locale);
		
		if (null == properties || !properties.containsKey(key)) return null;
		
		final String message = properties.getProperty(key);
		return MessageFormat.format(message, args);
		
	}
	
	private Properties getMessages(Locale locale) {
		
		final Date lastModifiedDate = getLastModifiedDate();
		if (null == lastModifiedDate) return null; // TODO: Is this logic actually fine? (w.r.t. server error for example)
		
		Properties properties = cachedMessages.get(locale);
		final Date cacheLastModifiedDate = null == properties ? null : (Date) properties.get(LAST_MODIFIED_KEY);
		
		if (null != properties && null != cachedMessages && null != lastModifiedDate && null != cacheLastModifiedDate) {
			if (lastModifiedDate.compareTo(cacheLastModifiedDate) <= 0) return properties;
		}
		
		properties = retrieveRepositoryMessages(locale); 
		if (null != properties) {
			properties.put(LAST_MODIFIED_KEY, lastModifiedDate);
			cachedMessages.put(locale, properties);
		}
		
		return properties;
		
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
			
		
		final Response response = callAlfresco(CONFIGS_TIMESTAMP_GET, "Unable to retrieve configs time-stamp from Alfresco Repository");
		if (null == response) return null;
		
        logger.info("Successfully retrieved config timestamp from Alfresco.");
        
        /*
         * JSON response content :
         * {
         *   lastModified : "2014-10-29T18:45:59.422+01:00",
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
		
		if (null == date || date.isEmpty()) return null; // Beware! No commons-lang in Share
		
        try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			logger.warn("Error while parsing date", e);
		}
        
        return null;
	}
	
	private Properties retrieveRepositoryMessages(Locale locale) {
		
		if (cachedMessages.containsKey(locale)) {
			final Properties properties =  cachedMessages.get(locale);
			if (null != properties) return properties;
		}
		
		final String url = MESSAGES_CONTENT_GET.replace("{locale}", locale.getLanguage());		
		final Response response = callAlfresco(url, "Unable to retrieve the messages content");
		if (null == response) return null;
		
		final InputStream input = response.getResponseStream();
		
		try {
			
			final Properties properties = new Properties();
			properties.load(input);
			
			cachedMessages.put(locale, properties);
			
			return properties;
			
		} 
		catch (Exception e) {
			logger.error("Cannot read messages content.", e);
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
	

	public void destroy() {
		cachedMessages.clear();
		lastModifiedDateCache.clear();
	}
	
	public void setConnectorService(ConnectorService connectorService) {
		this.connectorService = connectorService;
	}
	
	public void setBindCacheToSession(boolean bindCacheToSession) {
		this.bindCacheToSession = bindCacheToSession;
	}

}
