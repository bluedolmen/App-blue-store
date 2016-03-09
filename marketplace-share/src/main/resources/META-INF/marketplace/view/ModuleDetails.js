Ext.define("Marketplace.view.ModuleDetails", {

	extend : "Ext.Component",
	html : '&nbsp',
	
	xtype : 'moduledetails',
	
	moduleId : null,
	
	tpl : new Ext.XTemplate(
		'<div class="module-details-wrapper">',
			'<div class="presentation">',
				'<div class="title">{name:htmlEncode}</div>',
				'<div class="author">{author:htmlEncode}</div>',
				'<div class="category">{category:htmlEncode}</div>',
				'<div class="cost">{cost:htmlEncode}</div>',
				'<div class="star-rating">',
					'<div class="current-rating" style="width:{rating * 20.}%">',
					'</div>',
				'</div>',
			'</div>',
			
			'<div class="screenshots">',
				'<h1>Screenshots</h1>',
				'<div class="screenshot-images">',
					'<tpl for="screenshots">',
					'<div><img src="{url}"></img></div>',
					'</tpl>',
				'</div>',
			'</div>',

			'<div class="description">',
				'<h1>Description</h1>',				
				'{description}</div>',
			'</div>',
			
			'<div class="comments">',
				'<h1>Comments</h1>',
				'<tpl for="comments">',
					'<div class="comment">{comment}</div>',
				'</tpl>',
			'</div>',		
		'</div>'
	),
	
	initComponent : function() {
		
		var me = this;
		
		if (null == me.moduleId) {
			Ext.Error.raise('The moduleId is not defined correctly');
		}
		
		me.setLoading(true);
		
		var ModuleDetails = Ext.ModelManager.getModel('Marketplace.model.ModuleDetails');
		ModuleDetails.load(me.moduleId, {
			
			callback: function(record, operation, success) {
				if (success) {
					me.tpl.overwrite(me.el, record.getData());
					$('.screenshot-images').slick();
				}
				me.setLoading(false);
		    }
			
		});
		
	}
	
});

