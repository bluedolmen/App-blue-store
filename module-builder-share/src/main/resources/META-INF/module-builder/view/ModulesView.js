Ext.define('ModuleBuilder.view.ModulesView', {
	
	extend : 'Marketplace.view.ModulesView',
	
	xtype : 'mbModulesview',
	
    extraActionButtons : [
	    {
	     	id : 'load',
	     	iconCls : 'icon-load',
	     	isAvailable : true,
	     	position : 0,
	     	handler : function(record) {
	     		
	         	this.onLoadModule(record);
	     		
	     	}
	    
	    }
    ],
    
    onLoadModule : function(record) {
    	
    	var 
			me = this,
			moduleId = record.get('id'),
			url = Alfresco.constants.PROXY_URI_RELATIVE + 'bluedolmen/mp/load-module/{moduleId}'
				.replace(/\{moduleId\}/, moduleId),
			headers = Marketplace.util.Alfresco.getAlfrescoHeaders()
		;
    	
    	function onSuccess(jsonModel) {
    		var 
				window = Ext.create('ModuleBuilder.view.FormBuilderWindow', {
					modal : true
				})
			;
			
    		window.setDataModel(jsonModel);
			window.show();
    		
    	}
		
		Ext.Ajax.request({
			
		    url : url,
		    method : 'POST',
		    headers : headers,
		    
		    success: function(response){
		    	
			    var 
			    	text = response.responseText || '{}',
			    	jsonResponse = Ext.JSON.decode(text, true /* safe */)
			    ;		    	
		        
		    	onSuccess(jsonResponse);
		    	
		    },
		    
		    failure : ModuleBuilder.utils.Alfresco.messageBoxErrorHandler
		
		});
    	
    }
	
        
});