Ext.define('Marketplace.controller.Main', {
	
    extend: 'Ext.app.Controller',
    
    requires: [
        'Marketplace.view.Header',
        'Marketplace.util.StorageUtils',        
        'Ext.window.Window',
    ],
    
	refs : [
		{
			ref : 'userButton',
			selector : 'userbuttonmenu'
		},
		{
			ref : 'contentPanel',
			selector : '#content-panel'
		}
	],
		
	init: function() {
		
		this.control({
			
		});
		
		Marketplace.util.StorageUtils.on({
			login : this.onLogin,
			logout : this.onLogout,
			scope : this
		});
			
		this.callParent();
	},
	
	onApplicationReady : function() {
		
        StorageUtils.retrieveSessionTicket();

	},
	
	onLogin : function(ticket, userName) {
		
		this.application.fireEvent('login', ticket, userName)
		
		var userButton = this.getUserButton();
		userButton.updateLogin();
		
		var contentPanel = this.getContentPanel();
		contentPanel.refreshStore();
		
	},
	
	onLogout : function() {
		
		this.application.fireEvent('logout');
		
		var userButton = this.getUserButton();
		userButton.clearLogin();
		
		var contentPanel = this.getContentPanel();
		contentPanel.refreshStore();
		
	}
    
});
