Ext.define('Marketplace.view.ContentPanel', {
	
	extend : 'Ext.panel.Panel',
	
	requires : [
		'Marketplace.view.ModulesView'
	],
	
	xtype : 'contentPanel',
	id : 'content-panel',
	title : '&nbsp;',

	autoScroll : true,
//	overflowY : 'scroll',
	
	layout : {
		type : 'vbox',
		align : 'stretch'
	},
	
	items : [
		{
			xtype : 'modulesview',
			itemId : 'modules-view',
			margin : '5px 20px 5px 20px'
		}
	],
	
	initComponent : function() {
		
		this.tools = [{
			type : 'refresh',
			tooltip : 'Refresh',
			handler : this.refreshStore,
			scope : this
		}]; 
		
		this.callParent(arguments);
	},
	
	refreshStore : function() {
		
		var 
			me = this,
			store = null
		;
		if (null == me.modulesView) {
			me.modulesView = me.queryById('modules-view');
		}
		
		store = me.modulesView.store;
		if (!store) return;
		
		store.load();
		
	}
	
});
