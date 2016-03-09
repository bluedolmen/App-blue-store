<#if mappings??>
<#list mappings as mapping>
${mapping.source}=${mapping.target}
</#list>
</#if>
