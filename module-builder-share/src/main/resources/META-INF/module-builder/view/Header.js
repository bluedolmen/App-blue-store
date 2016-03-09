Ext.define('ModuleBuilder.view.Header', {
	
	extend : 'Ext.Container',
	
	requires : [
		'Ext.toolbar.Spacer',
		'ModuleBuilder.view.UserButtonMenu'
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
				html : '<b>蓝</b> Blue AppStore - Module Editor'
//				flex : 1
			},
			{ xtype: 'tbspacer', flex : 1 },
//			'->',
//			{
//				xtype : 'mainsearchcombo',
//				id : 'app-header-combo',
//				width : 400
//			},
//			{
//				xtype : 'mainfilterbox',
//				id : 'app-header-filter',
//				width : 400
//			},
//			{ xtype: 'tbspacer', flex : 1 },
			{
				xtype : 'button',
				scale : 'large',
				id : 'new-module-button',
				iconCls : 'icon-new-module',
				text : i18n.t('mb:view.new-button.label'),
				margin : '0 10 0 0'
			},
			{
				xtype : 'userbuttonmenu',
				margin: '0 10 0 0'
			}
//			'->'
		];

		this.callParent();
		
	}
	
});
