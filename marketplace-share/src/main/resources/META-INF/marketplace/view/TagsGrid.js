Ext.define('Marketplace.view.TagsGrid', {
	
	extend : 'Ext.grid.Panel',
	
	title : 'Tags',
	xtype : 'tagsgrid',
	
	hideHeaders : true,
	store : Ext.create('Ext.data.ArrayStore', {fields : []}),
	
	columns : [
		{
			text : 'Name',
			dataIndex : 'name',
			flex : 1
		},
		{
			text : 'Count',
			dataIndex : 'count',
			width : 30
		}					
	],
	
	initComponent : function() {
		
		this.callParent(arguments);
	}
});