<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param java.util.List<kh.edu.istad.iam.application.result.ConnectedApplicationResult> apps--%>
<%--@param String pageError--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("apps.title")}</title></head><body><main>--%>
<%--<nav><a href="/">${messages.get("common.home")}</a> | <a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav><h1>${messages.get("apps.title")}</h1>@if (pageError != null) { <p role="alert">${pageError}</p> }--%>
<%--<ul>@for (var app : apps) { <li><strong>${app.clientName()}</strong> ${messages.get("apps.scopes")}: ${app.scopes()}<form method="post" action="/profile/apps/${app.clientId()}/revoke">@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }<button>${messages.get("apps.revoke")}</button></form></li> }</ul>@if (apps.isEmpty()) { <p>${messages.get("apps.empty")}</p> }</main></body></html>--%>




@param java.util.Map<String, String> messages
@param String lang
@param java.util.List<kh.edu.istad.iam.application.result.ConnectedApplicationResult> apps
@param String pageError
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("apps.title")}</title>
</head>
<body>
<main>
    <nav>
        <a href="/">${messages.get("common.home")}</a> |
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("apps.title")}</h1>

    @if (pageError != null)
        <p role="alert">${pageError}</p>
    @endif

    @if (apps.isEmpty())
        <p>${messages.get("apps.empty")}</p>
    @else
        <ul>
            @for (var app : apps)
                <li>
                    <strong>${app.clientName()}</strong>

                    <p>${messages.get("apps.scopes")}:</p>

                    <ul>
                        @for (String scope : app.scopes())
                            <li>${scope}</li>
                        @endfor
                    </ul>

                    <form method="post"
                          action="/profile/apps/${app.clientId()}/revoke">

                        @if (csrf != null)
                            <input
                                    type="hidden"
                                    name="${csrf.getParameterName()}"
                                    value="${csrf.getToken()}">
                        @endif

                        <button type="submit">
                            ${messages.get("apps.revoke")}
                        </button>
                    </form>
                </li>
            @endfor
        </ul>
    @endif
</main>
</body>
</html>
