Ext.define('ModuleBuidler.view.FormBuilder', {
	
	extend : 'Ext.tree.Panel',
	
	requires : [
        'Ext.tree.plugin.TreeViewDragDrop'
	],
	
	xtype : 'form-builder',
	title : i18n.t('mb:form-builder.title'),
	
    useArrows: true,
    rootVisible: false,
    singleExpand: true,
    
    viewConfig : {
    	plugins : [
    	    {
	            ptype: 'treeviewdragdrop',
	            containerScroll: true
	        }
    	]
    },
    
    fieldNumber : 1,
    
    initComponent: function() {
    	
    	var
    		typeStore = Ext.create('Ext.data.JsonStore', {
    			fields : [ 'id' ],
    			data : [
                    { id : 'Text' },
                    { id : 'Date' },
                    { id : 'Boolean' },
                    { id : 'Number' }
                ],
                proxy : {
                	type : 'memory'
                },
                autoLoad : true
    		}),
    		editorDefaultConfig = {
                shadow: false,
                completeOnEnter: true,
                cancelOnEsc: true,
                updateEl: true,
                ignoreNoChange: true
            }
    	;
    	
    	this.typeStore = typeStore;
    	
    	this.store = Ext.create('Ext.data.TreeStore', {
            model: 'ModuleBuilder.model.Form',
            folderSort: true,
            root : {
            	
            	expanded : true,
            	text : 'Root'
            	
            },
            proxy: {
                type: 'sessionstorage',
                id  : 'myProxyKey'
            }
        });
    	
        this.cellEditing = Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 1
        });
        
        this.plugins = [
            this.cellEditing
        ];
        
//        this.dockedItems = [
//            {
//	            xtype: 'toolbar',
//	            items: [
//	                '->',
//	                {
//		                iconCls: 'icon-add',
//		                text: 'Add',
//		                scope: this,
//		                handler: this.onAddClick
//		            }
//	            ]
//            }
//        ];
        
        this.tools = [
            {
            	type : 'plus',
            	callback : this.onAddClick,
            	scope : this
            }
        ];
    	
    	this.columns = [
    	    {
                xtype: 'treecolumn',
                text: 'Name',
                flex: 2,
                dataIndex: 'name',
                sortable : false,
                editor: {
                	xtype : 'textfield',
                	value : ''
                }
            },
    	    {
                text: 'Label',
                flex: 2,
                dataIndex: 'label',
                sortable : false,
                editor: {
                	xtype : 'textfield',
                	value : ''
                }
            },
    	    {
                text: 'Type',
                flex: 1,
                sortable: true,
                dataIndex: 'type',
                editable: false,
                editor: Ext.create('Ext.form.field.ComboBox', {
                    typeAhead: true,
                    triggerAction: 'all',
                    store: typeStore,
                    value: 'Text',
//                    forceSelection: true,
                    displayField: 'id',
                    valueField: 'id',
                    queryMode : 'local'
                }),
                sortable : false
            },
            {
                xtype: 'actioncolumn',
                width: 30,
                sortable: false,
                menuDisabled: true,
                items: [{
                    iconCls: 'icon-remove',
                    tooltip: 'Remove Field',
                    scope: this,
                    handler: this.onRemoveClick
                }]
            }
        ];
    	
        this.callParent();
        
        this.view.on('beforedrop', function(node, data, overModel, dropPosition, dropHandlers) {
        	return "append" != dropPosition;
        });
        
    },
    
    onAddClick : function() {
    	
    	var 
    		store = this.store,
    		rootNode = store.getRootNode(),
    		rec = Ext.create('ModuleBuilder.model.Form', {
                name : 'field' + this.fieldNumber,
                label : 'Field ' + this.fieldNumber,
                type : 'Text'
            }),
            edit = this.cellEditing
    	;
    	
        edit.cancelEdit();
        rootNode.appendChild(rec);
        
        this.fieldNumber++;
        
    },
    
    onRemoveClick: function(grid, rowIndex){
        grid.getStore().removeAt(rowIndex);
    },
    
    getTypeStore : function() {
    	return this.typeStore;
    },
    
    addAvailableType : function(typeName, index) {
    	
    	if (index) {
    		this.typeStore.insert(index, {id : typeName});
    	}
    	else {
    		this.typeStore.add({id : typeName});
    	}
    	
    },
    
    removeAvailableType : function(typeName) {
    	
    	var
    		index = this.typeStore.indexOfId(typeName)
    	;
    	
    	if (index < 0) return;
    	
    	this.typeStore.removeAt(index);
    	return index;
    	
    },
    
    updateAvailableType : function(oldTypeName, newTypeName) {
    	
    	var
    		rootNode = null != this.store ? this.store.getRootNode() : null,
    		index = 0
    	;
    		
    	index = this.removeAvailableType(oldTypeName);
    	
    	this.addAvailableType(newTypeName, index);
    		
    	if (null == rootNode) return;
    	 
    	rootNode.cascadeBy(function visitor(node) {
    		
    		var
    			type = node.get('type')
    		;
    		
    		if (oldTypeName == type) {
    			node.set('type', newTypeName);
    		}
    		
    	});
    	
    }
    
});