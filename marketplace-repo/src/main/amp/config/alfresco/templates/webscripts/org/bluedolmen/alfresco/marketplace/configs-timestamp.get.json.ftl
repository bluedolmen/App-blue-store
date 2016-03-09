<#macro dateFormat date=""><#if date?is_date>${xmldate(date)}</#if></#macro>

<#escape x as jsonUtils.encodeJSONString(x)>
{
	"lastModified" : "<@dateFormat lastModified />"
}
</#escape>
