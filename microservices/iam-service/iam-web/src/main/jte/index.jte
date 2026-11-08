<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param kh.edu.istad.iam.application.result.UserResult user--%>
<%--@param kh.edu.istad.iam.application.result.UserAuthoritiesResult authorities--%>
<%--@param String pageError--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html>--%>
<%--<html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("profile.title")}</title></head><body><main>--%>
<%--<nav><a href="/">${messages.get("common.home")}</a> | <a href="/profile/edit">${messages.get("common.editProfile")}</a> | <a href="/profile/password">${messages.get("common.password")}</a> | <a href="/profile/accounts">${messages.get("common.linkedAccounts")}</a> | <a href="/profile/apps">${messages.get("common.connectedApps")}</a>--%>
<%--<form method="post" action="/logout">@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }<button>${messages.get("common.logout")}</button></form>--%>
<%--<a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav>--%>
<%--<h1>${messages.get("profile.title")}</h1>--%>
<%--@if (pageError != null) { <p role="alert">${pageError}</p> }--%>
<%--@if (user != null && authorities != null) {--%>
<%--<dl><dt>${messages.get("register.username")}</dt><dd>${user.username()}</dd><dt>${messages.get("profile.email")}</dt><dd>${user.email()}</dd>--%>
<%--<dt>${messages.get("profile.emailVerified")}</dt><dd>${user.emailVerified()}</dd><dt>${messages.get("profile.locale")}</dt><dd>${user.locale()}</dd><dt>${messages.get("profile.enabled")}</dt><dd>${user.enabled()}</dd></dl>--%>
<%--<h2>${messages.get("profile.roles")}</h2><ul>@for (var role : authorities.roles()) { <li>${role.name()}</li> }</ul>--%>
<%--<h2>${messages.get("profile.permissions")}</h2><ul>@for (String permission : authorities.effectivePermissions()) { <li>${permission}</li> }</ul>--%>
<%--}--%>
<%--</main></body></html>--%>



@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.application.result.UserResult user
@param kh.edu.istad.iam.application.result.UserAuthoritiesResult authorities
@param String pageError
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("profile.title")}</title>
</head>
<body>
<main>
    <nav>
        <a href="/">${messages.get("common.home")}</a> |
        <a href="/profile/edit">${messages.get("common.editProfile")}</a> |
        <a href="/profile/password">${messages.get("common.password")}</a> |
        <a href="/profile/accounts">${messages.get("common.linkedAccounts")}</a> |
        <a href="/profile/apps">${messages.get("common.connectedApps")}</a>

        <form method="post" action="/logout">
            @if (csrf != null)
                <input
                        type="hidden"
                        name="${csrf.getParameterName()}"
                        value="${csrf.getToken()}">
            @endif

            <button type="submit">${messages.get("common.logout")}</button>
        </form>

        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("profile.title")}</h1>

    @if (pageError != null)
        <p role="alert">${pageError}</p>
    @endif

    @if (user != null && authorities != null)
        <dl>
            <dt>${messages.get("register.username")}</dt>
            <dd>${user.username()}</dd>

            <dt>${messages.get("profile.email")}</dt>
            <dd>${user.email()}</dd>

            <dt>${messages.get("profile.emailVerified")}</dt>
            <dd>${user.emailVerified()}</dd>

            <dt>${messages.get("profile.locale")}</dt>
            <dd>${user.locale()}</dd>

            <dt>${messages.get("profile.enabled")}</dt>
            <dd>${user.enabled()}</dd>
        </dl>

        <h2>${messages.get("profile.roles")}</h2>
        <ul>
            @for (var role : authorities.roles())
                <li>${role.name()}</li>
            @endfor
        </ul>

        <h2>${messages.get("profile.permissions")}</h2>
        <ul>
            @for (String permission : authorities.effectivePermissions())
                <li>${permission}</li>
            @endfor
        </ul>
    @endif
</main>
</body>
</html>
