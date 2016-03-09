Ext.define('Marketplace.controller.Modules', {
	
    extend: 'Ext.app.Controller',
    
    requires: [
        'Marketplace.view.ContentPanel',
        'Marketplace.view.DetailsPanel',
        'Marketplace.view.Navigation',
        'Marketplace.view.ModulesView',
        'Ext.window.Window',
    ],
    
    stores: [
		'Categories',
		'InstalledModules',
		'Modules'
	 ],
         
	refs : [
		{
			ref : 'tagsGrid',
			selector : '#tags-grid'
		},
		{
			ref : 'detailsPanel',
			selector : 'detailspanel'
		}
	],
	
	currentFilters : {
		
		'tags' : [],
		'privacy' : []
		
	},
		
	init: function() {
		
		this.control({
			
			'modulesview' : {
				load : this.onModulesViewStoreLoad,
				selectionchange : this.onModuleSelectionChange
			},
			'#tags-grid' : {
				selectionchange : this.onTagSelected
			},
			'#privacy-grid' : {
				selectionchange : this.onPrivacySelected
			},
			'mainfilterbox' : {
				change : this.onFilterChange
			}
			
		});
		
		this.callParent();
	},
	
	onModulesViewStoreLoad : function(store, records, successful) {
		
		var 
			me = this,
			tagsGrid = this.getTagsGrid(),
			data = [],
			jsonStore = null;
			tagsCount = {}
		;
		
		store.each(function(record) {
			
			var tags = record.get('tags');
			if (!tags) return;
			
			Ext.Array.forEach(tags, function(tag) {
				if (undefined === tagsCount[tag]) {
					tagsCount[tag] = 0;
				}
				tagsCount[tag] = tagsCount[tag] + 1;
			});
			
		});
		
		Ext.Object.each(tagsCount, function(key, value, myself) {
			
			data.push({
				name : key,
				count : value
			});
			
		});
		
		jsonStore = Ext.create('Ext.data.JsonStore', { fields : ['name','count'], data : data});
		jsonStore.sort({
			property : 'name'
		});
		
		tagsGrid.bindStore(jsonStore);
		
	},
	
	onTagSelected : function(grid, selected) {
		
		var 
			selectedTags = Ext.Array.map(selected, function(record) {
				return record.get('name');
			}),
			modulesStore = this.getModulesStore()
		;
		
		this.currentFilters['tags'] = (selected.length == 0) ? null : {
				
			filterFn : function(record) {
				
				return 0 != Ext.Array.intersect(record.get('tags'), selectedTags).length; 
				
			}	
		
		}; 
		
		this._applyModuleFilters();
		
	},

	onPrivacySelected : function(grid, selected) {
		
		var 
			privacy,
			modulesStore = this.getModulesStore()
		;
		
		privacy = selected.length == 0 ? null : selected[0].get('name');
		this.currentFilters['privacy'] = (selected.length == 0) ? null : {
				
			filterFn : function(record) {
				
				var isPublic = record.get('isPublic');
				return ('PUBLIC' == privacy && true == isPublic) || ('PRIVATE' == privacy && false == isPublic);
				
			}
		
		};
		
		this._applyModuleFilters();
		
	},
	
	_applyModuleFilters : function() {
		
		var 
			modulesStore = this.getModulesStore(),
			filters = [],
			filterId
		;
		
		for (filterId in this.currentFilters) {
			filters.push.apply(filters, [].concat(this.currentFilters[filterId] || []));
		}
		
		if (filters.length == 0) {
			modulesStore.clearFilter(false);
			return;
		}
		
		modulesStore.clearFilter(true);
		modulesStore.filter(filters);
		
		
	},
	
	onModuleSelectionChange : function(view, selected, eOpts) {
		
		if (Ext.isEmpty(selected)) return; // should remove the details
		
		var 
			detailsPanel = this.getDetailsPanel(),
			selectedModule = selected[0],
			moduleId = selectedModule.get('id'),
			ModuleDetails = Ext.ModelManager.getModel('Marketplace.model.ModuleDetails')
		;
		
		ModuleDetails.load(moduleId, {
			
			callback: function(record, operation, success) {
				if (!success) return;
				detailsPanel.showDetails(record);
		    }
			
		});
		
	},
	
	onFilterChange : function(textfield, newValue, oldValue, eOpts ) {
		
		var 
			modulesStore = this.getModulesStore()
		;
		
		this.currentFilters['text'] = (Ext.isEmpty(newValue) || newValue.length < 2) ? null : {
			filterFn : function(record) {
				
				var
					name = record.get('name'),
					id = record.get('id')
				;
				
				return -1 != name.indexOf(newValue) || -1 != id.indexOf(newValue); 
				
			}	
		};

		this._applyModuleFilters();
		
	}

    
});
