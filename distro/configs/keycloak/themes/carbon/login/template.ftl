<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayWide=false>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />

  <#if properties.meta?has_content>
    <#list properties.meta?split(' ') as meta>
      <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
    </#list>
  </#if>

  <title>${msg("loginTitle", (realm.displayName!''))}</title>
  <link rel="icon" href="${url.resourcesPath}/img/faviconV2.png"/>

  <#if properties.styles?has_content>
    <#list properties.styles?split(' ') as style>
      <link href="${url.resourcesPath}/${style}" rel="stylesheet"/>
    </#list>
  </#if>

  <#if properties.scripts?has_content>
    <#list properties.scripts?split(' ') as script>
      <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
    </#list>
  </#if>

  <#if scripts??>
    <#list scripts as script>
      <script src="${script}" type="text/javascript"></script>
    </#list>
  </#if>
</head>
<body>
  <div id="page">
    <div class="login-container">
      <#if displayMessage && message?has_content>
          <#assign messageType = (message.type!'error')?lower_case>
          <#assign notificationClass = "bx--inline-notification--error">
          <#assign notificationTitle = "Error">
          <#if messageType == "warning">
            <#assign notificationClass = "bx--inline-notification--warning">
            <#assign notificationTitle = "Warning">
          <#elseif messageType == "success">
            <#assign notificationClass = "bx--inline-notification--success">
            <#assign notificationTitle = "Success">
          </#if>
          <div class="bx--inline-notification ${notificationClass}" style="width: 23rem">
            <div class="bx--inline-notification__details">
              <#if messageType == "error">
                <svg focusable="false" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg" fill="currentColor" aria-hidden="true" width="20" height="20" viewBox="0 0 20 20" class="bx--inline-notification__icon">
                  <path d="M10,1c-5,0-9,4-9,9s4,9,9,9s9-4,9-9S15,1,10,1z M13.5,14.5l-8-8l1-1l8,8L13.5,14.5z"></path>
                  <path d="M13.5,14.5l-8-8l1-1l8,8L13.5,14.5z" data-icon-path="inner-path" opacity="0"></path>
                </svg>
              <#elseif messageType == "warning">
                <svg focusable="false" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg" fill="currentColor" aria-hidden="true" width="20" height="20" viewBox="0 0 20 20" class="bx--inline-notification__icon">
                  <path d="M10,1c-5,0-9,4-9,9s4,9,9,9s9-4,9-9S15,1,10,1z M9.2,6.1h1.6v5.4H9.2V6.1z M10,13.3c-0.6,0-1.1-0.5-1.1-1.1s0.5-1.1,1.1-1.1s1.1,0.5,1.1,1.1S10.6,13.3,10,13.3z"></path>
                </svg>
              <#else>
                <svg focusable="false" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg" fill="currentColor" aria-hidden="true" width="20" height="20" viewBox="0 0 20 20" class="bx--inline-notification__icon">
                  <path d="M10,1c-5,0-9,4-9,9s4,9,9,9s9-4,9-9S15,1,10,1z M8.5,14.5l-3-3l1-1l2,2l5-5l1,1L8.5,14.5z"></path>
                </svg>
              </#if>
              <div class="bx--inline-notification__text-wrapper">
                <p class="bx--inline-notification__title">${notificationTitle}</p>
                <div class="bx--inline-notification__subtitle">${kcSanitize(message.summary)?no_esc}</div>
              </div>
            </div>
            <button type="button" class="bx--inline-notification__close-button">
                <svg focusable="false" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg" fill="currentColor" aria-hidden="true" width="20" height="20" viewBox="0 0 32 32" class="bx--inline-notification__close-icon"><!----><path d="M24 9.4L22.6 8 16 14.6 9.4 8 8 9.4 14.6 16 8 22.6 9.4 24 16 17.4 22.6 24 24 22.6 17.4 16 24 9.4z"></path></svg>
            </button>
          </div>
        </#if>
      <#nested "form">
    </div>
  </div>
</body>
</html>
</#macro>
