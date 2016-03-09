Ext.define("Marketplace.view.Navigation", {
	
	extend : "Ext.panel.Panel",
	
	requires : [
		'Ext.grid.Panel',
		'Ext.data.ArrayStore'
	],
	
	xtype : 'nav',
	
	stateful : true,
	stateId : 'mainnav.west',
	collapsible : true,
	
//	tools : [ {
//		type : 'gear',
//		regionTool : true
//	} ],
	
	layout : {
		type : 'vbox',
		align : 'stretch'
	},
	
	defaults : {
		margin : 3
	},
	
	initComponent : function() {
		
		var 
			store = Ext.data.StoreManager.lookup("Categories")
		;
		
		this.items = [
			{
				title : 'Categories',
				xtype : 'grid',
//				header : false,
				hideHeaders : true,
				store : store,
				columns : [
					{
						text : 'Name',
						dataIndex : 'id',
						flex : 1
					}
				]
			},
			{
				title : 'Tags',
				xtype : 'grid',
				itemId : 'tags-grid',
				hideHeaders : true,
				maxHeight : 300,
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
				selModel : {
					allowDeselect : true,
					mode : 'MULTI'
				}
			},
			{
				title : 'Privacy',
				xtype : 'grid',
				itemId : 'privacy-grid',
				hideHeaders : true,
				store : Ext.create('Ext.data.JsonStore', {
					fields : ["name"],
					data : [
					    { name : 'PUBLIC' },
					    { name : 'PRIVATE' }			    
					]
				}),
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
				selModel : {
					allowDeselect : true,
					mode : 'SINGLE'
				}
			}
		];
		
		this.callParent(arguments);
		
	}
	
});