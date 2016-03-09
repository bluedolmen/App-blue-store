Ext.define('ModuleBuilder.view.NamespaceManagementDialog', {

	extend : 'Ext.window.Window',
	alias : 'widget.namespacemanagementdialog',
	
	requires : [
		'Ext.grid.plugin.RowEditing',
	    'ModuleBuilder.view.AddNamespaceForm'
	],
	
	title : 'Manage Namespace Attribution',
	height : 500,
	width : 500,
	modal : true,
	
	layout : 'border',
	
	defaults : {
		height : '100%',
		border : 1,
		flex : 1,
		margin : 5
	},
	
	renderTo : Ext.getBody(),
	
	initComponent : function() {
		
		var
		
			me = this,
			
			usersStore = Ext.create('Ext.data.Store', {
			    fields:[
			        'userName',
					'firstName',
					'lastName',
					'email',
					{
			        	name : 'displayName',
			        	convert : function(value, record) {
			        		
			        		if (!record) return "";
			        		
			        		return Ext.String.trim(
			        			(record.get('firstName') || "")
			        		  + " "
			        		  + (record.get('lastName')  || "")
			        		); 
			        		
			        	}
					}
				],
			    proxy: {
			        type: 'ajax',
			        url: '/share/proxy/alfresco/api/people',
			        reader: {
			            type: 'json',
			            root: 'people'
			        }
			    }
			    
	 		}),
	 		
	 		namespacesStore = Ext.create('ModuleBuilder.store.ManagedContainers')
	 		
		;
		
		this.namespacesGrid = Ext.create('Ext.grid.Panel', {
			
		    title: 'Namespaces',
		    header : false,
		    hideHeaders : true,
		    store: namespacesStore,
		    flex : 1,
		    border : 0,
		    
		    columns: [
		        { 
		        	text: 'Namespace', 
		        	dataIndex: 'namespace', 
		        	flex: 1 
		        },
		        {
		        	text : 'Actions',
		        	xtype : 'actioncolumn',
		        	width : 60,
		        	items: [
						{
			                iconCls: 'icon-remove',
			                tooltip: 'Remove',
			                handler: function(grid, rowIndex, colIndex) {
			                	
			                    var 
			                    	record = grid.getStore().getAt(rowIndex),
			                    	namespace = record.get('namespace'),
			                    	userName = me.personCombo.getValue()
			                    ;
			                    
			                	me.removeNamespace(namespace, userName);
			                	
			                }
			            }
		        	]
		        	
		        }
		        
		    ]
		
		});
		
		this.personCombo = Ext.create('Ext.form.field.ComboBox', {
			
			itemId : 'usercombo',
			minChars : 3,
		    labelWidth : 50,
		    queryMode: 'remote',
		    queryParam: 'filter',
		    displayField : 'displayName',
		    valueField: 'userName',
		    hideTrigger : true,
		    grow : true,
		    fieldLabel: 'User',
		    emptyText: 'username',
		    labelSeparator : '',
		    labelStyle : 'background-repeat:no-repeat ; background-position:center',
		    
		    listConfig: {
				loadingText: 'Recherche...',
				emptyText: 'Aucun utilisateur trouvé.'		
			},
			
			store : usersStore,
			
			listeners : {
				
				change : function(combo, newValue) {
					
					if (newValue) return;
					
					var 
						addButton = me.queryById('add-button')
					;
					
					addButton.setDisabled(true);
					
				},
				
				select : function(combo, records, e) {
					
					if (records.length == 0) return;
					
					var 
						firstRecord = records[0],
						userName = firstRecord.get('userName'),
						displayName = firstRecord.get('displayName'),
						addButton = me.queryById('add-button')
					;
					
					addButton.setDisabled(!userName);
					
					if (!userName) return;
					
					var namespacesStore = me.namespacesGrid.getStore();
					namespacesStore.load({
						params: {
							'userName' : userName
						}
					});
					
				}
				
			},
			
			flex : 1
		    
		});
				
		this.items = [
			{
				xtype : 'panel',
				region : 'center',
				layout : 'vbox',
				plain : true,
			    title: false,				
				defaults : {
					width : '100%',
					margin : 0
				},
				items : [
				    {
				    	xtype : 'container',
				    	layout : {
				    		type : 'hbox',
				    		align : 'stretch'
				    	},
				    	padding : 3,
				    	items : [
				    	    this.personCombo,
					        { 
					        	xtype: 'button',
					        	itemId : 'add-button',
					        	text: 'Add',
					        	iconCls : 'icon-add',
					        	margin : 2,
					        	handler : function() {
									me.addNamespace();
					        	},
					        	disabled : true
					        }
				    	]
				    },
					
					this.namespacesGrid
				]
			}
		];
		
		this.callParent();
		
	},
	
	getSelectedUserName : function() {
				
		return this.personCombo.getValue();
		
	},
	
	addNamespace : function() {
		
		
		var
			me = this,
			addWin = Ext.create('Ext.window.Window', {
			
				title : 'Add a namespace',
				layout : 'fit',
				height : 200,
				width : 400,
				items : [
				    {
				    	xtype : 'addnamespaceform',
				    	header : false,
				    	userName : this.getSelectedUserName() || '',
				    	onSuccess : function() {
				    		
				    		me.reloadNamespaces();
				    		addWin.close();
				    		
				    	}
				    }
				]
				
			})
		;

		addWin.show();
		
	},
	
	reloadNamespaces : function() {
		
		var 
			namespacesGrid = this.namespacesGrid,
			namespacesStore = namespacesGrid.getStore()
		;
		
		namespacesStore.reload();
		
	},
	
	removeNamespace : function(namespace, userName) {

    	var
			me = this,
			headers = ModuleBuilder.utils.Alfresco.getAlfrescoHeaders()
		;
		
		Ext.Ajax.request({
			
		    url : '/share/proxy/alfresco/bluedolmen/mps/namespace-permission/' + namespace,
		    method : 'POST',
		    headers : headers,
		    jsonData : {
		    	userName : userName
		    },
		    
		    success: function(response){
		        
		    	me.reloadNamespaces();
		    	
		    },
		    
		    failure : ModuleBuilder.utils.Alfresco.messageBoxErrorHandler
		
		});
		
		
	}
	
});