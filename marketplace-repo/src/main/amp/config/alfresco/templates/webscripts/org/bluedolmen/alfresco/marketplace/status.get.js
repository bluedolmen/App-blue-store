///<import resource="classpath:/alfresco/templates/webscripts/org/bluedolmen/alfresco/actions/parseargs.lib.js">
///<import resource="classpath:/alfresco/extension/bluedolmen/utils/utils.lib.js">

(function() {
	
//	function buildModel(nodes) {
//		
//		var 
//			modules = Utils.Array.map(nodes, function(node) {
//				
//				return ({
//					id : node.properties[MKP.get('PROP_FULLID')],
//					version : node.properties[MKP.get('PROP_VERSION')],
//					installedOn : node.properties[MKP.get('PROP_INSTALLED_ON')],
//					installedBy : node.properties[MKP.get('PROP_INSTALLED_BY')]
//				});
//			
//			})
//		;
//		
//		model.modules = modules;
//		
//	}
//	
//	var
//		MKP = Utils.Object.getJavaConstantsClassAdapter('org.bluedolmen.alfresco.marketplace.model.MarketPlaceModel'),
//		installedModuleNodes = AppStoreService.getInstalledModuleNodes()
//	;
//	
//	buildModel(installedModuleNodes);
	
	model.status = {
		"MAIN_URL" : AppStoreService.getAppStoreUrl(),
		"browseOnly" : AppStoreService.isBrowseOnly()
	}

})();