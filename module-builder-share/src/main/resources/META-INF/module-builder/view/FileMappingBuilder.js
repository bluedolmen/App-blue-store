Ext.define('ModuleBuidler.view.FileMappingBuilder', {
	
	extend : 'Ext.grid.Panel',
	
	xtype : 'filemappingbuilder',
	
    useArrows: true,
    
    fieldNumber : 1,
    
    initComponent: function() {
    	
    	var
    		editorDefaultConfig = {
                shadow: false,
                completeOnEnter: true,
                cancelOnEsc: true,
                updateEl: true,
                ignoreNoChange: true
            }
    	;
    	
    	this.store = Ext.create('Ext.data.JsonStore', {
    		fields : [
    		    { name : 'source', type : 'string' },
    		    { name : 'target', type : 'string' }
    		],
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
        
        this.tools = [
            {
            	type : 'plus',
            	callback : this.onAddClick,
            	scope : this
            }
        ];
    	
    	this.columns = [
    	    {
                text: 'source',
                flex: 1,
                dataIndex: 'source',
                editor: {
                	xtype : 'textfield',
                	value : ''
                }
            },
    	    {
                text: 'target',
                flex: 3,
                dataIndex: 'target',
                sortable : false,
                editor: {
                	xtype : 'textfield',
                	value : ''
                }
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
        
    },
    
    onAddClick : function() {
    	
    	var 
    		store = this.store,
            edit = this.cellEditing
    	;
    	
    	store.add({
    		source : 'directory' + this.fieldNumber,
    		target : 'app:dictionary/cm:bluedolmen/cm:directory' + this.fieldNumber + '/'
    	});
    	
        edit.cancelEdit();        
        this.fieldNumber++;
        
    },
    
    onRemoveClick: function(grid, rowIndex){
        grid.getStore().removeAt(rowIndex);
    },
    
    getDataModel : function() {
    	
    	var
    		store = this.getStore(),
    		records = store.getRange()
    	;
    	
    	return ({
    		
    		mappings : Ext.Array.map(records, function(record) {
    			return record.getData();
    		})
    		
    	});
    	
    },
    
    setDataModel : function(dataModel) {
    	
    	var
			store = this.getStore()
		;
    	
    	if (dataModel.mappings) {
    		store.loadData(dataModel.mappings);
    	}
    	
    }
    
});