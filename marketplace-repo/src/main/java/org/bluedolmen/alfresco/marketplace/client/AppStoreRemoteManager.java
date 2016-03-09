package org.bluedolmen.alfresco.marketplace.client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.marketplace.commons.module.Version;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.extensions.config.RemoteConfigElementReader;
import org.springframework.extensions.config.element.GenericConfigElement;
import org.springframework.extensions.config.xml.XMLConfigService.PlaceholderResolvingStringValueResolver;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.exception.CredentialVaultProviderException;
import org.springframework.extensions.webscripts.connector.AlfrescoAuthenticator;
import org.springframework.extensions.webscripts.connector.AuthenticatingConnector;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorContext;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.ConnectorSession;
import org.springframework.extensions.webscripts.connector.CredentialVault;
import org.springframework.extensions.webscripts.connector.CredentialVaultProvider;
import org.springframework.extensions.webscripts.connector.Credentials;
import org.springframework.extensions.webscripts.connector.EndpointManager;
import org.springframework.extensions.webscripts.connector.HttpMethod;
import org.springframework.extensions.webscripts.connector.Response;
import org.springframework.extensions.webscripts.connector.ResponseStatus;
import org.springframework.extensions.webscripts.connector.UserContext;

public class AppStoreRemoteManager implements InitializingBean {
	
	public static final class AppStoreRemoteException extends AlfrescoRuntimeException {

		private static final long serialVersionUID = -357918774134837981L;
		
		public AppStoreRemoteException(String msgId) {
			super(msgId);
		}
		
		public AppStoreRemoteException(String msgId, Throwable t) {
			super(msgId, t);
		}
		
	}
	
	@SuppressWarnings("unused")
	private static final Log logger = LogFactory.getLog(AppStoreRemoteManager.class);

	private static final String VERSION_URI = "/bluedolmen/mp/latestversion/{moduleId}";
	private static final String DOWNLOAD_URI = "/bluedolmen/mp/{moduleId}/{version}";

	private static final String LATEST_VERSION_KEY = "latestVersion";
 
	public File downloadToTemporary(HttpSession session, final String moduleId, final Version version) throws IOException {
		
		final File file = TempFileProvider.createTempFile("download", null);
		final OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
		try {
			download(session, moduleId, version, output);
		}
		finally {
			IOUtils.closeQuietly(output);
		}
		
		return file;
		
	}
	
	/**
	 * Retrieve the latest version number of a given module.
	 * 
	 * @param moduleId the module id
	 * @return
	 * @throws IOException 
	 */
	public Version getLatestVersion(HttpSession session, final String moduleId) throws IOException {
		
		final String uri = VERSION_URI.replace("{moduleId}", moduleId);
		
		Response response = callRemoteMethod(session, uri);
		
		final String responseBody = response.getResponse();
		final JSONObject json = getValidJSONObject(responseBody);
		
		Version version = null;
		if (json.containsKey(LATEST_VERSION_KEY)) {
			final String latestVersion = (String) json.get(LATEST_VERSION_KEY);
			version = Version.fromString(latestVersion);
		}
		
		if (null == version) {
			throw new AppStoreRemoteException("The returned json response does not contains the latest version: " + responseBody);
		}
		
		return version;

	}
	
	private Response callRemoteMethod(HttpSession session, String uri) {
		
		final Connector connector = getConnector(session);
		if (null == connector) {
			throw new AppStoreRemoteException(String.format("Cannot retrieve a valid connector to the remote server"));
		}

		return callRemoteMethod(connector, uri);
		
	}
	
	private Response callRemoteMethod(Connector connector, String uri) {

		final Response response = connector.call(uri);
		final ResponseStatus status = response.getStatus();
		final int statusCode = status.getCode();
		
        if(statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
        	
        	final String redirectLocation = status.getLocation();
        	try {
				return callRemoteMethod(connector, new URI(redirectLocation, true).toString());
			} catch (URIException | NullPointerException e) {
				throw new AlfrescoRuntimeException("Cannot build a valid URI", e);
			}
        	
        }

		if (statusCode != HttpStatus.SC_OK) {
			throw new AppStoreRemoteException(
				String.format("Cannot call the remote URI: %s", uri, status.getMessage())
			);
		}
		
		return response;

	}
	
	private JSONObject getValidJSONObject(String jsonString) {
		
		if (StringUtils.isBlank(jsonString)) {
			throw new AppStoreRemoteException("The content is null or empty.");
		}
		
		try {
			
			final JSONParser jsonParser = new JSONParser();
			final Object json = jsonParser.parse(jsonString);
			
			if (!(json instanceof JSONObject)) {
				throw new AppStoreRemoteException("The response for version number is not a valid JSON Object content: " + jsonString);
			}
			
			return (JSONObject) json;
			
		} catch (ParseException e) {
			throw new AppStoreRemoteException("The response for version number is not a valid JSON content: " + jsonString);
		}
		
	}
		
	/**
	 * Download the module given its module-id and the potential version (may be
	 * null to get the latest)
	 * 
	 * @param moduleId
	 * @param version
	 * @param output
	 * @throws IOException
	 */
	public Response download(final HttpSession session, final String moduleId, final Version version, final OutputStream output) throws IOException {
		
		final String uri = DOWNLOAD_URI
			.replace("{moduleId}", moduleId)
			.replace("{version}", null == version ? "" : version.toString())
			.replace("/?", "?") // fix no version
		;
		
		final Connector connector = getConnector(session);
		if (null == connector) {
			throw new AppStoreRemoteException(String.format("Cannot retrieve a valid connector to the remote server"));
		}
		
		return download(connector, uri, output);
		
	}
	
	private Response download(Connector connector, String uri, final OutputStream output) {

        final ConnectorContext context = new ConnectorContext(HttpMethod.GET);
		final Response response = connector.call(uri, context, null /* in */, output);
		final ResponseStatus status = response.getStatus();
		
		int statusCode = status.getCode();
		
        if(statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
        	
        	final String redirectLocation = status.getLocation();
        	
        	try {
				return download(connector, new URI(redirectLocation, true).toString(), output);
			} catch (URIException | NullPointerException e) {
				throw new AlfrescoRuntimeException("Cannot build a valid URI", e);
			}
        	
        }

		if (statusCode != HttpStatus.SC_OK) {
			throw new AppStoreRemoteException(
				String.format("Cannot call the remote URI: %s", uri, status.getMessage())
			);
		}
		
		return response;
		
	}
	
	
	
	private ConnectorService connectorService;
	private CredentialVaultProvider credentialVaultProvider;
	private Properties properties;
	PlaceholderResolvingStringValueResolver resolver;
	
	public static final String APPSTORE_ENDPOINT = "appstore";
	private static final String VAULT_PROVIDER_ID = "mp.credential.vault.provider";
    private static final String PREFIX_VAULT_SESSION  = "_alfwsf_vaults_"; // reuse the one of the connector service	
    private static final String PREFIX_CONNECTOR_SESSION = "_alfwsf_consession_";
    
	@Override
	public void afterPropertiesSet() throws Exception {
		
		resolver = new PlaceholderResolvingStringValueResolver(
			properties, 
			PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX, 
			PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX, 
			PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR, 
			true
		);
		
	}
	
	public String login(HttpSession session, String username, String password) {

		ParameterCheck.mandatory("session", session);
		ParameterCheck.mandatory("username", username);
		
		final CredentialVault vault = getCredentialVault(session, username);
		Credentials credentials = vault.retrieve(APPSTORE_ENDPOINT);
		if (null == credentials) {
			credentials = vault.newCredentials(APPSTORE_ENDPOINT);
		}
        credentials.setProperty(Credentials.CREDENTIAL_USERNAME, username);
        credentials.setProperty(Credentials.CREDENTIAL_PASSWORD, password);
        
        final AuthenticatingConnector authenticatingConnector = getAuthenticatingConnector(session, username);
		if (!authenticatingConnector.handshake()) return null;
		
        return getTicket(authenticatingConnector);
        
	}
	
	private AuthenticatingConnector getAuthenticatingConnector(HttpSession session, String username) {
		
		final Connector connector = getConnector(session, username);
        final AuthenticatingConnector authenticatingConnector;
        
        if (connector instanceof AuthenticatingConnector) {
            authenticatingConnector = (AuthenticatingConnector)connector;
        }
        else {
        	throw new AlfrescoRuntimeException("Cannot make an authenticated connector on endpoint " + APPSTORE_ENDPOINT);
        }
        
        return authenticatingConnector;
        
	}
	
	private String getTicket(AuthenticatingConnector authenticatingConnector) {
		
		final ConnectorSession connectorSession = authenticatingConnector.getConnectorSession();
		if (null == connectorSession) return null;
		
		return connectorSession.getParameter(AlfrescoAuthenticator.CS_PARAM_ALF_TICKET);
		
	}
	
	public String getTicket(HttpSession session) {
		
        final AuthenticatingConnector authenticatingConnector = getAuthenticatingConnector(session, null);
		return getTicket(authenticatingConnector);
		
	}
	
	
	public Connector getConnector(HttpSession session) {
		
		return getConnector(session, null);
		
	}
	
	public Connector getConnector(HttpSession session, String userId) {
		
		Credentials credentials = null;
		UserContext userContext = null;
		
		if (null != session) {
			
			final CredentialVault vault = getCredentialVault(session, userId);
	        if (null != vault) {
	        	
	            credentials = vault.retrieve(APPSTORE_ENDPOINT);
	            
	            if (null == userId) {
	            	userId = getCredentialVaultUserName(vault);
	            }
	            
	        }

	        final ConnectorSession connectorSession = getConnectorSession(session);
	        
	        userContext = null != userId ? new UserContext(userId, credentials, connectorSession) : null;
	        
		}
		

		Connector connector;
		try {
			connector = connectorService.getConnector(APPSTORE_ENDPOINT, userContext, null);
		} catch (ConnectorServiceException e) {
			throw new AppStoreRemoteException(String.format("Cannot get a valid connector for the endpoint '%s'", APPSTORE_ENDPOINT));
		}
		
		fixEndPoint(connector);
		
		return connector;
		
	}
	
	/**
	 * Fixes the endpoint that may contains properties instead of a plain url.
	 * <p>
	 * The problem seems to come from the fact taht {@link RemoteConfigElementReader} does not
	 * use the property resolution (contrarily to {@link GenericConfigElement})
	 * 
	 * @param connector
	 */
	private void fixEndPoint(Connector connector) {
		
		String endPoint = connector.getEndpoint();
		endPoint = resolver.resolveStringValue(endPoint);
		connector.setEndpoint(endPoint);

		EndpointManager.registerEndpoint(endPoint);

	}
	
	String getAppStoreBaseURI() {
		
		final String remoteUrl = "${appstore.protocol}://${appstore.host}:${appstore.port}";
		return resolver.resolveStringValue(remoteUrl);
		
	}
	
    public ConnectorSession getConnectorSession(HttpSession session) {

    	ParameterCheck.mandatory("session", session);
        
        final String key = getSessionEndpointKey(APPSTORE_ENDPOINT);
        ConnectorSession cs = (ConnectorSession) session.getAttribute(key);
        if (cs == null) {
        	
            cs = new ConnectorSession(key);
            session.setAttribute(key, cs);
            
        }
        
        return cs;
        
    }
    
    private static String getSessionEndpointKey(String endpointId) {
    	
        return PREFIX_CONNECTOR_SESSION + endpointId;
        
    }
	
    public String getSessionUser(HttpSession session) {
    	
    	ParameterCheck.mandatory("session", session);
    	
    	CredentialVault vault = getCredentialVault(session, null);
    	if (null == vault) return null;

    	return getCredentialVaultUserName(vault);
    	
    }
    
    public CredentialVault getCredentialVault(HttpSession session, String userId) {
    	
    	ParameterCheck.mandatory("session", session);
    	
        if (null == credentialVaultProvider) {
            throw new NullPointerException("The credential vault provider is incorrect."); 
        }
            
        // session cache binding key
        final String cacheKey = PREFIX_VAULT_SESSION + credentialVaultProvider.generateKey(VAULT_PROVIDER_ID, userId);
        
        // pull the credential vault from session
        CredentialVault vault = (CredentialVault) session.getAttribute(cacheKey);
        
        // if no existing vault, build a new one
        if (null == vault && null != userId) {
        	
            try {
				vault = (CredentialVault) credentialVaultProvider.provide(userId);
			} catch (CredentialVaultProviderException e) {
				throw new AppStoreRemoteException(String.format("Cannot provide the vault for user-id '%s'", userId), e);
			}
            session.setAttribute(cacheKey, vault); // place onto session
            
        }
        
        return vault;
            
    }
    
    private String getCredentialVaultUserName(CredentialVault vault) {

    	final Credentials credentials = vault.retrieve(APPSTORE_ENDPOINT);
    	if (null == credentials) return null;
    	
    	return (String) credentials.getProperty(Credentials.CREDENTIAL_USERNAME);

    }
	
	public void setConnectorService(ConnectorService connectorService) {
		this.connectorService = connectorService;
	}
	
	public void setCredentialVaultProvider(CredentialVaultProvider credentialVaultProvider) {
		this.credentialVaultProvider = credentialVaultProvider;
	}
 	
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

	
}
