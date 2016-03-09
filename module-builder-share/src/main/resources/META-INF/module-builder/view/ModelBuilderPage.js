Ext.define('ModuleBuidler.view.ModelBuilderPage', {
	
	extend : 'Ext.form.Panel',
	
	xtype : 'modelbp',
	
	requires : [
        'ModuleBuilder.view.FormBuilder',
        'ModuleBuilder.view.EnumEditor'
	],
	
    fieldDefaults: {
        labelWidth: 100,
        labelStyle: 'font-weight:bold'
    },
    
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
	
	initComponent : function() {
		
		var 
			required = '<span style="color:red;font-weight:bold" data-qtip="Required">*</span>'
		;
		
		this.items = [		 	    
	 		{
	 			xtype : 'textfield',
	 			name: 'typeName',
	 			itemId: 'typeName',
	 			afterLabelTextTpl: required,
	 			fieldLabel: 'Type Name',
	 			allowBlank: false,
	 			validator: function(value) {
	 				return /[a-z][A-Za-z0-9]{2,}/.test(value) ? true : 'A type-name is an alphanumeric string starting with a lowercase letter which length is at least 3';
	 			},
	 			validateOnChange : true
	 		},
	 		
	 	    {
	 	    	xtype : 'form-builder',
	 	    	itemId : 'fieldsTree',
	 	    	title : 'Fields',
	 	    	flex : 2,
	 	    	height : 250,
	 	    	padding : '10 0 0 0' 
	 	    },
	 	    
	 	    {
	 	    	xtype : 'enum-editor',
	 	    	itemId : 'enumEditor',
	 	    	flex : 2,
	 	    	height : 250,
	 	    	padding : '15 0 0 0',
	 	    	listeners : {
	 	    		
	 	    		'enumadd' : function(store, records, index) {
	 	    			
	 	    			var
	 	    				formBuilder = this._getFormBuilder()
	 	    			;
	 	    			
	 	    			if (null == formBuilder || Ext.isEmpty(records)) return;
	 	    			
	 	    			Ext.Array.forEach(records, function(record) {
	 	    				var typeName = record.get('name');
		 	    			formBuilder.addAvailableType(typeName);
	 	    			});
	 	    			
	 	    		},
	 	    		
	 	    		'enumremove' : function(store, record, index, isMove) {
	 	    			
	 	    			var
	 	    				formBuilder = this._getFormBuilder(),
	 	    				typeName = record.get('name')
	 	    			;
	 	    			
	 	    			if (null == formBuilder) return;
	 	    			
	 	    			formBuilder.removeAvailableType(typeName);
	 	    			
	 	    		},
	 	    		
	 	    		'enumupdate' : function(value, originalValue) {
	 	    			
	 	    			var
	 	    				formBuilder = this._getFormBuilder()
	 	    			;
	 	    			
	 	    			if (null == formBuilder) return;
	 	    			
	 	    			formBuilder.updateAvailableType(originalValue, value);
	 	    			
	 	    		},
	 	    		
	 	    		scope : this
	 	    		
	 	    	}
	 	    }
	 	         
	 	];
		
		this.callParent();
		
		this.formBuilder = this.queryById('fieldsTree');
		
	},
	
	getDataModel : function() {
		
		var
			innerForm = this.getForm(),
			formModelData = innerForm ? innerForm.getFieldValues() : {},
			fieldsTree = this.queryById('fieldsTree'),
			fieldsStore = fieldsTree.getStore(),
			rootNode = fieldsStore.getRootNode(),
			fields = [],
			enumEditor = this.queryById('enumEditor')
		;
			
		if (!formModelData.moduleId) {
			formModelData.moduleId = formModelData.typeName + "-model"; 
		}

		// Flatten the tree
		rootNode.cascadeBy(function visit(node) {
			
			if (node.isRoot()) return;
			
			fields.push({
				name : node.get('name'),
				label : node.get('label'),
				type : node.get('type')
			});
			
		});
		
		formModelData.fields = fields;
		
		formModelData.enums = enumEditor.getDataModel();
		
		return formModelData;
		
	},
	
	setDataModel : function(model) {
		
		if (!model) return;

		var
			innerForm = this.getForm(),
			fieldValues = model.fields,
			fieldsTree = this.queryById('fieldsTree'),
			fieldsStore = fieldsTree.getStore(),
			rootNode = fieldsStore.getRootNode(),
			enumEditor = this.queryById('enumEditor')
		;
		
		if (Ext.isArray(fieldValues)) {
			Ext.Array.forEach(fieldValues, function(field) {
				rootNode.appendChild(field);
			});
		}
		
		innerForm.setValues(model);
		
		if (model.enums) {
			enumEditor.setDataModel(model.enums);
		}
		
	},
	
	_getFormBuilder : function() {
		
		return this.formBuilder;
		
	}
	
	
});