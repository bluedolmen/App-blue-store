package org.bluedolmen.alfresco.marketplace.model;

import org.alfresco.service.namespace.QName;

public interface MarketPlaceModel {

	public static final String MARKETPLACE_PREFIX = "mkp";
	public static final String MARKETPLACE_NAMESPACE_URI = "http://www.bluedolmen.org/model/marketplace/1.0";
	
	public static final QName ASPECT_MODULE_DESCRIPTION = QName.createQName(MARKETPLACE_NAMESPACE_URI, "moduleDescription");
	public static final QName PROP_NAME = QName.createQName(MARKETPLACE_NAMESPACE_URI, "name");
	public static final QName PROP_GROUPID = QName.createQName(MARKETPLACE_NAMESPACE_URI, "groupid");
	public static final QName PROP_ID = QName.createQName(MARKETPLACE_NAMESPACE_URI, "id");
	public static final QName PROP_FULLID = QName.createQName(MARKETPLACE_NAMESPACE_URI, "fullid");	
	public static final QName PROP_PACKAGING = QName.createQName(MARKETPLACE_NAMESPACE_URI, "packaging");
	public static final QName PROP_AUTHOR = QName.createQName(MARKETPLACE_NAMESPACE_URI, "author");
	public static final QName PROP_COST = QName.createQName(MARKETPLACE_NAMESPACE_URI, "cost");
	public static final QName PROP_CATEGORY = QName.createQName(MARKETPLACE_NAMESPACE_URI, "category"); // temporary
	public static final QName PROP_RATING = QName.createQName(MARKETPLACE_NAMESPACE_URI, "rating"); // rating
	
	public static final QName ASSOC_THUMBNAIL = QName.createQName(MARKETPLACE_NAMESPACE_URI, "thumbnail");
	public static final QName ASSOC_SCREENSHOT = QName.createQName(MARKETPLACE_NAMESPACE_URI, "screenshot");
	public static final QName ASSOC_PRESENTATION = QName.createQName(MARKETPLACE_NAMESPACE_URI, "presentation");
	
	public static final QName ASPECT_MODULE = QName.createQName(MARKETPLACE_NAMESPACE_URI, "module");
	public static final QName PROP_VERSION = QName.createQName(MARKETPLACE_NAMESPACE_URI, "version");
	public static final QName PROP_DEFINITION = QName.createQName(MARKETPLACE_NAMESPACE_URI, "definition");
	
	public static final String MARKETPLACE_CLIENT_PREFIX = "mkpc";
	public static final String MARKETPLACE_CLIENT_NAMESPACE_URI = "http://www.bluedolmen.org/model/marketplace/client/1.0";

	public static final QName ASPECT_INSTALLED = QName.createQName(MARKETPLACE_CLIENT_NAMESPACE_URI, "installed");
	public static final QName PROP_INSTALLED_BY = QName.createQName(MARKETPLACE_CLIENT_NAMESPACE_URI, "installedBy");
	public static final QName PROP_INSTALLED_ON = QName.createQName(MARKETPLACE_CLIENT_NAMESPACE_URI, "installedOn");
	public static final QName ASSOC_INSTALLATION_DETAILS = QName.createQName(MARKETPLACE_CLIENT_NAMESPACE_URI, "installationDetails");
	
}
