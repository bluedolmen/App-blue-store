package org.bluedolmen.alfresco.appstore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.extensions.config.xml.XMLConfigService;

public class BeanFactoryPostProcessor implements org.springframework.beans.factory.config.BeanFactoryPostProcessor {

	private static final String WEB_CONFIG_BEAN = "web.config";
	private static final Map<String, Object> WEB_CONFIG_EXTRA_PROPERTIES;
	
	private static final String FORM_UI_GET_BEAN = "webscript.org.alfresco.components.form.form.get";
	private static final Map<String, Object> FORM_UI_GET_EXTRA_PROPERTIES;
	
	static {
		
		WEB_CONFIG_EXTRA_PROPERTIES = new HashMap<String, Object>(2);
		WEB_CONFIG_EXTRA_PROPERTIES.put("connectorService", new RuntimeBeanReference("connector.service"));
		WEB_CONFIG_EXTRA_PROPERTIES.put("deserveGlobalConfig", "false");			
		
		FORM_UI_GET_EXTRA_PROPERTIES = new HashMap<String, Object>(1);
		FORM_UI_GET_EXTRA_PROPERTIES.put("repositoryMessagesHelper", new RuntimeBeanReference("appstore.RepositoryMessagesHelper"));
		
	}
	
	
	private static final Log logger = LogFactory.getLog(BeanFactoryPostProcessor.class);
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		addRequestInterceptor(beanFactory);
		
		replaceChecked(beanFactory, WEB_CONFIG_BEAN, XMLConfigService.class, RepoXMLConfigService.class, WEB_CONFIG_EXTRA_PROPERTIES);
		
		replaceChecked(beanFactory, FORM_UI_GET_BEAN, org.alfresco.web.scripts.forms.FormUIGet.class, org.bluedolmen.alfresco.appstore.FormUIGet.class, FORM_UI_GET_EXTRA_PROPERTIES);
		
	}
	
	private void addRequestInterceptor(ConfigurableListableBeanFactory beanFactory) {
		
		if (!beanFactory.containsBean("webframeworkHandlerMappings")) return;
			
		final BeanDefinition beanDefinition = beanFactory.getBeanDefinition("webframeworkHandlerMappings");
		final MutablePropertyValues pvs = beanDefinition.getPropertyValues();
		final PropertyValue interceptors = pvs.getPropertyValue("interceptors");
		final Object value = interceptors.getValue();
		if (value instanceof List<?>) {
			@SuppressWarnings("unchecked")
			final List<Object> list = (List<Object>) value;
			for (final Object o : list) {
				if (!(o instanceof RuntimeBeanReference)) continue;
				final RuntimeBeanReference runtimeBeanReference = (RuntimeBeanReference) o;
				if ("appstore.configContextInterceptor".equals(runtimeBeanReference.getBeanName())) return;
			}
			list.add(new RuntimeBeanReference("appstore.configContextInterceptor"));
		}		
		
	}
	
	private BeanDefinition replaceChecked(ConfigurableListableBeanFactory beanFactory, String beanName, Class<?> standardClass, Class<?> replacingClass, Map<String, Object> extraProperties) {

		if (!beanFactory.containsBeanDefinition(beanName)) return null;
		
		final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
		
		if (!standardClass.getCanonicalName().equals(beanDefinition.getBeanClassName())) {
			
			logger.warn(
				String.format(
						"Bean with name '%s' does not match the expected target class '%s', skipping!", 
						beanName, 
						standardClass.getCanonicalName()
				)
			);
			return null;
			
		}
		
		beanDefinition.setBeanClassName(replacingClass.getCanonicalName());
		if (null != extraProperties) {
			final MutablePropertyValues pvs = beanDefinition.getPropertyValues();
			for (final String propertyName : extraProperties.keySet()) {
				pvs.addPropertyValue(propertyName, extraProperties.get(propertyName));
			}
		}

		return beanDefinition;
		
	}

}
