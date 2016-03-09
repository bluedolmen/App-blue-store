<#import "/org/bluedolmen/alfresco/utils/item.lib.ftl" as itemLib />
<#escape x as jsonUtils.encodeJSONString(x)>
{
	resources : <@itemLib.renderValue resources /> 
}
</#escape>
