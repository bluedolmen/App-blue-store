package org.bluedolmen.alfresco.appstore;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.config.ConfigService;
import org.springframework.extensions.surf.mvc.AbstractWebFrameworkInterceptor;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;

public class ConfigContextInterceptor extends AbstractWebFrameworkInterceptor {
	
	private static final Log logger = LogFactory.getLog(ConfigContextInterceptor.class);
	private RepoXMLConfigService configService;
    
    /* (non-Javadoc)
     * @see org.springframework.web.context.request.WebRequestInterceptor#preHandle(org.springframework.web.context.request.WebRequest)
     */
    public void preHandle(WebRequest request) throws Exception {
    	
    }

    /* (non-Javadoc)
     * @see org.springframework.web.context.request.WebRequestInterceptor#postHandle(org.springframework.web.context.request.WebRequest, org.springframework.ui.ModelMap)
     */
    public void postHandle(WebRequest request, ModelMap model) throws Exception {
    	
    }

    /* (non-Javadoc)
     * @see org.springframework.web.context.request.WebRequestInterceptor#afterCompletion(org.springframework.web.context.request.WebRequest, java.lang.Exception)
     */
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {
    	
    	if (null == configService) return;
    	
    	configService.releaseLocalConfig();
    	
    }
    
    public void setConfigService(ConfigService configService) {
    	
    	if (!(configService instanceof RepoXMLConfigService)) {
    		logger.warn("Config Service is not an " + RepoXMLConfigService.class.getCanonicalName() + ", the interceptor will be inactivated");
    		return;
    	}
    	
    	this.configService = (RepoXMLConfigService) configService;
    	
    }
    
    
}