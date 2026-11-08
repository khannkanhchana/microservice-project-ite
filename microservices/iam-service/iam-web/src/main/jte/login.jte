<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param boolean hasError--%>
<%--@param boolean registered--%>
<%--@param java.util.List<String> socialProviders--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html>--%>
<%--<html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("login.title")}</title></head>--%>
<%--<body><main>--%>
<%--<nav><span>${messages.get("common.language")}</span> <a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav>--%>
<%--<h1>${messages.get("login.title")}</h1>--%>
<%--@if (hasError) { <p role="alert">${messages.get("login.invalid")}</p> }--%>
<%--@if (registered) { <p role="status">${messages.get("login.registered")}</p> }--%>
<%--<form method="post" action="/login">--%>
<%--@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }--%>
<%--<label>${messages.get("login.username")}<input name="username" autocomplete="username" required></label>--%>
<%--<label>${messages.get("login.password")}<input type="password" name="password" autocomplete="current-password" required></label>--%>
<%--<button type="submit">${messages.get("login.submit")}</button>--%>
<%--</form>--%>
<%--<h2>${messages.get("login.continueWith")}</h2>--%>
<%--<ul>@for (String provider : socialProviders) { <li><a href="/oauth2/authorization/${provider}">${messages.get("login." + provider)}</a></li> }</ul>--%>
<%--<a href="/register">${messages.get("login.register")}</a>--%>
<%--</main></body></html>--%>




@param java.util.Map<String, String> messages
@param String lang
@param boolean hasError
@param boolean registered
@param java.util.List<String> socialProviders
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("login.title")}</title>
</head>
<body>
<main>
    <nav>
        <span>${messages.get("common.language")}</span>
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("login.title")}</h1>

    @if (hasError)
        <p role="alert">${messages.get("login.invalid")}</p>
    @endif

    @if (registered)
        <p role="status">${messages.get("login.registered")}</p>
    @endif

    <form method="post" action="/login">
        @if (csrf != null)
            <input
                    type="hidden"
                    name="${csrf.getParameterName()}"
                    value="${csrf.getToken()}">
        @endif

        <label>
            ${messages.get("login.username")}
            <input
                    type="text"
                    name="username"
                    autocomplete="username"
                    required>
        </label>

        <label>
            ${messages.get("login.password")}
            <input
                    type="password"
                    name="password"
                    autocomplete="current-password"
                    required>
        </label>

        <button type="submit">
            ${messages.get("login.submit")}
        </button>
    </form>

    <h2>${messages.get("login.continueWith")}</h2>

    <ul>
        @for (String provider : socialProviders)
            <li>
                <a href="/oauth2/authorization/${provider}">
                    ${messages.get("login." + provider)}
                </a>
            </li>
        @endfor
    </ul>

    <a href="/register">${messages.get("login.register")}</a>
</main>
</body>
</html>
