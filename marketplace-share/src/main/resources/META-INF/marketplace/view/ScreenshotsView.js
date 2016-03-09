Ext.define('Marketplace.view.ScreenshotsView', {
	
	extend : 'Ext.window.Window',
	
	urls : [],
	height : 500,
	width : 800,
	
	tpl : new Ext.XTemplate(
		'<ul class="screenshots">',
			'<tpl for=".">',
			'<li><img src="{url}"></img></li>',
			'<li><img src="{url}"></img></li>',
			'</tpl>',
		'</ul>'		
	),
	
//	layout : {
//		type : 'vbox',
//		align : 'stretch'
//	},
	
	layout : 'fit',
	
	items : [
		{
			xtype : 'component',
			itemId : 'screenshots',
			baseCls : 'div',
			cls : 'screenshots',
			id : 'screenshots-container',
			flex : 1
		}
	],
	
	initComponent : function() {
		
		this.on('afterrender', function(window) {
			
			var el = window.queryById('screenshots').getEl();
			this.tpl.overwrite(el, this.urls);
			$('#screenshots-container').bxSlider();
			
		});
		
		this.callParent(arguments);
		
	}
	
	
	
	
});