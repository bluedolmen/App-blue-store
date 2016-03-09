package org.bluedolmen.alfresco.marketplace.modulebuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.extensions.webscripts.WebScriptSession;

public class AttachedResourceUtils {

	public static AttachedResource getFirstSessionResource(WebScriptSession session, String classification) {
		
		final List<AttachedResource> resources = getSessionResources(session, classification);
		if (resources.isEmpty()) return null;
		
		return resources.get(0);
		
	}
	
	public static AttachedResource getSessionResourceByName(WebScriptSession session, String classification, String name) {

		if (null == name) {
			throw new IllegalArgumentException("The provided name has to be a non-null string");
		}

		if (StringUtils.isBlank(classification)) {
			classification = "default";
		}

		final Map<String, List<AttachedResource>> resourcesByKind = getSessionResources(session);
		if (!resourcesByKind.containsKey(classification)) return null;
		
		final List<AttachedResource> resources = resourcesByKind.get(classification);
		
		for (AttachedResource resource : resources) {
			if (name.equals(resource.fileName)) {
				return resource;
			}			
		}
		
		return null;

	}
	
	public static List<AttachedResource> getSessionResources(WebScriptSession session, String classification) {
		
		if (StringUtils.isBlank(classification)) {
			classification = "default";
		}
		
		final Map<String, List<AttachedResource>> resourcesByKind = getSessionResources(session);
		if (!resourcesByKind.containsKey(classification)) {
			return Collections.emptyList();
		}
		
		return resourcesByKind.get(classification);
		
	}
	
	public static List<AttachedResource> saveSessionContent(WebScriptSession session, String classification, String name, InputStream input, boolean replace) throws IOException {
		
		final String suffix = FilenameUtils.getExtension(name);
		final File tempFile = TempFileProvider.createTempFile(session.getId(), "." + suffix);
		
		FileOutputStream output = null;
		try {
			output = new FileOutputStream(tempFile);
			IOUtils.copy(input, output);
		} 
		finally {
			IOUtils.closeQuietly(input);
			IOUtils.closeQuietly(output);
		}
		
		return setSessionResource(session, tempFile, classification, name, replace);
		
	}
	
	public static List<AttachedResource> setSessionResource(WebScriptSession session, File file, String classification, String name, boolean replace) throws IOException {
		
		if (StringUtils.isBlank(classification)) {
			classification = "default";
		}
		
		if (StringUtils.isBlank(name)) {
			name = file.getName();
		}
		
		final Map<String, List<AttachedResource>> resourcesByKind = getSessionResources(session);
		if (!resourcesByKind.containsKey(classification)) {
			resourcesByKind.put(classification, new ArrayList<AttachedResource>());
		}
		
		final List<AttachedResource> resources = resourcesByKind.get(classification);
		if (replace) {
			resources.clear();
		}
		final AttachedResource attachedResource = new AttachedResource(file, name, classification);
		resources.add(attachedResource);
		
		return resources;
		
	}
	
	public static void deleteSessionContent(WebScriptSession session, String classification, String name) {
		
		if (null == name) {
			throw new IllegalArgumentException("The provided name has to be a non-null string");
		}

		if (StringUtils.isBlank(classification)) {
			classification = "default";
		}

		final Map<String, List<AttachedResource>> resourcesByKind = getSessionResources(session);
		if (!resourcesByKind.containsKey(classification)) return;
		
		final List<AttachedResource> resources = resourcesByKind.get(classification);
		final Iterator<AttachedResource> iterator = resources.iterator();
		
		while (iterator.hasNext()) {
			
			final AttachedResource resource = iterator.next();
			if (name.equals(resource.fileName)) {
				FileUtils.deleteQuietly(resource.file);
				iterator.remove();
				break;
			}
			
		}
		
	}
	
	public static final class AttachedResource {
		
		public final File file;
		public final String fileName;
		public final String classification;
		
		private AttachedResource(File file, String fileName, String classification) {
			this.file = file;
			this.fileName = fileName;
			this.classification = classification;
		}
		
	}
	
	public static Map<String, List<AttachedResource>> getSessionResources(WebScriptSession session) {
		
		@SuppressWarnings("unchecked")
		Map<String, List<AttachedResource>> resources = (Map<String, List<AttachedResource>>) session.getValue("resources");
		if (null == resources) {
			resources = new HashMap<String, List<AttachedResource>>(1);
			session.setValue("resources", resources);
		}
		
		return resources;
		
	}
	
	
	
	private AttachedResourceUtils(){}; // Meant to be used statically
	
}
