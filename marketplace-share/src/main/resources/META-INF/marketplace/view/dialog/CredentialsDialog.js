Ext.define('Marketplace.view.dialog.CredentialsDialog', {

	extend : 'Ext.window.Window',
	
	requires : [
	    'Ext.form.Panel'
	],
	
	title : 'Login',
	layout : 'fit',
	
	height : 180,
    width : 300,
    
	defaults : {
		width : '100%',
		margin : '10'
	},
	
	initComponent : function() {
		
		var
			me = this
		;
		
		this.items = this._getItems();
		
		this.buttons = [
			{
				text : 'Login',
				itemId : 'login-button',
				iconCls : 'icon-login',
				handler : this.onLoginClick,
				scope : this
				
			}
		];		
		
		this.on('afterRender', function(thisForm, options) {
			
	        me.keyNav = Ext.create('Ext.util.KeyNav', this.el, {                    
	            enter: me.onLoginClick,
	            scope: me
	        });
	        
	    });
		
		this.callParent();
		
	},
	
	_getItems : function() {
		
		var
			me = this,
				
			items  = [
			          
				{
				    xtype: 'textfield',
				    name: 'username',
				    itemId: 'username-field',
				    fieldLabel: 'Username',
				    allowBlank: false,  // requires a non-empty value
				    value : (Alfresco.constants.USERNAME || '')
				}, 
				{
				    xtype: 'textfield',
				    name: 'password',
				    itemId: 'password-field',
				    fieldLabel: 'Password',
				    inputType : 'password',
				    allowBlank: false
				}
				
			]
		;
		
		return [
		        
			Ext.create('Ext.form.Panel', {
				border : false,
				defaults : {
					margin : '5 0 5 0'
				},
				items : items
			})
			
		];
		
	},
	
	onLoginClick : function(button) {
		
		var
			me = this,
			url = Alfresco.constants.PROXY_URI_RELATIVE + 'bluedolmen/mp/remote-login',
			username = this.queryById('username-field').getValue(),
			password = this.queryById('password-field').getValue()
		;
		
		function onFailure(response) {

			var
				json = Ext.decode(response.responseText)
			;
			
			Ext.Msg.show({
				title : 'Login failed!',
				msg : 'Cannot authenticate.' + '<br>' + (json.message || 'Please check your username/password.'),
				icon : Ext.MessageBox.ERROR,
				buttons : Ext.MessageBox.OK
			});
			
		}
		
		this.setLoading('Authentication in progress...');
		
		Ext.Ajax.request({
			
			url : url,
			
			method : 'POST',
//			cors : true,
//			withCredentials : true,
//			
			jsonData : {
				username : username,
				password : password
			},

			success : function(response, options) {
				
				var
					json = Ext.decode(response.responseText),
					success = json.success,
					ticket = json.ticket
				;
				
				if (false === success) {
					onFailure(response);
				}
				else {
					me.onSuccess(ticket, username);
					me.close();
				}
				
			},
			
			failure : onFailure,
			
			callback : function() {
				
				me.setLoading(false);
				
			}
			
		});
		
	},
	
	onSuccess : function(ticket, userName) {
		
		this.fireEvent('success', ticket, userName);
		this.storeTicket(ticket, userName);
		
	},
	
	storeTicket : Ext.emptyFn
	
});