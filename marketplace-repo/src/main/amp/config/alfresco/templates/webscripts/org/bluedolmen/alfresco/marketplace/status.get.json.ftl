<#import "/org/bluedolmen/alfresco/utils/item.lib.ftl" as itemLib />
<#escape x as jsonUtils.encodeJSONString(x)>
<@itemLib.renderValue status /> 
</#escape>
