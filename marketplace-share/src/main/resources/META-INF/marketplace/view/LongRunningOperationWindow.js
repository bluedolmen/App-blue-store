Ext.define('Marketplace.view.LongRunningOperationWindow', {
	
	extend : 'Ext.window.Window',
	
	requires : [
		'Ext.form.Label',
		'Marketplace.util.Alfresco'
	],
	
	URL_OPERATION : Alfresco.constants.PROXY_URI_RELATIVE + 'bluedolmen/mp/{operation}/{moduleId}',
	URL_OPERATION_STATE : Alfresco.constants.PROXY_URI_RELATIVE + 'bluedolmen/mp/{operation}/{moduleId}/state',
	STATE_UNAVAILABLE : "UNAVAILABLE",
	STATE_PREPARING : 'PREPARING',
	STATE_STARTED : 'STARTED',
	STATE_FAILURE : 'FAILURE',
	STATE_SUCCESS : 'SUCCESS',
	
	ICON_CLS_MAP : {
		'UNAVAILABLE' : 'icon-coffee',
		'PREPARING' : 'icon-walk',
		'DOWNLOADING' : 'icon-inbox',
		'PROCESSING' : 'icon-gear',
		'SUCCESS' : 'icon-flags',
		'FAILURE' : 'icon-weather'
	},
	
//	iconCls : 'icon-install',
	
	/**
	 * @required
	 */
	operation : null, 
	
	height : 300,
	width : 500,
	headerPosition : 'left',
	closable : false,
	modal : true,
	
	moduleId : null,
	
	layout : {
		type : 'vbox',
		align : 'stretch'
	},
	
	defaults : {
		margin : 2
	},
	
	items : [
		{
			xtype : 'label',
			itemId : 'state-label',
			padding : '2 2 2 40',
			componentCls : 'installation-state-label',
			height : 32,
			style : {
				'background-repeat' : 'no-repeat',
				'font-size' : '16px',
				'line-height' : '32px'
			}
		},
		{
			xtype : 'displayfield',
			itemId : 'state-details',
			flex : 1,
			padding : '2'
		}
	],
	
	dockedItems: [],
	
	initComponent : function() {
		
		var me = this;
		
		if (!this.operation) {
			Ext.Error.raise('IllegalStateException! The operation is a mandatory property.');
		}
		
		this.title = i18n.t('mp:window.' + this.operation + '.title'),
		
		this.dockedItems = [
			{
			    xtype: 'toolbar',
			    dock: 'bottom',
			    items: [
		            {
		            	xtype : 'label',
		            	padding : '2 2 2 40',
		            	text : i18n.t('mp:window.long-running.about-to') + ' ' + i18n.t('mp:operation-label.' + this.operation),
		            	componentCls : 'label.installation-state-label ' + 'icon-' + this.operation,
		    			height : 32,
		    			style : {
		    				'background-repeat' : 'no-repeat',
		    				'font-size' : '16px',
		    				'line-height' : '32px'
		    			}		            	
		            },
					'->',
					{
						xtype : 'button',
						itemId : 'launch-button',
						text : i18n.t('mp:window.button.launch'),
						disabled : false,
						handler : function() {
							me.launch(me.moduleId);
						}
					},
					{
						xtype : 'button',
						itemId : 'close-button',
						text : i18n.t('mp:window.button.close'),
						disabled : true,
						handler : function() {
							me.close();
						}
					}
			    ]
			}		                    
		];
		
		this.callParent(arguments);
		
		me.on('state-available', function(status) {
			
			if (status.exception || me.STATE_FAILURE == status.state) {
				me.setFailure(status.status ? status.status.description : status.message, status.failureReason || status.message);
			}
			else if (me.STATE_SUCCESS == status.state) {
				me.setSuccess();
			}
			else {
				me.setStateLabel(status.state, status.message);
			}
			
		});
		
	},
	
	prepare : function(moduleId, title, options) {
		this.moduleId = moduleId;
		if (title) {
			this.setTitle(title);
		}
		this.options = options || {};
		this.clearStateLabel();
		this.setLaunchButtonDisabled(false);
		this.setCloseButtonDisabled(false);
	},
	
	launch : function(moduleId) {
		
		if (!moduleId) {
			Ext.Error.raise('The moduleId is not valid');
		}
		
		this.moduleId = moduleId;
		
    	var 
    		me = this,
			url = this.URL_OPERATION
				.replace(/\{moduleId\}/, this.moduleId)
				.replace(/\{operation\}/, this.operation),
			headers = Marketplace.util.Alfresco.getAlfrescoHeaders()
		;
		
		me.setStateLabel("UNAVAILABLE");
		me.setCloseButtonDisabled(true);
		me.setLaunchButtonDisabled(true);
		
    	Ext.Ajax.request({
    		
			url : url,
			method : 'POST',
			
			jsonData : me.options,
			headers : headers,
			
			success : function(response) {
				me.setStateLabel(me.STATE_STARTED);
				me.startStateUpdateTimer();
			},
			
			failure : function(response) {
				me.setFailure(response);
			}
			
		});
		
	},
	
	startStateUpdateTimer : function() {
		
		if (null != this.stateUpdateTimer) {
			Ext.util.TaskManager.stop(this.stateUpdateTimer); // should not happen
		}

		this.stateUpdateTimer = Ext.util.TaskManager.start({
			run : this.requestState,
			scope : this,
			interval : 500, /* ms */
		});
		
	},
	
	stopStateUpdateTimer : function() {
		
		if (null == this.stateUpdateTimer) return;
		Ext.util.TaskManager.stop(this.stateUpdateTimer);
		this.stateUpdateTimer = null;
		
	},
	
	requestState : function() {
		
		if (null == this.moduleId) return;
		
    	var 
    		me = this,
    		url = this.URL_OPERATION_STATE
    			.replace(/\{moduleId\}/, this.moduleId)
    			.replace(/\{operation\}/, this.operation)
    	;
    	
    	Ext.Ajax.request({
    		
			url: url,
			method : 'GET',
			
			success: function(response) {
				
			    var 
			    	text = response.responseText || '{}',
			    	jsonResponse = Ext.JSON.decode(text, true /* safe */)
			    ;
			   
			    me.fireEvent('state-available', jsonResponse);
			},
			
			failure : function(response) {
				if (!me.moduleId) return;
				Ext.Error.raise('Cannot get the state of deployment for module \'' + me.moduleId + '\': ' + response);
			}
			
		});
		
		
	},
	
	setSuccess : function() {
		this.setStateLabel(this.STATE_SUCCESS);
		this.setFinished();
		this.fireEvent('success', this);
	},
	
	setFailure : function(reason, details) {
		if (!Ext.isString(reason)) {
			reason = reason.statusText || i18n.t("mp:window.long-running.unknown-internal-error");
		}
		if (!details) {
			details = '';
		}
		
		this.setStateLabel(this.STATE_FAILURE, reason + '<br>' + details);
		this.setFinished();
		this.fireEvent('failure', this, reason, details);
	},
	
	setFinished : function() {
		this.stopStateUpdateTimer();
		this.setLoading(false);
		this.setCloseButtonDisabled(false);
	},
	
	setCloseButtonDisabled : function(disabled) {
		var closeButton = this.queryById('close-button');
		closeButton.setDisabled(disabled);
	},
	
	setLaunchButtonDisabled : function(disabled) {
		var launchButton = this.queryById('launch-button');
		launchButton.setDisabled(disabled);
	},
	
	setStateLabel : function(state, details) {
		
		var 
			stateLabel = this.queryById('state-label'),
			stateDetails = this.queryById('state-details'),
			stateText = i18n.t('mp:window.long-running.state.' + state),
			stateIconCls = this.ICON_CLS_MAP[state]
		;
		
		if (null != stateIconCls) {
			if (null != this.currentStateCls) {
				stateLabel.removeCls(this.currentStateCls);
			}
			stateLabel.addCls(stateIconCls);
			this.currentStateCls = stateIconCls;
		}
		
		stateLabel.setText(stateText);
		stateDetails.setValue(details);
		
	},
	
	clearStateLabel : function() {
		
		var 
			stateLabel = this.queryById('state-label'),
			stateDetails = this.queryById('state-details')
		;
		
		if (null != this.currentStateCls) {
			stateLabel.removeCls(this.currentStateCls);
			this.currentStateCls = null;
		}
		
		stateLabel.setText('');
		stateDetails.setValue('');
		
	}
	
});