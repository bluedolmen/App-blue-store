Ext.define('Marketplace.view.ModuleList', {
	
	extend : 'Ext.grid.Panel',
	
	title : 'Modules',
	
	columns : [
		{
			text : 'Description',
			dataIndex : 'moduleId'
		},
		{
			text : 'Actions'
		}
	           
	],
	
	store : Ext.create('Ext.data.ArrayStore', {fields : []})
	
});