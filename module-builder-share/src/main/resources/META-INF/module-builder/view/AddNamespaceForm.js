Ext.define('ModuleBuilder.view.AddNamespaceForm', {

	extend : 'Ext.form.FormPanel',
	
	xtype : 'addnamespaceform',
	
	requires : [
	],
	
	title: 'Add a namespace',
    bodyPadding: 5,
    frame: true,
    width: 340,
    defaultType: 'textfield',
    
    initComponent: function(){
    	
    	var
    		me = this
    	;
    	
        Ext.apply(this, {
        	
            fieldDefaults: {
                labelWidth: 110,
                anchor: '100%'
            },
            items: [
                {
	                fieldLabel: 'Username',
	                name: 'username',
	                value: this.userName || '',
	                allowBlank: false
	            },
	            {
	                fieldLabel: 'Namespace',
	                name: 'namespace',
	                value: this.namespace || 'org.bluedolmen' + (this.userName ? '.' + this.userName : ''),
	                allowBlank: false
	            },
	            {
	            	fieldLabel: 'Visibility',
	            	name: 'visibility',
	            	xtype : 'combobox',
	            	displayField: 'name',
                    valueField: 'value',
                    queryMode: 'local',
	            	store: new Ext.data.ArrayStore({
                        fields: ['name', 'value'],
                        data: [
                           ['Private', 'PRIVATE'],
                           ['Public', 'PUBLIC'],
                           ['Opened', 'OPENED']
                        ]
                    }),
                    forceSelection : true,
                    value : 'PUBLIC',
                    allowBlank : false
	            }
	        ]
        });
        
        this.buttons = [
	        {
	  	        text: 'Add',
	  	        disabled: true,
	  	        formBind: true,
	  	        handler : function() {
	  	        	me.handleAdd();
	  	        }
	        }
//	        {
//	        	text: 'Cancel',
//	        	handler : function() {
//	        		me.close();
//	        	}
//	        }
	    ];
        
        this.callParent();
        
    },
    
    handleAdd : function() {
    	
    	var
    		me = this,
    		values = this.getValues(),
    		userName = values.username,
    		groupId = values.namespace,
    		visibility = values.visibility,
			headers = ModuleBuilder.utils.Alfresco.getAlfrescoHeaders()
    	;
    	
		Ext.Ajax.request({
			
		    url : '/share/proxy/alfresco/bluedolmen/mps/namespace-permission/' + groupId,
		    method : 'POST',
		    headers : headers,
		    jsonData : {
		    	userName : userName,
		    	visibility : visibility
		    },
		    
		    success: function(response){
		        
		    	me.onSuccess();
		    	
		    },
		    
		    failure : ModuleBuilder.utils.Alfresco.messageBoxErrorHandler
		
		});
    	
    },
    
    onSuccess : function() {
    	
    }
    
});

