Ext.Loader.setConfig(
	{
		enabled : true,
		disableCaching : false,
		paths : {
			'Bluedolmen' : '/share/res/bluedolmen',
			'Ext' : '/extjs/src',
			'Marketplace' : '/share/res/marketplace'
		}
	}
);

Ext.require([	
], function() {
	Ext.useShims = true;
	initApplication();	
});

AppStore = {
	
	MAIN_URL : ''
	
}

// Extend i18n.sync.fetchOne in order to customise the url that the file gets loaded with
var originalFetchOne = i18n.sync._fetchOne;
i18n.sync._fetchOne = function (lng, ns, options, done) {
    // Could enhance this to deal with multi-level namespaces like ns1:ns2:key etc
    if (ns == 'mp') {
        options.resGetPath = '/share/res/marketplace/locales/__lng__/__ns__.json';
    }
    return originalFetchOne(lng, ns, options, done);
}

var options = {
	resGetPath : '/share/res/module-builder/locales/__lng__/__ns__.json',
	lng : 'en-US',
	ns: { 
		namespaces: ['mb','mp'], 
		defaultNs: 'mb'
	}			
};
i18n.init(options);

function initApplication() {

	Ext.application({

	    name: 'ModuleBuilder',
	    appFolder : '/share/res/module-builder',

	    requires: [
	        'Ext.state.CookieProvider',
	        'Ext.window.MessageBox',
	        'Ext.tip.QuickTipManager',
	        'ModuleBuilder.view.Viewport',
	        'ModuleBuilder.utils.Alfresco'
	    ],

	    controllers: [
	        'Main',
	        'Marketplace.controller.Modules'
	    ],
	    
	    models : [
	        'ModuleBuilder.model.Form',
	        'Marketplace.model.ModuleDetails'
		],
		
//		stores : [
//		    'Marketplace.store.Modules'
//		],

//	    autoCreateViewport: true,

	    init: function() {
	    	
	    	var me = this;
	    	
	        Ext.setGlyphFontFamily('Pictos');
	        Ext.tip.QuickTipManager.init();
	        Ext.state.Manager.setProvider(Ext.create('Ext.state.CookieProvider'));
	        
	    	var modulesStore = Ext.create('Marketplace.store.Modules',{
    			proxy : {
    				type : 'ajax',
    				url : '/share/proxy/alfresco/bluedolmen/mps/list',
    				reader : {
    					type : 'json',
    					root : 'modules'
    				},
    				extraParams : {
    					onlyManagedContainers : true
    				}
//    				callbackKey: 'alf_callback'
    			}	    			
	    	}); // Create the store in the store-manager as a side effect
	        
	        this.initViewPort();
	        
	    },
	    
	    initViewPort : function(store, records) {
	    	
        	this.viewport = Ext.create('ModuleBuilder.view.Viewport');
        	this.hideLoadingMask();
	    	
	    },
	    
		hideLoadingMask : function() {
		    Ext.get('loading-mask').fadeOut({remove:true});			
		    Ext.get('loading').remove();			
		}
	    
	});	
	
}

