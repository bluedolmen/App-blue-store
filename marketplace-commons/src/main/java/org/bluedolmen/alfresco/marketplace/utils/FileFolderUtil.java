package org.bluedolmen.alfresco.marketplace.utils;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public interface FileFolderUtil {
	
	public NodeRef createPathTarget(NodeRef parentNodeRef, List<String> pathElements);
	
	public NodeRef checkPathExists(NodeRef parentNodeRef, List<String> pathElements);

	public NodeRef getOrCreatePathTarget(
		NodeRef parentNodeRef, 
		List<String> pathElements,
        QName folderTypeQName
    );
	
	public NodeRef getOrCreatePathTarget(
		NodeRef parentNodeRef, 
		List<String> pathElements,
        QName folderTypeQName,
        NodeRef relativeRootNode
    );
	
}
