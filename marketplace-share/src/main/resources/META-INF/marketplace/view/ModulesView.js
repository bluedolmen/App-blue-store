Ext.define('Marketplace.view.ModulesView', {
	
	extend : 'Ext.view.View',
	
	requires : [
		'Marketplace.view.ModuleDetails',
		'Marketplace.view.LongRunningOperationWindow'
	],
	
	xtype : 'modulesview',
	
	store : null, // required
	
    multiSelect: true,
    trackOver: true,
    overItemCls: 'x-item-over',
    itemSelector: 'div.thumb-wrap',
    emptyText: 'No modules to display',
    
    initComponent : function() {
    	
    	var me = this;
    	
    	Ext.Array.forEach(this.extraActionButtons, function(actionButton) {
    		
    		if (undefined !== actionButton.position) {
    			Ext.Array.insert(me.actionButtons, actionButton.position, [actionButton]);
    		}
    		else {
    			me.actionButtons.push(actionButton);
    		}
    		
    	});
    	
    	this.mappedActionButtons = Ext.Array.toValueMap(this.actionButtons, function(actionButton){return actionButton.id;});
    	
    	this.tpl = this.getInitTpl();
    	
    	this.store = Ext.StoreManager.get('Modules');
    	this.store.load();
    	
    	this.callParent(arguments);
    	
    	var selectionModel = this.getSelectionModel();
    	selectionModel.setSelectionMode('SINGLE');
    	
    },
    
    actionButtons : [
        {
        	id : 'install',
        	iconCls : 'icon-install',
        	isAvailable : function(moduleId, version, cost, isInstalled, isLastVersion, isFree, isBrowseOnly) {
        		return !isBrowseOnly && (!isInstalled && isFree);
        	},
        	longRunning : true,
//        	handler : function(record) {
//        		
//            	if (true === AppStore.browseOnly) {
//            		
//            		Ext.MessageBox.show({
//            			
//            			title : 'Browse Only!',
//            			msg : 'The Store is in browse-only mode. This module cannot be installed as such.',
//            			buttons : Ext.MessageBox.OK,
//            			icon : Ext.MessageBox.INFO
//            			
//            		});
//            		
//            		return;
//            		
//            	}
//            	
//            	this.onLongRunningOperation(record, 'install');
//        		
//        	},
        	options : {
        		'overrideExisting' : true
        	}
        },
        {
        	id : 'buy',
        	iconCls : 'icon-buy',
        	isAvailable : function(moduleId, version, cost, isInstalled, isLastVersion, isFree, isBrowseOnly) {
        		return !isInstalled && !isFree;
        	},
        	handler : function(record) {
        		Ext.create('Marketplace.view.PaymentWindow').show();
        	}
        },
        {
        	id : 'update',
        	iconCls : 'icon-update' 
        },
        {
        	id : 'uninstall',
        	iconCls : 'icon-uninstall',
        	isAvailable : function(moduleId, version, cost, isInstalled, isLastVersion, isFree, isBrowseOnly) {
        		return !isBrowseOnly && isInstalled;
        	},
        	longRunning : true
        },
        {
        	id : 'comment',
        	iconCls : 'icon-comment',
        	isAvailable : false
        },
        {
        	id : 'share',
        	iconCls : 'icon-share',
        	isAvailable : false
        }
	],
	
	extraActionButtons : [],
    
    getInitTpl : function() {
    	
    	var tpl = [
   			'<tpl for=".">',
    			'<div class="thumb-wrap wrapper" id="{id:stripTags}">',
    				'<div class="buttons">'
    	];
    	
    	Ext.Array.forEach(this.actionButtons, function(actionButton){
    		
    		tpl.push(
		    			'<div '   
		    			+ 'id="' + actionButton.id + '-{id:stripTags}" ' 
		    			+ 'class="' + (actionButton.iconCls ? actionButton.iconCls : 'icon-' + actionButton.id) + ' button-icon">'
		    			+ '</div>'
    		);
    		
    	});
    	
    	Ext.Array.push(tpl, [
    				'</div>',
    				'<tpl if="null != this.installedModule(id)">',
    					'<div class="ribbon-wrapper"><div class="ribbon ribbon-green"><div class="ribbon-installed">INST v.{[this.installedModule(values.id).get("version")]}</div></div></div>',
    				'<tpl elseif="this.isNew(createdOn)">',
    					'<div class="ribbon-wrapper"><div class="ribbon ribbon-red">NEW</div></div>',
    				'</tpl>',
    				'<div class="thumb"><img src="{[this.resolveAppStoreProtocol(values.logoUrl)]}" title="{id:htmlEncode}"></div>',
    				'<div class="modulePrivacy {[this.getPrivacyModuleClass(values.isPublic)]}"></div>',
    				'<div class="appName">{name:htmlEncode}<span class="paragraph-end"></span></div>',
    				'<div class="appAuthor">{author:htmlEncode}</div>',
    				'<div class="cost">{cost:htmlEncode}</div>',
    				'<div class="star-rating">',
    					'<div class="current-rating" style="width:{rating * 20.}%">',
    					'</div>',
    				'</div>',
    			'</div>',
    		'</tpl>',
    		'<div class="x-clear"></div>',
    		{
    			installedModule : function(moduleId) {
    	    		var 
    	    			installedModulesStore = Ext.StoreManager.get('InstalledModules'),
    	    			installedModule = installedModulesStore.getById(moduleId)
    	    		;
    	    		return installedModule;
    			},
    			isNew : function(createdDate) {
    				
    				var dt = Ext.Date.subtract(new Date(), Ext.Date.DAY, 1);
    				return (createdDate > dt);
    				
    			},
    			getPrivacyModuleClass : function(isPublic) {
    				
    				return false == isPublic ? 'private-module' : 'public-module';
    				
    			},
    			resolveAppStoreProtocol : function(value) {
    				
    				value = StorageUtils.resolveAppStoreProtocol(value);
    				value = StorageUtils.authenticatedUrl(value);
    				return value;
    				
    			}
    		}
    	]);
    	
    	return new Ext.XTemplate(tpl);
    	
    },
    
    bindStoreListeners : function(store) {
    	
    	var me = this;
    	me.relayEvents(store, ['load','filterchange','groupchange']);
        	
    	this.callParent(arguments);
    	
    },
    
    listeners: {
    	
    	buttonclicked : function(button, operation, record) {
    		
    		var 
    			handler = this.mappedActionButtons[operation].handler,
    			longRunning = true === this.mappedActionButtons[operation].longRunning
    		;
    		if (Ext.isFunction(handler)) {
    			handler.call(this, record);
    		}
    		
    		if (longRunning) {
    			this.onLongRunningOperation.call(this, record, operation);
    		}
    		
    	},
    	
    	select : function(store, record) {
    		var 
    			me = this,
    			node = Ext.get(this.getNodeByRecord(record))
    		;
    		node.addCls('thumb-selected');
    	},
    	
    	deselect : function(store, record) {
    		var 
    			me = this,
    			node = Ext.get(this.getNodeByRecord(record))
    		;
    		node.removeCls('thumb-selected');
    	},
    	
        selectionchange: function(store, records){
        	
            var 
            	l = records.length,
            	s = l !== 1 ? 's' : '',
				firstRecord = records[0],
				node = firstRecord ? Ext.get(this.getNodeByRecord(firstRecord)) : null 
            ;
            
            if (!node) return;

            
            
//            var moduleId = firstNode.get('id');
            
//            Ext.create('Ext.window.Window', {
//                title: 'Hello',
//                height: 500,
//                width: 700,
//                layout: 'fit',
//                items: {  // Let's put an empty grid in just to illustrate fit layout
//                    xtype: 'moduledetails',
//                    border: false,
//                    moduleId : moduleId
//                }            	
//            }).show();
            
        },
        
        itemmouseenter : function(view, record, item, index, e) {
        	
        	var 
        		me = this,
        		buttonsDiv
        	;
        	
        	if (!item) return;
        	
        	if (!item.buttonsDiv) {
            	buttonsDiv = (item.getElementsByClassName('buttons') || [null])[0];
            	if (null == buttonsDiv) return;
            	item.buttonsDiv = Ext.get(buttonsDiv); 
        	}
        	
        	item.buttonsDiv.animate({
    	       duration: 300,
    	        to: {
    	            opacity: 100
    	        }
        	});
        	
        	if (item.listenersSet) return;
        	
        	item.listenersSet = true;
        	
        	var availableOperations = me.getAvailableOperations(record);
        	
        	var buttons = buttonsDiv.getElementsByClassName('button-icon') || []; // DO NOT CHANGE to item.buttonsDiv
        	Ext.Array.forEach(Ext.Array.toArray(buttons), function(button) {

        		var
        			operation = (button.id || "").split('-')[0],
        			el = Ext.get(button),
        			operationAvailable = availableOperations[operation] || false
        		;
        		
        		if (!operationAvailable) {
        			// hide operation
        			el.setVisibilityMode(Ext.dom.AbstractElement.DISPLAY);
        			el.setVisible(false);
        			return;
        		}
        		
        		el.on('click', function(e, t, opts) {
					e.stopEvent();
					me.fireEvent('buttonclicked', el, operation, record);
        		});

        	}); 	
        	
        },
        
        itemmouseleave : function(view, record, item, index, e) {
        	
        	if (!item) return;
        	if (!item.buttonsDiv) return;
        	
        	item.buttonsDiv.animate({
    	       duration: 300,
    	        to: {
    	            opacity: 0
    	        }
        	});
        	
        }
        
//        itemcontextmenu : function(view, record, item, index, e) {
//        	
//			var
//				moduleName = record.get('name'),
//				moduleMenu = Ext.create('Ext.menu.Menu', {
//					title : moduleName,
//				    plain: true,
//				    renderTo: Ext.getBody(),
//				    items : [
//				    	{
//				    		text : "Install",
//				    		iconCls : 'icon-install',
//				    		listeners : {
//				    			click : function(item) {
//				    			}
//				    		}
//				    	},
//				    	{
//				    		text : "Update",
//				    		iconCls : 'icon-update',
//				    		listeners : {
//				    			click : function(item) {
//				    			}
//				    		}
//				    	},
//				    	{
//				    		text : "Remove",
//				    		iconCls : 'icon-uninstall',
//				    		listeners : {
//				    			click : function(item) {
//				    			}
//				    		}
//				    	}
//				    ]
//				})
//			;
//        	
//        	var position = e.getXY();
//            e.stopEvent();
//            moduleMenu.showAt(position);
//        	
//        }
    },
    
    getAvailableOperations : function(record) {
    	
    	var 
    		me = this,
    		moduleId = record.get('id'),
    		version = record.get('version'),
    		cost = record.get('cost'),
    		installedModulesStore = Ext.StoreManager.get('InstalledModules'),
    		installedModule = installedModulesStore.getById(moduleId),
    		isInstalled = null != installedModule,
    		isLastVersion = null == version || version == installedModule.get('version'),
    		isFree = !cost || 'free' == cost.toLowerCase() || Ext.String.endsWith('0.00'),
    		isBrowseOnly = "undefined" != typeof AppStore ? AppStore.browseOnly : false,
    		availableOperations = {}
    	;
    	
    	Ext.Array.forEach(this.actionButtons, function(actionButton) {
    		
    		availableOperations[actionButton.id] = Ext.isFunction(actionButton.isAvailable)
    			? actionButton.isAvailable.call(me, moduleId, version, cost, isInstalled, isLastVersion, isFree, isBrowseOnly)
    			: (actionButton.isAvailable || false)
    		;
    		
    	});
    	
    	return availableOperations;
    	
    },
    
    onLongRunningOperation : function(record, operation) {
    	
    	var
			me = this,
			moduleId = record.get('id'),
			moduleName = record.get('name'),
			operationWindow = this.mappedActionButtons[operation].window;
		;
		
		if (!operationWindow) {
			operationWindow = Ext.create('Marketplace.view.LongRunningOperationWindow', {
				operation : operation,
				closeAction : 'hide'
			});
			operationWindow.on('success', this.refreshInstalledModulesStore, this /* scope */);
			this.mappedActionButtons[operation].window = operationWindow;
		}
		
		operationWindow.prepare(moduleId, moduleName, this.mappedActionButtons[operation].options);
		operationWindow.show();
		
    },
    
    /**
     * @private
     */
    refreshInstalledModulesStore : function(refreshMe /* true */) {
    	
		var
			me = this,
			installedModulesStore = Ext.StoreManager.get('InstalledModules')
		;
		if (null == installedModulesStore) return;

    	installedModulesStore.load(function(records, operation, success) {
    		
    		if (!success) return;
    		if (refreshMe === false) return;
    		if (!me.store) return;
    		
    		me.store.load();
    		
    	});
		
    }
        
});