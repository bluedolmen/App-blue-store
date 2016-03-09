Ext.define('Marketplace.view.UserButtonMenu', {
	
	requires : [
	    'Marketplace.util.StorageUtils'
	],
	
	extend : 'Ext.button.Button',
	xtype : 'userbuttonmenu',
	
	componentCls : 'userbuttonmenu',
	
	scale : 'large',
	text : i18n.t('mp:window.user-button.label'),
	
	iconCls : 'icon-user',
	
	handler : function() {
		
		this.authenticateToStorage();
		
	},
	
	authenticateToStorage : function() {
		
		Marketplace.util.StorageUtils.login();
		
	},
	
	updateLogin : function() {
		
		var me = this;
		
		Marketplace.util.StorageUtils.requestUsernameInformation (
				
			function onAvailable(loginInformation) {
				
				var
					firstName = loginInformation.firstName || null,
					lastName = loginInformation.lastName || null,
					displayName = Ext.Array.clean([firstName, lastName]).join(' ')
				;
				
				me.setText(displayName);
				
			}
			
		);
		
	},
	
	clearLogin : function() {
		
		this.setText(i18n.t('mp:window.user-button.label'));
		
	}
	
});