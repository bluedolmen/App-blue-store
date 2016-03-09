<#macro dateFormat date=""><#if date?is_date>${xmldate(date)}</#if></#macro>

<#escape x as jsonUtils.encodeJSONString(x)>
[
	<#list configFiles as file>
	{
		"nodeRef": "${file.nodeRef}", 
		"name": "${file.properties.name}",
		"modifiedOn": "<@dateFormat file.properties.modified />"
	}
	<#if file_has_next>,</#if>
	</#list>
]
</#escape>
