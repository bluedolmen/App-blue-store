package org.bluedolmen.alfresco.marketplace.deployer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bluedolmen.marketplace.commons.module.ModuleDescription;

public class DeployerFactory {

	private static final Log logger = LogFactory.getLog(DeployerFactory.class);
	private Map<String, Deployer> cache = new HashMap<String, Deployer>();
	
	public Deployer createDeployer(String packaging) {
		
		if (StringUtils.isBlank(packaging)) {
			throw new IllegalArgumentException("The packaging has to be a valid non-empty string");
		}
		
		if (!cache.containsKey(packaging)) {
			return null;
		}
		
		return cache.get(packaging);
		
	}
	
	public Deployer createDeployer(ModuleDescription moduleDescription) {

		final String packaging = moduleDescription.getPackaging();
		return createDeployer(packaging);
		
	}
	
	void register(Deployer deployer) {
		
		final String packaging = deployer.getPackaging();
		
		if (StringUtils.isBlank(packaging)) {
			throw new IllegalStateException(
				String.format("The deployer '%s' does not define a valid packaging.", deployer.getClass().getName())
			);
		}
		
		if (cache.containsKey(packaging)) {
			logger.warn(
				String.format(
					"The deployer for packaging '%s' is already registered by the deployer '%s'. Overriding!",
					packaging,
					cache.get(packaging).getClass().getName()
				)
			);
		}
		
		cache.put(packaging, deployer);
		
	}
	
}
