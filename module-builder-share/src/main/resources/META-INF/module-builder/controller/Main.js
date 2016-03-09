Ext.define('ModuleBuilder.controller.Main', {
	
    extend: 'Ext.app.Controller',
    
    requires: [
        'ModuleBuilder.view.Header',
//        'ModuleBuilder.view.ContentPanel',
        'Ext.window.Window'
    ],
    
    stores: [
	 ],
         
	init: function() {
		
		this.control({
			
			'#new-module-button' : {
				
				click : this.onNewModule
				
			}
			
		});
	
		this.callParent();
		
	},
	
	onNewModule : function() {
		
		var 
			window = Ext.create('ModuleBuilder.view.FormBuilderWindow', {
				modal : true
			})
		;
		
		window.show();
		
	}
    
});
