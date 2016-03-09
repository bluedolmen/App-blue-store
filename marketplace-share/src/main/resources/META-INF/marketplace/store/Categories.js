Ext.define('Marketplace.store.Categories', {
	
	extend : 'Ext.data.JsonStore',
	
	storeId : 'categories',
	
	fields : [
	    "id",
		"color",
		"icon"
	],
	
	data : [
		{
			id : i18n.t("mp:categories.business-models"),
			"color" : "orange",
			"icon" : ""
		}
	]
	
});