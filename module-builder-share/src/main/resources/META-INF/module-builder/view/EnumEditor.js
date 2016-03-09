Ext.define('ModuleBuilder.view.EnumEditor', {
	
	extend : 'Ext.panel.Panel',
	
	xtype : 'enum-editor',
	
	title : 'Enumerations',
	
	layout : {
		type : 'hbox',
		align : 'stretch'
	},
	
	split : true,
	
	defaults : {
		flex : 1
	},
	
	enumNumber : 1,
	
	initComponent : function() {
		
		var
			me = this,
			enumStore = Ext.create('Ext.data.JsonStore', {
				fields : [ 
				    'name',
				    {
				    	name : 'dynamic',
				    	type : 'boolean'
				    },
				    {
				    	name : 'values',
				    	convert : function(value, record) {
				    		
				    		if (!Ext.isArray(value)) return value;
				    		
				    		return Ext.create('Ext.data.JsonStore', {
								fields : [ 'name' ],
								data : Ext.Array.map(value, function(v) {
									return { name : v };
								})
							});
				    		
				    	}
				    }
				]
			}),
			enumValuesStore = Ext.create('Ext.data.JsonStore', {
				fields : [ 'name' ]
			}),
			editorDefaultConfig = {
	            shadow: false,
	            completeOnEnter: true,
	            cancelOnEsc: true,
	            updateEl: true,
	            ignoreNoChange: true
	        }
		;
		
		this.enumStore = enumStore;
		
        this.cellEditing = Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 1
        });
        
        this.cellValuesEditing = Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 1
        });
        
        this.tools = [
            {
            	type : 'plus',
            	callback : this.onAddClick,
            	scope : this
            }
        ];
        
		this.items = [
		    {
		    	xtype : 'grid',
		    	store : enumStore,
		    	plugins : [
		    	    this.cellEditing
		    	],
		    	columns : [
		    	    {
		    	    	text : 'Name',
		    	    	dataIndex : 'name',
		    	    	flex : 1,
		    	    	menuDisabled : true,
		    	    	editor: {
		                	xtype : 'textfield',
		                	value : '',
		                }
		    	    },
		    	    {
		    	    	text : 'Dyn.',
		    	    	tooltip : 'Allow modifications?',
		    	    	dataIndex : 'dynamic',
		    	    	xtype : 'checkcolumn',
		    	    	width : 40,
		    	    	sortable : false,
		    	    	menuDisabled : true,
		    	    	editor: {
		                	xtype : 'checkbox',
		                	value : ''
		                }
		    	    },
		    	    {
		    	    	xtype  : 'actioncolumn',
		    	    	text : '&nbsp',
		    	    	width : 40,
		    	    	menuDisabled : true,
		    	    	items : [
		    	    	    {
		    	    	    	iconCls : 'icon-remove',
		    	    	    	tooltip : 'Remove',
		    	    	    	handler : function(grid, rowIndex, colIndex) {
		    	    	    		
		    	    	    		var
		    	    	    			store = grid.getStore()
		    	    	    		;
		    	    	    		
		    	    	    		store.removeAt(rowIndex);
		    	    	    		
		    	    	    	}
		    	    	    }
		    	    	]
		    	    }
		    	],
		    	listeners : {
		    		'selectionchange' : this.onEnumSelected,
            		'edit' : function(editor, e) {
            			this.fireEvent('enumupdate', e.value, e.originalValue);
            		},
            		scope : this
		    	}

		    },
		    {
		    	xtype : 'grid',
		    	store : enumValuesStore,
		    	itemId : 'enumValuesGrid',
		    	plugins : [
		    	    this.cellValuesEditing
		    	],
		    	dockedItems: [{
		            xtype: 'toolbar',
		            dock: 'top',
		            items: [
		                '->',
		                {
			            	xtype : 'button',
			            	itemId : 'addEnumValueButton',
			            	iconCls : 'icon-add',
			                text: 'Value',
			                handler : this.onCreateEnumValue,
			                scope : this,
			                disabled : true
			            }
		            ]
		        }],
		    	hideHeaders : true,
		    	columns : [
		    	    {
		    	    	text : 'Value',
		    	    	dataIndex : 'name',
		    	    	flex : 1,
		    	    	menuDisabled : true,		    	    	
		    	    	editor: {
		                	xtype : 'textfield',
		                	value : ''
		                }
		    	    },
		    	    {
		    	    	xtype  : 'actioncolumn',
		    	    	text : '&nbsp',
		    	    	width : 40,
		    	    	menuDisabled : true,
		    	    	items : [
		    	    	    {
		    	    	    	iconCls : 'icon-remove',
		    	    	    	tooltip : 'Remove',
		    	    	    	handler : function(grid, rowIndex, colIndex) {
		    	    	    		
		    	    	    		var
		    	    	    			store = grid.getStore()
		    	    	    		;
		    	    	    		
		    	    	    		store.removeAt(rowIndex);
		    	    	    		
		    	    	    	}
		    	    	    }
		    	    	]
		    	    }
		    	],
		    	padding : '0 0 0 5'
		    }
		];
		
		this.callParent();
		
		this.relayEvents(this.enumStore, ['add','remove'], 'enum');
		
	},
	
	onAddClick : function() {
		
    	var 
			store = this.enumStore,
	        edit = this.cellEditing
		;
		
		store.add({
			name : 'enum' + this.enumNumber,
			dynamic : false
		});
		
	    edit.cancelEdit();        
	    this.enumNumber++;
		
	},
	
	onCreateEnumValue : function() {
		
		var
			grid = this.queryById('enumValuesGrid'),
			edit = this.cellValuesEditing,
			store = grid.getStore()
		;
		
		if (null == store.valueNumber) {
			store.valueNumber = 1;
		}
		
		store.add({
			name : 'value' + store.valueNumber++
		});
		
	    edit.cancelEdit();
	    this.enumNumber++;
	    
	},
	
	onEnumSelected : function(grid, selected, eOpts) {
		
		var
			enumValueButton = this.queryById('addEnumValueButton'),
			enumValuesGrid = this.queryById('enumValuesGrid'),
			firstSelected = null
		;
		
		enumValueButton.setDisabled(Ext.isEmpty(selected));
		
		if (Ext.isEmpty(selected)) return;
		
		firstSelected = selected[0];
		
		if (null == firstSelected.valuesStore) {
			
			firstSelected.valuesStore = firstSelected.get('values') || Ext.create('Ext.data.JsonStore', {
				fields : [ 'name' ]
			});
			
		}
		
		enumValuesGrid.reconfigure(firstSelected.valuesStore);
		
	},
	
	getDataModel : function() {
		
		var
			enums = this.enumStore.getRange()
		;
		
		return Ext.Array.map(enums, function(enumRecord) {
			
			var
				enumValuesStore = enumRecord.valuesStore,
				values = null == enumValuesStore ? [] : Ext.Array.map(enumValuesStore.getRange(), function(valueRecord) {
					return valueRecord.get('name');
				})
			;
			
			return ({
				
				name : enumRecord.get('name'),
				dynamic : enumRecord.get('dynamic'),
				values : values
				
			});
			
		});
		
	},
	
	setDataModel : function(model) {
		
		this.enumStore.loadData(model);
		
	}

	
});