Ext.define('Marketplace.view.Viewport', {

	extend : 'Ext.container.Viewport',

	requires : [ 
		'Ext.tab.Panel', 
		'Ext.layout.container.Border' 
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
//			height : 200,
			split : true
		}, 
		{
			region : 'center',
			xtype : 'contentPanel'
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