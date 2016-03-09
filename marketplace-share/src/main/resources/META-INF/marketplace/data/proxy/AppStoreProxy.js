Ext.define('Marketplace.data.proxy.AppStoreProxy', {
	
	extend : 'Ext.data.proxy.JsonP',
	
	requires : [
	    'Marketplace.util.StorageUtils'
    ],
	
	alias : ['proxy.appstore'],
	
	reader : {
		type : 'json'
	},
	
	callbackKey: 'alf_callback',
	
	buildUrl : function(request) {
	
		var url = this.callParent(arguments);
		
		url = StorageUtils.resolveAppStoreProtocol(url);
		url = StorageUtils.authenticatedUrl(url);
		
		return url;
		
	}
	
});