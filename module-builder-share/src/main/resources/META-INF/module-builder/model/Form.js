Ext.define('ModuleBuilder.model.Form', {
	
	extend : 'Ext.data.Model',
//	mixins : ['Ext.data.NodeInterface'],
	
	fields : [
	     { name : 'name', type : 'string' },
	     { name : 'label', type : 'string' },
	     { name : 'type', type : 'string' }
	],
	
	validations : [
	     { type : 'format', field : 'name', matcher: /[a-z][A-Za-z0-9]*/}
    ]
	
});