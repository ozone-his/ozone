<#-- Keycloak's documented extension point for a custom login footer (see keycloak.v2/login/footer.ftl).
     The logo list itself lives in theme.properties ("poweredByLogos") so a child theme can replace
     the whole row by overriding that one property plus its own image files - no template edits. -->
<#macro content>
  <#if properties.poweredByLogos?has_content>
    <p class="ozone-powered-by-text">${msg("poweredBy")}</p>
    <div class="ozone-powered-by-logos">
      <#list properties.poweredByLogos?split(',') as logo>
        <#assign logoFile = logo?trim?split(':')[0]>
        <#assign logoAlt = (logo?trim?split(':')[1])!''>
        <div class="ozone-powered-by-logo">
          <img src="${url.resourcesPath}/img/${logoFile}" alt="${logoAlt}"/>
        </div>
      </#list>
    </div>
  </#if>
</#macro>
