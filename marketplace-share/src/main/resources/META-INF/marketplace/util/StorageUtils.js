Ext.define('Marketplace.util.StorageUtils', {
	
	alternateClassName : 'StorageUtils',
	
	requires : [
	    'Marketplace.view.dialog.CredentialsDialog'
	],
	
	mixins : {
		observable : 'Ext.util.Observable'
	},
	
	singleton : true,
	alfrescoStorageUrl : AppStore.MAIN_URL,
	APPSTORE_PROTOCOL : 'appstore://',
	
	ticket : null,
	userName : null,
	
    constructor: function (config) {
    	
        this.mixins.observable.constructor.call(this, config);
        
        this.addEvents(
            'login',
            'logout'
        );
        
    },
	
	storeTicket : function(ticket, userName) {
		
		this.ticket = ticket;
		this.userName = userName;
		
		this.fireEvent('login', ticket, userName);
		
	},
	
	getTicket : function(ticket) {

		return this.ticket;
		
	},
	
	retrieveSessionTicket : function() {
		
		var
			me = this,
			url = Alfresco.constants.PROXY_URI_RELATIVE + 'bluedolmen/mp/remote-login'
		;
		
		Ext.Ajax.request({
			
			url : url,
			
			method : 'GET',
			
			success : function(response, options) {
				
				var
					json = Ext.decode(response.responseText),
					success = json.success,
					username = json.username,
					ticket = json.ticket
				;
				
				if (true === success) {
					me.storeTicket(ticket, username);
				}
				
			}
			
		});
		
		
	},
	
	login : function(onSuccess) {
		
    	var 
			me = this
		;
    	
    	Ext.create('Marketplace.view.dialog.CredentialsDialog', {
    		
    		storeTicket : function(ticket, userName) {
    			
    			me.storeTicket(ticket, userName);
    			
    		},
    		
    		modal : true
    	
    	}).show();
		
	},
	
	isAuthenticated : function() {
		
		return (null != this.ticket);
		
	},
	
	authenticatedUrl : function(url, failAsGuest /* boolean = true */) {
		
		var 
			params = {},
			ticket = this.getTicket()
		;
		
		if (ticket) {
			params['alf_ticket'] = ticket;
		}
		else if (false !== failAsGuest){
			params['guest'] = true;
		}
		else {
			throw new Error('Not authenticated');
		}
		
		return Ext.urlAppend(url, Ext.Object.toQueryString(params));
		
	},
	
	authenticatedAjaxRequest : function(ajaxConfig) {
		
		if (!this.isAuthenticated) {
			
			Ext.MessageBox.show({
				
				title : 'Not authenticated!',
				msg : 'You have to authenticated on the marketplace to perform this operation.',
				icon : Ext.MessageBox.ERROR,
				buttons : Ext.MessageBox.OK
				
			});
			
			return false;
			
		}
		
		Ext.merge(ajaxConfig, {
			params : {
				'alf_ticket' : this.ticket
			}
		});
		
		Ext.Ajax.request(ajaxConfig);
		
	},
	
	getAjaxJSONResponse : function(response) {
		
		return Ext.decode(response.responseText);
		
	},
	
	manageAjaxFailure : function(message, response) {
		
		var
			status = response.status,
			statusText = response.statusText
		;
		
		if (403 == status) {
			this.fireEvent('logout');
		}

		Ext.MessageBox.show({
			
			title : 'Error!',
			msg : '' + ':\n' + response.responseText,
			icon : Ext.MessageBox.ERROR,
			buttons : Ext.MessageBox.OK
			
		});		
		
	},
	
	getManagedContainerIds : function() {
		
		var 
			me = this
		;
		
		this.authenticatedAjaxRequest({
			
			method : 'GET',
		
			url : this.resolveAppStoreProtocol('appstore://bluedolmen/mps/managed-containers'),
			
			success : function(response, options) {
				
				var
					json = me.getAjaxJSONResponse(response);
				;
				
				return json.managedContainers;
				
			},
			
			failure : function(response) {
				me.manageAjaxFailure('Cannot get managed containers', response);
			}
			
		});
		
	},
	
	requestUsernameInformation : function(onAvailable) {
		
		var me = this;
		
		if (!Ext.isFunction(onAvailable)) {
			Ext.Error.raise('IllegalArgumentException! Must be called with a callback function to get the result');
		}
		
		this.authenticatedAjaxRequest({
			
			method : 'GET',			
			
			url : this.resolveAppStoreProtocol('appstore://api/people/{userName}'.replace(/\{userName\}/, this.userName) ),
			
			success : function(response, options) {
				
				var json = me.getAjaxJSONResponse(response);
				onAvailable(json);
				
			},
			
			failure : function(response) {
				me.manageAjaxFailure('Cannot get user-name information', response);
			}
			
		});
		
	},
	
	resolveAppStoreProtocol : function(url) {
		
		if (!url) return url;
		if (0 != url.indexOf(this.APPSTORE_PROTOCOL)) return url;
		
		return this.alfrescoStorageUrl + '/alfresco/service/' + url.substring(this.APPSTORE_PROTOCOL.length);
		
	}
		
});