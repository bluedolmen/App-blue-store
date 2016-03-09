Ext.define('ModuleBuilder.view.ModuleContentBuilderPage', {

	extend : 'Ext.form.Panel',
	
	requires : [
	    'ModuleBuilder.view.AttachedResourceUploader'
	],
	
	xtype : 'modulecontentbp',
	
    fieldDefaults: {
        labelAlign: 'top',
        labelWidth: 100,
        labelStyle: 'font-weight:bold'
    },
	
	initComponent : function() {
		
		var 
			tagsStore = Ext.create('Ext.data.ArrayStore', {
			    fields: ['name'],
			    data : []
			})
		;
		
		Ext.Ajax.request({
		    url: '/share/proxy/alfresco/api/tags/workspace/SpacesStore',
		    success: function(response){
		    	
		        var 
		        	text = response.responseText,
		        	tags = Ext.JSON.decode(text, true /* safe */),
		        	filteredTags = []
		        ;
		        
		        if (!Ext.isArray(tags)) return;
		        
		        filteredTags = Ext.Array.filter(tags, function removeSharpedTags(tag) {
		        	return !Ext.String.startsWith(tag, '#');
		        });
		        
		        if (Ext.isEmpty(tags)) return;
		        
		        tagsStore.loadData(filteredTags);
		        
		    }
		});		
		
		this.items = [
		              
			{
			    xtype: 'fieldcontainer',
			    fieldLabel: 'Module Identification',
			    defaultType: 'textfield',
			
			    fieldDefaults: {
			        labelAlign: 'top'
			    },
			    
        		labelWidth: 89,
        		anchor: '100%',
        		layout: {
        			type: 'hbox',
        			defaultMargins: {top: 0, right: 5, bottom: 0, left: 0}
        		},
			
			    items: [
			        {
			            width: 60,
			            name: 'version',
			            itemId: 'version',
			            fieldLabel: 'Version',
			            value: '1.0'
			        },
			        {
			            flex: 1,
			            name: 'groupId',
			            fieldLabel: 'Group Id',
			            xtype : 'combo',
			            store : Ext.create('ModuleBuilder.store.ManagedContainers'),
			            allowBlank : false,
			            value : 'org.bluedolmen',
			            displayField : 'namespace',
			            valueField : 'namespace',
			            margins: '0 0 0 5'
			        },
			        {
			            flex: 1,
			            name: 'moduleId',
			            fieldLabel: 'Module Id',
			            margins: '0 0 0 5'
			        }
			    ]
			},
   	        {
   	        	xtype: 'textareafield',
   	        	flex: 2,
   	        	name: 'description',
   	        	fieldLabel: 'Description',
   	        	margins: '0 0 0 5',
   	        	width : '100%'
   	        },

			{
   	        	xtype: 'fieldcontainer',
   	        	fieldLabel: 'Classification',
   	        	defaultType: 'textfield',

   	        	fieldDefaults: {
   	        		labelAlign: 'top'
   	        	},

        		labelWidth: 89,
        		anchor: '100%',
        		layout: {
        			type: 'hbox',
        			defaultMargins: {top: 0, right: 5, bottom: 0, left: 0}
        		},
        		
   	        	items: [
   	        	    {
        	        	flex: 2,
        	        	xtype : 'combo',
        	        	name: 'tags',
        	        	fieldLabel: 'Tags',
        	        	store: tagsStore,
        	        	queryMode: 'local',
        	        	displayField: 'name',
        	        	valueField: 'name',
        	        	multiSelect: true
        	        },
        	        {
        	        	flex: 1,
        	        	name: 'category',
        	        	itemId: 'category',
        	        	fieldLabel: 'Category',
        	        	margins: '0 0 0 5',
        	        	value: '/Business Model',
        	        	disabled: true
        	        }
   	        	]
			},
			
			{
				xtype: 'attachedresourceupload',
				title : 'Logo',
				kind : 'logo',
				replace : true,
				previewWidth : 128,
				displayName : false,
				height : 200,
				padding : '10 0 5 0'
			}

	 	         
	    ];
		
		this.callParent();
		
	},
	
	getDataModel : function() {
		
		var
			innerForm = this.getForm()
		;
		
		return innerForm ? innerForm.getFieldValues() : {};
			
		
	},
	
	setDataModel : function(model) {
		
		if (!model) return;
		
		var innerForm = this.getForm();
		if (!innerForm) return;
		
		innerForm.setValues(model);
		
	}
	
	
});
