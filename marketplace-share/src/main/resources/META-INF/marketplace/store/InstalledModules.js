Ext.define('Marketplace.store.InstalledModules', {
	
	extend : 'Ext.data.Store',
	
	requires : [
		'Ext.data.proxy.Ajax'
	],
	
	storeId : 'installedModules',
	
	fields : [
		"id",
	    "installedOn",
		"installedBy",
		"version"
	],
	
	proxy : {
		type : 'ajax',
		url : '/share/proxy/alfresco/bluedolmen/mp/installed-modules',
		reader : {
			type : 'json',
			root : 'modules'
		}
	},
	
	listeners : {
		
		'beforeload' : function(store, operation, eOpts) {
			if (AppStore.browseOnly) return false; // stop loading if in browse-only mode
		}
	
	},
	
	isInstalled : function(moduleId) {
		
		return -1 != store.find("id", moduleId);
		
	}
	
});