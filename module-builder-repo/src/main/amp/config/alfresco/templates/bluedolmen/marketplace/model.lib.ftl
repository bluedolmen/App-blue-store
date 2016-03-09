<#macro renderType value>
<@compress single_line=true>
<#if value == "text">
d:text
<#elseif value == "boolean">
d:boolean
<#elseif value == "number">
d:long
<#elseif value == "date">
d:date
<#elseif value == "datetime">
d:datetime
<#else>
d:text
</#if>
</@compress>
</#macro>

<#function isStaticEnum typeName>
  <#list enums as enum><#if enum.name = typeName><#return !enum.dynamic></#if></#list>
  <#return false>
</#function>

<#function getEnumDefinition typeName>
  <#list enums as enum><#if enum.name = typeName><#return enum></#if></#list>
  <#return null>
</#function>

<#function getDefaultValue enum>
  <#if enum.default??><#return enum.default></#if>
  <#if enum.values?size = 0><#return ""></#if>
  <#return enum.values[0]>
</#function>

<#macro join array sep>
<@compress single_line=true>
<#list array as elem>${elem}<#if elem_has_next>${sep}</#if></#list>
</@compress>
</#macro>

<#assign wwwsuffix=(groupId!"org.bluedolmen")?split(".")?reverse />
<#assign nsuri>http://www.<@join wwwsuffix "."/>/model/${typeName?lower_case}/1.0</#assign>
<#assign nsprefix=nsPrefix!(typeName?lower_case) />
