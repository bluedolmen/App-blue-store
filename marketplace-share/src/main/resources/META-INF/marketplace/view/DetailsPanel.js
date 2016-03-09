Ext.define('Marketplace.view.DetailsPanel', {
	
	extend : 'Ext.panel.Panel',
	xtype : 'detailspanel',
	
	requires : [
		'Marketplace.view.ScreenshotsView'
	],
	
	title : 'Module Details',
	
	tpl : new Ext.XTemplate(
		'<div class="module-details-wrapper">',
			'<div class="presentation">', 
				'<ul class="categories">', // has to be first
					'<li class="category"><a href="#">{[this.getCategory(values.category, values.packaging)]}</a></li>',
				'</ul>',
				'<div class="appLogo"><img src="{[this.resolveAppStoreProtocol(values.logoUrl)]}" title="{id:htmlEncode}"></div>',
				'<div class="title">{name:htmlEncode}</div>',
				'<div class="appAuthor">{author:htmlEncode}</div>',
				'<div class="latestVersion">{latestVersion}</div>',
				'<ul class="tags">', // has to be last
				'<tpl for="tags">',
				'<li><a href="#">{.}</a></li>',
				'</tpl>',
				'</ul>',
				'<span class="clear"></span>',
			'</div>',
			
			'<div class="screenshots">',
				'<h1>Screenshots</h1>',
				'<div class="screenshot-images">',
					'<tpl for="screenshots">',
					'<a href="{[this.resolveAppStoreProtocol(values.url)]}" data-lightbox="details-screenshots"><img src="{[this.resolveAppStoreProtocol(values.url)]}" alt="{id}"></img></a>',
					'</tpl>',
				'</div>',
			'</div>',

			'<div class="description">',
				'<h1>Description</h1>',				
				'<div>{[this.toHtml(values.description)]}</div>',
			'</div>',
			
			'<div class="comments">',
				'<h1>Comments</h1>',
				'<tpl for="comments">',
					'<div class="comment">',
						'<div class="avatar"><img src="{[this.getAvatarUrl(values.avatarUrl)]}" width="64" ></img></div>',
						'<div class="author">{author}</div>',
						'<div class="content">{content}</div>',
						'<div class="published">' +
							'<span class="created">' + i18n.t('mp:details.published-on') + ' {created:date("' + i18n.t('mp:details.date-format') + '")}</span>' + 
							'<tpl if="isModified"><span class="modified"> (' + i18n.t('mp:details.modified-on') + ' {modified:date("' + i18n.t('mp:details.date-format') + '")})</span></tpl>' + 
						'</div>',
						'<span class="clear"></span>',
					'</div>',
				'</tpl>',
			'</div>',		
		'</div>',
		{
			toHtml : function(text) {
				return markdown.toHTML(text);
			},
			isModified : function() {
				return false; //TODO: implement
			},
			getAvatarUrl : function(avatarUrl) {
				if (!avatarUrl) return Alfresco.constants.URL_RESCONTEXT + 'components/images/no-user-photo-64.png';
				return avatarUrl;
			},
			getCategory : function(category, packaging) {
				if (category) return category;
				return i18n.t('mp:packaging.' + packaging);
			},
			resolveAppStoreProtocol : function(value) {
				
				value = StorageUtils.resolveAppStoreProtocol(value);
				value = StorageUtils.authenticatedUrl(value);
				return value;

			}
		}
	),
	
	layout : {
		type : 'fit'
	},
	
	items : [
		{
			xtype : 'component',
			itemId : 'content',
			baseCls : 'div',
			cls : 'module-details-wrapper',
			overflowY : 'auto',
		}
	],

	showDetails : function(record) {
		
		this.tpl.overwrite(this.queryById('content').getEl(), record.getData());
//		$('.screenshot-images').bxSlider();
//		var 
//			screenshots = Ext.get(this.el.query('.screenshots')[0])
//		;
//		if (screenshots) {
//			screenshots.on('click', function() {
//				
//				console.log('Screenshots clicked.');
//				
//				Ext.create('Marketplace.view.ScreenshotsView', {
//					renderTo : Ext.getBody(),
//					urls : record.get('screenshots')
//				}).show();
//				
//			});
//		}
		
		
	}
	
});