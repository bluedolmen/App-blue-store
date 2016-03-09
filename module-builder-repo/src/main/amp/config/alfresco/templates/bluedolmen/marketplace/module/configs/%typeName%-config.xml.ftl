<#import "/alfresco/templates/bluedolmen/marketplace/model.lib.ftl" as modellib />
<#macro generateForms>
		<forms>
			<form>
				<#if fields??>
				<field-visibility>
				<#list fields as field>
					<show id="${modellib.nsprefix}:${field.name}" force="true" />
				</#list>
				</field-visibility>
                
				<appearance>
					<#list fields as field>
						<field id="${modellib.nsprefix}:${field.name}" label-id="label.${modellib.nsprefix}_${field.name}">
				    	<#if field.template??>
							<control template="${field.template}" />
						</#if>
						</field>
					</#list>
				</appearance>
				</#if>
			</form>
		</forms>
</#macro>

<alfresco-config>

	<config evaluator="node-type" condition="${modellib.nsprefix}:${typeName}" >
<@generateForms/>
	</config>


	<config evaluator="model-type" condition="${modellib.nsprefix}:${typeName}" >
<@generateForms/>
	</config>

</alfresco-config>
