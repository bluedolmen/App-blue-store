Ext.define('ModuleBuilder.view.ExtraResourceUploader', {
	
	extend : 'ModuleBuilder.view.AttachedResourceUploader', 
	
	xtype : 'extraresourceupload',
	kind : 'extra',
	editable : true,
	
	remoteStore : null,
	
	initComponent : function() {
		
		this.remoteStore = this.getStore();
		this.store = null; 
		// The call to the parent will create a new instance that will be used locally
		// This is used to be able to use a local store with extra column which data are saved locally
		
		this.callParent();
		
		this.remoteStore.on('load', this.onRemoteStoreLoad, this);
		
	},
	
	/**
	 * @protected
	 */
	getStoreFieldsDefinition : function() {
		
		return [
		    {name : 'name', type : 'string'},
		    {name : 'mapping', type : 'string'},
		    {name : 'size', type : 'int'}
		];
		
	},
	
	/**
	 * @protected
	 */
	getGridColumnsDefinition : function() {
		
		var 
			columns = this.callParent(),
			mappingColumnDefinition = {
				text : 'mapping',
				dataIndex : 'mapping',
				width : 250,
                editor: {
                	xtype : 'textfield',
                	value : ''
                }
			}
		;
		columns.splice(1, 0, mappingColumnDefinition);
		
		return columns;
		
	},

	refreshAttachedResources : function() {
		
		this.remoteStore.reload();		
		
	},
	

	/**
	 * This not really elegant solution maintains a local copy of the data
	 * not synchronized with the remote store, here only the mapping field
	 */
	onRemoteStoreLoad : function(store, records, successful) {
		
		if (true !== successful) return;
		
		var 
			currentValues = this.store.data
		;
		
		Ext.Array.forEach(records, function(record) {
			
			var
				name = record.get('name'),
				locRecord = currentValues.getByKey(name)
			;
			
			if (null == locRecord) return;
			
			record.set('mapping', locRecord.get('mapping'))
			
		})
		
		this.store.loadRecords(records);
		
	},
	
    getDataModel : function() {
    	
    	var
    		store = this.getStore(),
    		records = store.getRange()
    	;
    	
    	return ({
    		
    		resources : Ext.Array.map(records, function(record) {
    			return record.getData();
    		})
    		
    	});
    	
    },
    
    setDataModel : function(dataModel) {
    	
    	if (dataModel.resources) {
    		this.store.loadData(dataModel.resources);
    	}
    	
    }
	
});