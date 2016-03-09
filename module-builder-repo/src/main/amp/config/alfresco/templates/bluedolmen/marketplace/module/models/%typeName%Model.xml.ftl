<#import "/alfresco/templates/bluedolmen/marketplace/model.lib.ftl" as modellib />
<?xml version="1.0" encoding="UTF-8"?>
<!-- xsi:schemaLocation="http://www.alfresco.org/model/dictionary/1.0 modelSchema.xsd" -->
<model name="${modellib.nsprefix}:${typeName}Model" 
       xmlns="http://www.alfresco.org/model/dictionary/1.0" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

	<description>BlueDolmen Custom Model for customized type '${typeName}'</description>
	<author>BlueDolmen</author>
	<published>${.now?date?iso_utc}</published>
	<version>${version!"1.0"}</version>

	<imports>
		<import uri="http://www.alfresco.org/model/dictionary/1.0" prefix="d"/>
		<import uri="http://www.alfresco.org/model/system/1.0" prefix="sys"/>
		<import uri="http://www.alfresco.org/model/content/1.0" prefix="cm"/>
		<import uri="http://www.alfresco.org/model/datalist/1.0" prefix="dl"/>
	</imports>

	<namespaces>
		<namespace uri="${modellib.nsuri}" prefix="${modellib.nsprefix}"/>
	</namespaces>
	
	<constraints>
	<#if enums??>
	<#list enums as enum><#if enum.dynamic?? && !enum.dynamic >
		<constraint name="${modellib.nsprefix}:${enum.name}" type="LIST">
			<parameter name="allowedValues">
				<list>
				<#list enum.values as value>
					<value>${value}</value>
				</#list>
				</list>
			</parameter>
		</constraint>
	</#if></#list>
	</#if>
	</constraints>

	<types>
	
		<type name="${modellib.nsprefix}:${typeName}">
			<title>${title!"Custom Type ${typeName}"}</title>
			<#if (contentLess!false) >
			<parent>cm:cmobject</parent>
			<#else>
			<parent>cm:content</parent>
			</#if>
			<archive>${archive!"true"}</archive>
			<#if fields??>
			<properties>
				<#list fields as field>
				<property name="${modellib.nsprefix}:${field.name}">
					<type><@modellib.renderType (field.type!"string") /></type>
					<#if field.mandatory?? && "true" == field.mandatory>
					<mandatory>${field.mandatory!"false"}</mandatory>
					</#if>
					<#assign enumDef = modellib.getEnumDefinition(field.type)!"" />
					<#assign typeIsStaticEnum = modellib.isStaticEnum (field.type) />
					<#if typeIsStaticEnum >
					<default>${modellib.getDefaultValue(enumDef)}</default>
					</#if>
					<#if field.index??>
					<index enabled="${field.index.enabled}">
						<atomic>${field.index.atomic}</atomic>
						<stored>${field.index.stored}</stored>
						<tokenised>${field.index.tokenized}</tokenised>
					</index>
					<#else>
					<index enabled="true">
						<atomic>true</atomic>
						<stored>false</stored>
						<tokenised>true</tokenised>
					</index>
					</#if>
					<#if typeIsStaticEnum >
					<constraints>
						<constraint ref="${modellib.nsprefix}:${enumDef.name}" />
					</constraints>
					</#if>
				</property>
				</#list>
			</properties>
			</#if>
		</type>
		
	</types>

</model>
