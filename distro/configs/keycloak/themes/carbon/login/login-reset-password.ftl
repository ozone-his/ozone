<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <div class="card login-card">
            <div id="kc-locale-div">
                <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                    <div class="${properties.kcLocaleMainClass!}" id="kc-locale">
                        <div id="kc-locale-wrapper" class="${properties.kcLocaleWrapperClass!}">
                            <div id="kc-locale-dropdown" class="${properties.kcLocaleDropDownClass!}">
                                <a href="#" id="kc-current-locale-link">${locale.current}</a>
                                <ul id="dropdown-content" class="${properties.kcLocaleListClass!}">
                                    <#list locale.supported as l>
                                        <li class="${properties.kcLocaleListItemClass!}">
                                            <a class="${properties.kcLocaleItemClass!}" href="${l.url}">${l.label}</a>
                                        </li>
                                    </#list>
                                </ul>
                            </div>
                        </div>
                    </div>
                </#if>
            </div>
            <div class="login-center">
                <img src="${url.resourcesPath}/img/logo.png" class="center-logo" />
            </div>
            <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
                <div class="input-group">
                    <div class="form-item">
                        <label for="username" class="label">
                            <#if !realm.loginWithEmailAllowed>
                                ${msg("username")}
                            <#elseif !realm.registrationEmailAsUsername>
                                ${msg("usernameOrEmail")}
                            <#else>
                                ${msg("email")}
                            </#if>
                        </label>
                        <div class="text-input-field-outer-wrapper">
                            <div class="text-input-field-wrapper">
                                <input
                                    type="text"
                                    id="username"
                                    name="username"
                                    class="${properties.kcInputClass!}"
                                    autofocus
                                    value="${(auth.attemptedUsername!'')}"
                                    aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                    dir="ltr"
                                    placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>"
                                />
                                <#if messagesPerField.existsError('username')>
                                    <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                    </span>
                                </#if>
                            </div>
                        </div>
                        <div class="form-options">
                            <a class="forgot-password-link" href="${url.loginUrl}">
                                ${msg("backToLogin")}
                            </a>
                        </div>
                    </div>
                    <button type="submit" class="continue-button btn-primary">
                        ${msg("doSubmit")}
                        <svg focusable="false" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg" fill="currentColor"
                            aria-label="Next" aria-hidden="true" width="24" height="24" viewBox="0 0 24 24" role="img" class="btn-icon">
                            <path d="M14 4L12.9 5.1 18.9 11.2 2 11.2 2 12.8 18.9 12.8 12.9 18.9 14 20 22 12z"></path>
                        </svg>
                    </button>
                </div>
            </form>
        </div>

    <#elseif section = "info" >
        <#if realm.duplicateEmailsAllowed>
            ${msg("emailInstructionUsername")}
        <#else>
            ${msg("emailInstruction")}
        </#if>
    </#if>
</@layout.registrationLayout>