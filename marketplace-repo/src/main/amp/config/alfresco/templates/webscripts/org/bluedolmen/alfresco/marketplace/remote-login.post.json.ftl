<#escape x as jsonUtils.encodeJSONString(x)>
{
	"success" : <#if ticket??>true<#else>false</#if>,
	"ticket"  : "${ticket!"#invalid#"}"
}
</#escape>