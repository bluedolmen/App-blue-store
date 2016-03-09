Ext.define('Marketplace.data.proxy.ModuleJsonPProxy', {
	
	extend : 'Marketplace.data.proxy.AppStoreProxy',
	
	alias : ['proxy.jsonpmodule'],
	
	buildUrl: function(request) {
		
		var 
			me      = this,
			url     = me.callParent(arguments),
			id = request.operation ? request.operation.id : null
		;
			
		if (null == id) {
			Ext.Error.raise('Cannot find the id on request operation');
		}
		
		return url.replace(/\{moduleId\}/, id);
		
	}	
	
});
