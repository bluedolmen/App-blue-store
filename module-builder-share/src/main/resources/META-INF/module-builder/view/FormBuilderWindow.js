Ext.define('ModuleBuilder.view.FormBuilderWindow', {

	extend : 'Ext.window.Window',
	
	requires : [
	    'Ext.tab.Panel',
        'ModuleBuilder.view.ModuleContentBuilderPage',
        'ModuleBuilder.view.ModelBuilderPage',
        'ModuleBuilder.view.AdditionalResourcesBuilderPage'
	],
	
	title : 'Create a new module',
	
	height : 700,
	width : 700,
	
	layout : {
		type : 'vbox',
		align : 'stretch'
	},
	
	initComponent : function() {
		
        this.dockedItems = [
            {
	            xtype: 'toolbar',
	            dock: 'bottom',
	            items: [
	                '->',
	                {
//		                iconCls: 'icon-add',
		                text: 'Generate',
		                scope: this,
		                handler: this.onGenerateClick
		            }
	            ]
            }
        ];
		
		
		this.items = [
		    {
		    	xtype : 'tabpanel',
		    	minHeight: 200,
	            layout: {
	                type: 'vbox',
	                align: 'stretch'
	            },
	            border: false,
	            bodyPadding: 10,
	            
	            defaults : {
	            	border : false,
	            	flex : 1
	            },
	            
	            items: [
	                {
	                	xtype : 'modelbp',
	                	itemId : 'modelBP',
	                	tabConfig : {
	                		title : 'Model'
	                	}
	                },
	                {
	                	xtype : 'modulecontentbp',
	                	itemId : 'moduleContentBP',
	                	tabConfig : {
	                		title : 'Module'
	                	}
	                },
	                {
	                	xtype : 'additionalresourcesbp',
	                	itemId : 'additionalResourcesBP',
	                	tabConfig : {
	                		title : 'Resources'
	                	}
	                }
		        ]
	            
	    	}
		]
		
		this.callParent();
		
	},
	
	getDataModel : function() {
		
		var
			me = this,
			pageIds = ['modelBP', 'moduleContentBP', 'additionalResourcesBP' ], // this may be retrieved automatically		
			dataModel = {}
		;
		
		Ext.Array.forEach(pageIds, function(pageId) {
			
			var
				builderPage = me.queryById(pageId),
				pageDataModel = builderPage.getDataModel()
			;
			
			Ext.apply(dataModel, pageDataModel);
			
		});
		
		return dataModel;
		
	},
	
	setDataModel : function(dataModel) {
		
		var
			me = this,
			pageIds = ['modelBP', 'moduleContentBP', 'additionalResourcesBP' ] // this may be retrieved automatically		
		;
		
		Ext.Array.forEach(pageIds, function(pageId) {
			
			var
				builderPage = me.queryById(pageId)
			;
			
			builderPage.setDataModel(dataModel)
			
		});
		
	},
	
	onGenerateClick : function() {
		
		var 
			me = this,
			dataModel = this.getDataModel(),
			headers = ModuleBuilder.utils.Alfresco.getAlfrescoHeaders(),
			url = '/share/proxy/alfresco/bluedolmen/mps/generate-module'
		;
		
		dataModel['auto-version'] = true;
// in headers?		
//		if (Alfresco.util.CSRFPolicy && Alfresco.util.CSRFPolicy.isFilterEnabled()) {
//		   url += "?" + Alfresco.util.CSRFPolicy.getParameter() + "=" + encodeURIComponent(Alfresco.util.CSRFPolicy.getToken());
//		}
		
		this.setLoading('Generating module...');
		
		Ext.Ajax.request({
			
		    url : url,
		    method : 'POST',
		    headers : headers,
		    jsonData : dataModel,
		    
		    callback : function(options, success, response) {
		    	me.setLoading(false);
		    },
		    
		    success: function(response){
		        
		    	// do nothing yet
		    	Ext.MessageBox.alert("Success", "Generation complete.");
		    	
		    },
		    
		    failure : function(response) {
		    	Ext.Msg.alert("Failed", "Error during generation (see alfresco logs)")
		    }
		
		});
		
	}

});