Ext.Loader.setConfig(
	{
		enabled : true,
		disableCaching : false,
		paths : {
			'Bluedolmen' : '/share/res/bluedolmen',
			'Ext' : '/extjs/src'
		}
	}
);

Ext.require([
], function() {
	
	Ext.useShims = true;//Ext.isIE;
	initApplication();
	
});

function initApplication() {

	Ext.application({

	    name: 'Marketplace',
	    appFolder : '/share/res/marketplace',

	    requires: [
	        'Ext.state.CookieProvider',
	        'Ext.window.MessageBox',
	        'Ext.tip.QuickTipManager',
	        'Marketplace.view.Viewport',
	        'Marketplace.util.StorageUtils'
	    ],

	    controllers: [
	        'Main',
	        'Modules'
	    ],
	    
	    models : [
			'ModuleDetails'
		],
		
		stores : [
//			'Modules',
			'InstalledModules'
		],

	    init: function() {
	    	
	    	var 
	    		me = this,
	    		installedModuleStore = null
	    	;
	    	
	    	Ext.create('Marketplace.store.Modules'); // Create the store in the store-manager as a side effect
	    	
	        Ext.setGlyphFontFamily('Pictos');
	        Ext.tip.QuickTipManager.init();
	        Ext.state.Manager.setProvider(Ext.create('Ext.state.CookieProvider'));
	        
	        if (AppStore.browseOnly) {
	        	me.initViewPort();
	        } else {
		        installedModuleStore = Ext.StoreManager.get('InstalledModules'); // load store as a side effect
		        installedModuleStore.on('load', this.initViewPort, this /* scope */);
		        installedModuleStore.load(function() {installedModuleStore.un('load', me.initViewPort, me);});	        		        		        	
	        }
	        
	    },
	    
	    initViewPort : function(store, records) {
	    	
        	this.viewport = Ext.create('Marketplace.view.Viewport');
        	this.hideLoadingMask();
        	
	        StorageUtils.retrieveSessionTicket();

	    },
	    
		hideLoadingMask : function() {
		    Ext.get('loading-mask').fadeOut({remove:true});			
		    Ext.get('loading').remove();			
		}
	    
	});	
	
}
