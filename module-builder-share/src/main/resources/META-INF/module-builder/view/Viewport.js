Ext.define('ModuleBuilder.view.Viewport', {

	extend : 'Ext.container.Viewport',

	requires : [ 
		'Ext.tab.Panel', 
		'Ext.layout.container.Border',
		'ModuleBuilder.view.ContentPanel'
	],

	layout : 'border',

	items : [ 
		{
			region : 'north',
			xtype : 'appHeader'
		}, 
		{
			region : 'west',
			xtype : 'nav',
			width : 180,
			minWidth : 100,
			split : true
		}, 
		{
			region : 'center',
			xtype : 'mbContentPanel'
		},
		{
			region : 'east',
			xtype : 'detailspanel',
			width : 390,
			margin : '0 0 0 5',
			minWidth : 100,
			split : true
		}
	]

});
