Ext.define('ModuleBuilder.view.ContentPanel', {
	
	extend : 'Marketplace.view.ContentPanel',
	
	requires : [
	    'ModuleBuilder.view.ModulesView'
	],
	
	xtype : 'mbContentPanel',
	title : '&nbsp;',

	items : [
		{
			xtype : 'mbModulesview',
			itemId : 'modules-view',
			margin : '5px 20px 5px 20px'
		}
	]
	
});
