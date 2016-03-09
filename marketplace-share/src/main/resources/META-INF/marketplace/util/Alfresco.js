Ext.define('Marketplace.util.Alfresco', {
	
	singleton : true,
	
	// TODO: This should be mutualized
	getAlfrescoHeaders : function() {
		
		var 
			headers = {}
		;
		
		// Manage CSRF policy for Alfresco 4.1+
		if (Alfresco.util.CSRFPolicy && Alfresco.util.CSRFPolicy.isFilterEnabled()) {
		   headers[Alfresco.util.CSRFPolicy.getHeader()] = Alfresco.util.CSRFPolicy.getToken();
		}
		
		return headers;
		
	}
	
});