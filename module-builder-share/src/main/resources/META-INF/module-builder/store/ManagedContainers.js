Ext.define('ModuleBuilder.store.ManagedContainers', {
	
	extend : 'Ext.data.Store',
	
    fields:[
        {
        	name : 'namespace',
        	convert : function(value, record) {
        		return record.raw;
        	}
        }
	],
	
    proxy: {
        type: 'ajax',
        url: '/share/proxy/alfresco/bluedolmen/mps/managed-containers',
        reader: {
            type: 'json',
            root: 'managedContainers'
        }
    }

});