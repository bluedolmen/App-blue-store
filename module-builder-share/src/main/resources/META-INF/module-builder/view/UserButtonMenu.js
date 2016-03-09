Ext.define('ModuleBuilder.view.UserButtonMenu', {
	
	extend : 'Ext.button.Button',
	xtype : 'userbuttonmenu',
	
	requires : [
	    'ModuleBuilder.view.NamespaceManagementDialog'
	],
	
	componentCls : 'userbuttonmenu',
	
	scale : 'large',
	text : i18n.t('mb:view.user-button.label'),
	
	iconCls : 'icon-user',
	
	menu : [
	   {
		   xtype : 'button',
		   text : 'Manage Namespaces',
		   handler : function() {
			   
			   var win = Ext.create('ModuleBuilder.view.NamespaceManagementDialog');
			   win.show();
			   
		   }
	   } 
	]
	
});