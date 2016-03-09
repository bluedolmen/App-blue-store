name         : ${typeName} Model
groupId      : ${groupId!"org.bluedolmen"}
moduleId     : <#if moduleId?? && (moduleId?trim?length gt 0) >${moduleId}<#else>${typeName}-model</#if>
version      : ${version!"1.0"}
packaging    : alfrescomodel

description  : >
<#if description?? && description?length gt 0>
  ${description}
<#else>
  This module brings the new custom type ${typeName}
</#if>

<#if author??>
author       :
  username   : ${author.username!"BlueDolmen"}
  contact    : ${author.contact!"contact@bluedolmen.org"}
</#if>

cost         : ${cost!"EUR 0"}

<#if license??>
license      : ${license!"license.md"}
</#if>

category     : ${category!"/Business Model"}

<#if tags?? && tags?size gt 0>
tags         :
<#list tags as tag>
<#-- Tags may contain the special sharp character which needs to be escaped by double-quoting tags -->
  - "${tag}"
</#list>
</#if>

<#if requires??>
<#list requires as require>
  - type     : ${require.type}
  - id       : ${require.id}
  - version  : ${require.version}
</#list>
</#if>

