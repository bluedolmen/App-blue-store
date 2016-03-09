Ext.define('Marketplace.model.ModuleDetails', {
	
	extend : 'Ext.data.Model',
	
	requires : ['Marketplace.data.proxy.ModuleJsonPProxy'],

	idProperty : 'id',
	
	fields : [
		'id',
		'tags',
		'author',
		'nodeRef',
		'groupId',
		'moduleId',
		'packaging',
		{
			name : 'description',
			convert : function(value, record) {
				return value.replace(/\\n/g,'\n');
			}
		},
		'name',
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
		'rating',
		'cost',
		'category',
		{
			name : "screenshots",
			convert : function(value, record) {
				return Ext.Array.map(value || [], function(value) {
					value.url = StorageUtils.APPSTORE_PROTOCOL + value.url;
					return value;
				});
			}
		},
		'comments',
		'latestVersion'
	],
	
	proxy : {
		
		type : 'jsonpmodule',
		url : 'appstore://bluedolmen/mps/{moduleId}/details'

	}
	
});