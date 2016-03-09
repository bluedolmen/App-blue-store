Ext.define('ModuleBuilder.utils.Alfresco', {
	
	singleton : true,
	
	getAlfrescoHeaders : function() {
		
		var 
			headers = {}
		;
		
		// Manage CSRF policy for Alfresco 4.1+
		if (Alfresco.util.CSRFPolicy && Alfresco.util.CSRFPolicy.isFilterEnabled()) {
		   headers[Alfresco.util.CSRFPolicy.getHeader()] = Alfresco.util.CSRFPolicy.getToken();
		}
		
		return headers;
		
	},
	
	messageBoxErrorHandler : function(response) {
		
		var
			message = '',
			error = {}
		;
		
		if (response && response.responseText) {
			
			error = Ext.JSON.decode(response.responseText);
			
		}
		
		if (error && error.status) {
			
			message = error.status.description;
			
		}
		
        Ext.MessageBox.show({
            title: 'Error',
            msg: message,
            buttons: Ext.MessageBox.OK,
            icon: Ext.MessageBox.ERROR
        });
		
	}
	
});