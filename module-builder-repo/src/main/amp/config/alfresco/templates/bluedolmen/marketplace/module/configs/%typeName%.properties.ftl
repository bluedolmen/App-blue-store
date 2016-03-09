<#import "/alfresco/templates/bluedolmen/marketplace/model.lib.ftl" as modellib />
<#if fields??>
<#list fields as field>
<#if field.label??>
label.${modellib.nsprefix}_${field.name}=${propsescape(field.label)?replace("'","''")}
</#if>
</#list>
</#if>