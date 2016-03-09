<#escape x as jsonUtils.encodeJSONString(x)>
{
	state : "<#if state??>${state}</#if>",
	message : "<#if stateMessage??>${stateMessage}</#if>",
	failureReason : "<#if failureReason??>${failureReason}</#if>" 
}
</#escape>
