<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param java.util.List<kh.edu.istad.iam.application.result.IdentityResult> identities--%>
<%--@param java.util.List<String> providers--%>
<%--@param String pageError--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("accounts.title")}</title></head><body><main>--%>
<%--<nav><a href="/">${messages.get("common.home")}</a> | <a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav><h1>${messages.get("accounts.title")}</h1>--%>
<%--@if (pageError != null) { <p role="alert">${pageError}</p> }<ul>@for (var identity : identities) { <li><strong>${identity.provider()}</strong> ${messages.get("accounts.email")}: ${identity.email()} ${messages.get("accounts.displayName")}: ${identity.displayName()} ${messages.get("accounts.linkedAt")}: ${identity.linkedAt()}<form method="post" action="/profile/accounts/${identity.uuid()}/unlink">@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }<button>${messages.get("accounts.unlink")}</button></form></li> }</ul>--%>
<%--@if (identities.isEmpty()) { <p>${messages.get("accounts.empty")}</p> }<h2>${messages.get("accounts.link")}</h2><ul>@for (String provider : providers) { <li><a href="/profile/accounts/link/${provider}">${messages.get("accounts.link")} ${messages.get("login." + provider)}</a></li> }</ul></main></body></html>--%>




@param java.util.Map<String, String> messages
@param String lang
@param java.util.List<kh.edu.istad.iam.application.result.IdentityResult> identities
@param java.util.List<String> providers
@param String pageError
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("accounts.title")}</title>
</head>
<body>
<main>
    <nav>
        <a href="/">${messages.get("common.home")}</a> |
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("accounts.title")}</h1>

    @if (pageError != null)
        <p role="alert">${pageError}</p>
    @endif

    @if (identities.isEmpty())
        <p>${messages.get("accounts.empty")}</p>
    @else
        <ul>
            @for (var identity : identities)
                <li>
                    <strong>${identity.provider()}</strong>

                    ${messages.get("accounts.email")}:
                    ${identity.email()}

                    ${messages.get("accounts.displayName")}:
                    ${identity.displayName()}

                    ${messages.get("accounts.linkedAt")}:
                    ${identity.linkedAt()}

                    <form method="post"
                          action="/profile/accounts/${identity.uuid()}/unlink">
                        @if (csrf != null)
                            <input
                                    type="hidden"
                                    name="${csrf.getParameterName()}"
                                    value="${csrf.getToken()}">
                        @endif

                        <button type="submit">
                            ${messages.get("accounts.unlink")}
                        </button>
                    </form>
                </li>
            @endfor
        </ul>
    @endif

    <h2>${messages.get("accounts.link")}</h2>

    <ul>
        @for (String provider : providers)
            <li>
                <a href="/profile/accounts/link/${provider}">
                    ${messages.get("accounts.link")}
                    ${messages.get("login." + provider)}
                </a>
            </li>
        @endfor
    </ul>
</main>
</body>
</html>
