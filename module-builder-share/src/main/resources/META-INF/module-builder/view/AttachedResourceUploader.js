Ext.define('ModuleBuilder.view.AttachedResourceUploader', {
	
	extend : 'Ext.container.Container',
	
	xtype : 'attachedresourceupload',
	
	url : '/share/proxy/alfresco/bluedolmen/mp/module-generator/attached-resource/{classification}',
	buttonText : 'Upload',
	kind : 'default',
	
	replace : false,
	editable : false,
	previewWidth : 0,
	displayName : true,
	
	title : null,
	
	store : null,
	
	layout : {
		type : 'vbox',
		align : 'stretch'
	},
	
	initComponent : function() {
		
		var 
			gridPlugins = []
		;
		
		this.layout.type = true === this.replace ? 'hbox' : 'vbox';
		
		if (true === this.editable) {
			
	        this.cellEditing = Ext.create('Ext.grid.plugin.CellEditing', {
	            clicksToEdit: 1
	        });
	        
	        gridPlugins = [
	            this.cellEditing
	        ];

		}
		
		this.items = [
			{
		        xtype : 'form',
		        border : false,
		        items : [
		            {
		            	flex: 1,
		        	    xtype : 'filefield',
		        	    name: 'content',
		        	    buttonText: this.buttonText,
		        	    clearOnSubmit : false,
		        	    buttonOnly : true,
		        	    listeners : {
			        	    'change' : this.onFileChange,
			        	    scope : this
	        	    	}

	        	   	}
	        	]
				
			},
			{
				xtype : 'gridpanel',
		        flex : 1,
				hideHeaders : true,
				minHeight : '30px',
				border : false,
				store : this.getStore(),
				disableSelection : true,
				bodyStyle : {
					borderWidth : '0px'
				},
				columns : this.getGridColumnsDefinition(),
				plugins : gridPlugins
			}
		];
		
		if (null != this.title) {
			
			this.items = [
			    {
			    	xtype : 'fieldset',
			    	flex : 1,
			    	title : this.title,
			    	layout : this.layout,
			    	items : this.items,
			    	padding : '0 0 7 5'
			    }
			];
			
		}
		
		this.callParent();
		
	},
	
	getStore : function() {
		
		var storeUrl;
		
		if (null == this.store) {
			
			storeUrl = '/share/proxy/alfresco/bluedolmen/mp/module-generator/attached-resources/{classification}'
				.replace(/\{classification\}/, this.kind);
			
			this.store = Ext.create('Ext.data.JsonStore', {
				
				autoLoad : false,
				
			    proxy: {
			        type: 'ajax',
			        url: storeUrl,
			        reader: {
			            type: 'json',
			            root: 'resources',
			            idProperty: 'name'
			        }
			    },
				
				fields : this.getStoreFieldsDefinition(),
				
			});
			
		}
		
		return this.store;
		
	},
	
	/**
	 * @protected
	 */
	getStoreFieldsDefinition : function() {
		
		return [
		    {name : 'name', type : 'string'},
		    {name : 'size', type : 'int'}
		];
		
	},
	
	/**
	 * @protected
	 */
	getGridColumnsDefinition : function() {
		
		var
			me = this,
			columns = []
		;
		
		if (this.previewWidth > 0) {
			
			columns.push(
				{
					text : 'preview',
					width : me.previewWidth + 4,
					dataIndex : 'name',
					renderer : function(value, metaData, record) {
						
						var 
							url = '/share/proxy/alfresco/bluedolmen/mp/module-generator/attached-resource/{classification}/{name}/content'
								.replace(/\{classification\}/, me.kind)
								.replace(/\{name\}/, value)
						;
						
						return '<img src="' + url + '" width="' + me.previewWidth + 'px" />';
						
					}
				}
			);
			
		}
		
		if (this.displayName) {
			columns.push(
			    {
			    	text : 'name',
			    	width : 250,
			    	dataIndex : 'name',
			    	renderer : function(value, metaData, record) {
			    		
			    		var 
			    			sizeInBytes = record.get('size'),
			    			readableSize = Ext.util.Format.fileSize(sizeInBytes)
			    		;
			    		
			    		return value + (null != readableSize ? ' (' + readableSize + ')' : '');
			    		
			    	}
			    }
			);			
		}
		
		columns.push(
		    {
		    	text : 'action',
		    	xtype : 'actioncolumn',
		    	width : 40,
		    	items : [
		    	    {
		    	    	iconCls : 'icon-remove',
		    	    	handler : this.onRemoveAttachedResource,
		    	    	scope : this
		    	    }
		    	]
		    	
		    }
		);
		
		return columns;
		
	},
	
	onFileChange : function(field, value, eOpts) {

    	var 
    		me = this,
			form = field.up('form').getForm(),
			url = '/share/proxy/alfresco/bluedolmen/mp/module-generator/attached-resource/{classification}'
		;
    	
    	url = url.replace(/\{classification\}/, this.kind);

		if (Alfresco.util.CSRFPolicy && Alfresco.util.CSRFPolicy.isFilterEnabled()) {
			url += "?" + Alfresco.util.CSRFPolicy.getParameter() + "=" + encodeURIComponent(Alfresco.util.CSRFPolicy.getToken());
		}

		form.submit({
			url : url,
			params: {
				kind: this.kind,
				replace : this.replace
			},
			errorReader : {
				read : function(response) {
					return {
						success : true,
						errors : []
					}
				}
			},
			success : function(form, action) {
				me.refreshAttachedResources();
			},
			failure : function(response) {
    			Ext.MessageBox.show({
    				title : 'Failure!',
    				msg : "Cannot attach the resource.",
    				buttons : Ext.MessageBox.OK,
    				icon : Ext.MessageBox.ERROR
    			});				
			}
		});

	},
	
	onRemoveAttachedResource : function(grid, rowIndex, colIndex) {
		
		var 
			me = this,
			record = grid.getStore().getAt(rowIndex),
		 	name = record.get('name'),
			url = '/share/proxy/alfresco/bluedolmen/mp/module-generator/attached-resource/{classification}/{name}',
			headers = {}
		;
		 
    	url = url
    		.replace(/\{classification\}/, this.kind)
    		.replace(/\{name\}/,name)
    	;
		 
		if (Alfresco.util.CSRFPolicy && Alfresco.util.CSRFPolicy.isFilterEnabled()) {
			headers[Alfresco.util.CSRFPolicy.getHeader()] = Alfresco.util.CSRFPolicy.getToken();
		}
		
    	Ext.Ajax.request({
    		url : url,
    		method : 'DELETE',
    		headers : headers,
    		scope : this,
    		success : function(response) {
    			me.refreshAttachedResources();
    		},
    		failure : function(response) {
    			Ext.MessageBox.show({
    				title : 'Failure!',
    				msg : "Cannot remove the attached resource '" + name + "'",
    				buttons : Ext.MessageBox.OK,
    				icon : Ext.MessageBox.ERROR
    			});
    		}
    	});
		
	},
	
	refreshAttachedResources : function() {
		
		this.store.reload();		
		
	}
	
});