<#escape x as jsonUtils.encodeJSONString(x)>
{
	"success" : <#if ticket??>true<#else>false</#if>,
	"username": "${username!""}",
	"ticket"  : "${ticket!"#invalid#"}"
}
</#escape>