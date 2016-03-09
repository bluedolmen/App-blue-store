Ext.define('Marketplace.view.Header', {
	
	extend : 'Ext.Container',
	
	requires : [
//		'Marketplace.view.MainSearchCombo',
		'Ext.toolbar.Spacer',
		'Marketplace.view.MainFilterBox',
		'Marketplace.view.UserButtonMenu'
	],
	
	xtype : 'appHeader',
	id : 'app-header',
	
	height : 52,
	
	layout : {
		type : 'hbox',
		align : 'middle'
	},
	
	initComponent : function() {
		
		this.items = [ 
			{
				xtype : 'component',
				id : 'app-header-title',
				html : '<b>蓝</b> Blue AppStore'
//				flex : 1
			},
			{ xtype: 'tbspacer', flex : 1 },
//			'->',
//			{
//				xtype : 'mainsearchcombo',
//				id : 'app-header-combo',
//				width : 400
//			},
			{
				xtype : 'mainfilterbox',
				id : 'app-header-filter',
				width : 400
			},
			{ xtype: 'tbspacer', flex : 1 },
			{
				xtype : 'userbuttonmenu',
				margin: '0 10 0 0'
			}
//			'->'
		];

		this.callParent();
		
	}
	
});
