Ext.define('Marketplace.view.MainSearchCombo', {
	
	extend : 'Ext.form.field.ComboBox',
	
	xtype : 'mainsearchcombo',
	
//    store: ds,
    displayField: 'description',
    typeAhead: false,
    hideLabel: true,
//    hideTrigger:true,
//    anchor: '100%',

    triggerCls : 'x-form-search-trigger',
    
    listConfig: {
    	
        loadingText: 'Searching...',
        emptyText: 'No matching modules found.'

//        getInnerTpl: function() {
//            return '<a class="search-item" href="http://www.sencha.com/forum/showthread.php?t={topicId}&p={id}">' +
//                '<h3><span>{[Ext.Date.format(values.lastPost, "M j, Y")]}<br />by {author}</span>{title}</h3>' +
//                '{excerpt}' +
//            '</a>';
//        }

    },
    
    pageSize: 10,
    
    initComponent : function() {
    	
		this.store = Ext.create('Marketplace.store.Modules');
		this.callParent();
    	
    }
    
    
});