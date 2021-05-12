############################################################
# Title .....: ALPS representation of ${fileName}
# Author ...: MDSL Freemarker Generator
# Date .....: tba
############################################################

<#--
# This is an MDSL-to-ALPS demo template provided with MDSL 5.1, author: socadk
# ALPS specification: https://github.com/alps-io/spec/blob/master/draft-07/draft-07.txt 
# ALPS generator tool "unified": https://github.com/mamund/alps-unified
-->

# you can generate OpenAPI and other specifications from ALPS:
# unified -h -f <alpsfile> -t <format type> -o <outfile> (type can be o, p, s; j, a, w)
# [nodemodulesdir]alps-unified\src\index.js -f ${fileName}.alps.yaml -t o -o ${fileName}.oas.yaml

<#assign groupTypes = genModel.dataTypes?filter(o->!(o.isAtomic()))>

<#function convertOperationSemanticsToHTTPVerbProperty responsibility>
<#if responsibility??>
    <#if responsibility="RETRIEVAL_OPERATION">
      <#return "safe">
    <#elseif responsibility="STATE_CREATION_OPERATION">
      <#return "unsafe">
    <#elseif responsibility="STATE_TRANSITION_OPERATION">
      <#return "unsafe">
    <#elseif responsibility="STATE_REPLACEMENT_OPERATION">
      <#return "idempotent">
    <#elseif responsibility="STATE_DELETION_OPERATION">
      <#return "idempotent">
    <#elseif responsibility="COMPUTATION_FUNCTION">
      <#return "safe">
    <#else>
      <#return "unknown responsibility">
    </#if>
<#else>
    <#return "unsafe">
</#if>
</#function>
<#-- TODO work with operation name as well, possibly HTTP binding (if any) too -->
<#function convertOperationSemanticsToCRUDPrimitive responsibility>
<#if responsibility??>
    <#if responsibility="RETRIEVAL_OPERATION">
      <#return "read">
    <#elseif responsibility="STATE_CREATION_OPERATION">
      <#return "create">
    <#elseif responsibility="STATE_TRANSITION_OPERATION">
      <#return "update">
    <#elseif responsibility="STATE_REPLACEMENT_OPERATION">
      <#return "update"> <#-- no "replace" or "patch" in ALPS --> 
    <#elseif responsibility="STATE_DELETION_OPERATION">
      <#return "delete">
    <#elseif responsibility="COMPUTATION_FUNCTION">
      <#return "compute">
    <#else>
      <#return "unknown responsibility">
    </#if>
<#else>
    <#return "">
</#if>
</#function>

<#macro group gtype> 
    - id: ${gtype.name}
      type: group
      text: n/a 
      descriptor:
</#macro>

<#-- 
<#macro idtag id href> 
      - href: '#${href}' # name is ${id}
</#macro>
-->
<#macro idtagForAP id href> 
      - href: '#${href}' # AP, name is ${id}
</#macro>

<#macro idtagForPT id href> 
      - href: '#${href}' # PT, name is ${id}
</#macro>

alps:
  version: '0.1'
  doc:
    value: 'ALPS document for ${genModel.apiName}'

  # metadata <#-- TODO get URIs from spec/calculate them -->
  ext:
    - type: metadata
      name: title
      value: '${genModel.apiName}'
      tags: 'oas'
    - type: metadata
      name: id
      value: http://tbd.tba.tbc
      tags: 'oas'
    - type: metadata
      name: root 
      value: http://tbd.tba.tbc
      tags: 'oas'
  
  descriptor:
    # properties
    # - atomic/flat data elements 
<#list genModel.dataTypes as type>
  <#list type.fields as field>
    <#if field.type.isAtomic()>
    - id: ${field.name}
      type: semantic 
      text: The type of the atomic parameter is ${field.typeAsString} <#-- could add MAP stereotype if present (genModel?) --> 
    </#if>
  </#list>
</#list>

    # groupings
    # - structured/nested data elements
    <#-- open issue: no way to express cardinality/nullable in ALPS; unified seems to always put an array into OAS? -->
<#list groupTypes as type>
  <@group gtype=type></@group>
  <#list type.fields as field>
    <#if field.type.isAtomic()>
      <@idtagForAP href=field.name id= field.typeAsString/>
  <#else>
      <@idtagForPT id=field.name href=field.typeAsString/>
  </#if>
  </#list>

</#list>

    # actions
    # - these are the operations
<#list genModel.endpoints as endpoint>
  <#-- could work with the URIs here to split into several ALPS? -->
  <#list endpoint.operations as operation>

    - id: ${operation.name}
      type: ${convertOperationSemanticsToHTTPVerbProperty(operation.responsibility)}
      tags: ${convertOperationSemanticsToCRUDPrimitive(operation.responsibility)}
      descriptor:
      <#list operation.parameters as parameter>
      - href: '#${parameter.typeAsString}' # parameter name: ${parameter.name}
      </#list>
      rt: '${operation.response.name}'
      text: ${operation.responsibility}
  </#list>            
</#list>

<#-- note: OAS generated by ALPS does not validate for delete (id hardcoded into URI template) -->
