package org.bluedolmen.alfresco.marketplace;

import java.util.List;

import org.bluedolmen.marketplace.commons.module.ModuleDescription;
import org.bluedolmen.marketplace.commons.module.Requirement;

public class RequirementsManager {

	public void checkRequirements(ModuleDescription pluginDescription) {
		
		final List<Requirement> requirements = pluginDescription.getRequires();
		
		for (final Requirement requirement : requirements){
			
		}
	}
	
}
