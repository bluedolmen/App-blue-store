Ext.define('ModuleBuilder.view.AdditionalResourcesBuilderPage', {
	
	extend : 'Ext.form.Panel',
	
	xtype : 'additionalresourcesbp',
	
	requires : [
	    'ModuleBuilder.view.FileMappingBuilder',
	    'ModuleBuilder.view.ExtraResourceUploader'
	],
	
	title : 'Extra Resources',
	
    fieldDefaults: {
        labelWidth: 100,
        labelStyle: 'font-weight:bold'
    },
    
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
	
	initComponent : function() {
		
		this.items = [		 	    
	 	    {
	 	    	xtype : 'filemappingbuilder',
	 	    	itemId : 'fileMappingBuilder',
	 	    	title : 'File Mapping',
	 	    	height : 180
	 	    },
	 	    {
	 	    	xtype : 'extraresourceupload',
	 	    	title : 'Extra Resources',
	 	    	itemId : 'extraResourceUpload',
	 	    	title : 'Resources Mapping',
	 	    	flex : 1,
	 	    	padding : '10 0 0 0'
	 	    }
	 	];
		
		this.callParent();
		
	},
	
	getDataModel : function() {
		
		var
			fileMappingBuilder = this.queryById('fileMappingBuilder'),
			extraResourceUpload = this.queryById('extraResourceUpload'),
			dataModel = {}
		;
		
		Ext.apply(dataModel, fileMappingBuilder.getDataModel());
		Ext.apply(dataModel, extraResourceUpload.getDataModel());
		
		return dataModel;
		
	},
	
	setDataModel : function(dataModel) {
		
		var
			fileMappingBuilder = this.queryById('fileMappingBuilder'),
			extraResourceUpload = this.queryById('extraResourceUpload')
		;
		
		fileMappingBuilder.setDataModel(dataModel);
		extraResourceUpload.setDataModel(dataModel);
		
	}
	
});