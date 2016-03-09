Ext.define('Marketplace.view.PaymentWindow', {
	
	extend : 'Ext.window.Window',
	
	title : 'Buy',
	
	height : 150,
	width : 300,
	headerPosition : 'left',
	modal : true,
	
	moduleId : null,
	
	layout : {
		type : 'vbox',
		align : 'center'
	},
	
	defaults : {
		margin : 2
	},
	
	items : [
		{
			xtype : 'label',
			text : 'Choose your payment method:',
			margin : 10
				
		},
		{
			xtype : 'container',
			layout : {
				type : 'hbox',
				aligh : 'stretch',
			},
			height : 48,
			margin : 10,
			defaults : {
				width : 48,
				height : 48
			},
			items : [
		 		{
					xtype : 'image',
					src : Alfresco.constants.URL_RESCONTEXT + 'marketplace/resources/icons/payment/masterCard.png'
				},
		 		{
					xtype : 'image',
					src : Alfresco.constants.URL_RESCONTEXT + 'marketplace/resources/icons/payment/visa.png'
				},
		 		{
					xtype : 'image',
					src : Alfresco.constants.URL_RESCONTEXT + 'marketplace/resources/icons/payment/paypal.png'
				}
			]
		}
	],
	
	initComponent : function() {
		
		this.callParent(arguments);
	}
	
});