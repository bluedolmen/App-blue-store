Ext.define('Marketplace.store.Modules', {
	
	extend : 'Ext.data.Store',
	
	requires : [
	    'Marketplace.data.proxy.AppStoreProxy'
	],
	
	storeId : 'Modules',
	
	fields : [
		"name",
	    "fullId",
		"groupId",
		"moduleId",
		"description",
		"packaging",
		{ 
			name : "downloadUrl",
			convert : function(value, record) {
				return StorageUtils.APPSTORE_PROTOCOL + value;
			}
		},
		{ 
			name : "logoUrl",
			convert : function(value, record) {
				return StorageUtils.APPSTORE_PROTOCOL + value;
			}
		},
		"rating",
		"author",
		"cost",
		"tags",
		{ name : "createdOn", type : 'date' },
		{ name : "modifiedOn", type : 'date' },
		{ name : "isPublic", type : 'boolean' }
	],
	
	remoteFilter : false, // temporary
	remoteGroup : false, // temporary
	remoteSort : false, // temporary
	
	sortOnLoad : true,
	
	sorters : [
		{
			property : 'name'
		}
	],
	
	proxy : {
		type : 'appstore',
		url : 'appstore://bluedolmen/mps/list',
		reader : {
			type : 'json',
			root : 'modules'
		}
	}
	
});