package org.bluedolmen.alfresco.marketplace.utils;

import java.util.Arrays;
import java.util.Iterator;

public final class XPathUtils {

	/**
	 * Applies the cm: default namespace to all the path-part on which the
	 * namespace is missing.
	 * 
	 * @param xpathValue
	 * @return
	 */
	public static String getXPathEquivalentPath(String pathToResource) {
		
		final StringBuilder sb = new StringBuilder();
		final String[] splitPath = pathToResource.split("/");
		final Iterator<String> it = Arrays.asList(splitPath).iterator();
		
		while (it.hasNext()) {
			final String pathElement = it.next();
			if (pathElement.isEmpty()) continue;
			
			if (!pathElement.contains(":") && !pathElement.contains("{")) { // weak but sufficient in a first place
				sb.append("cm:");
			}
			sb.append(pathElement);
			if (it.hasNext()) {
				sb.append("/");
			}
		}
		
		return sb.toString();
		
	}
	
	private XPathUtils(){};
	
}
