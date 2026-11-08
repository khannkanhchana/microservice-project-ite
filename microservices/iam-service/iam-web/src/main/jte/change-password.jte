<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param kh.edu.istad.iam.web.WebForms.Password form--%>
<%--@param java.util.List<String> errors--%>
<%--@param String success--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("password.title")}</title></head><body><main>--%>
<%--<nav><a href="/">${messages.get("common.home")}</a> | <a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav><h1>${messages.get("password.title")}</h1>--%>
<%--@if (success != null) { <p role="status">${success}</p> }@for (String error : errors) { <p role="alert">${error}</p> }--%>
<%--<form method="post" action="/profile/password">@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }--%>
<%--<label>${messages.get("password.current")}<input type="password" name="currentPassword" required></label><label>${messages.get("password.new")}<input type="password" name="newPassword" minlength="8" required></label><label>${messages.get("password.confirm")}<input type="password" name="confirmPassword" minlength="8" required></label><button>${messages.get("password.submit")}</button></form></main></body></html>--%>


@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.web.WebForms.Password form
@param java.util.List<String> errors
@param String success
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("password.title")}</title>
</head>
<body>
<main>
    <nav>
        <a href="/">${messages.get("common.home")}</a> |
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("password.title")}</h1>

    @if (success != null)
        <p role="status">${success}</p>
    @endif

    @for (String error : errors)
        <p role="alert">${error}</p>
    @endfor

    <form method="post" action="/profile/password">
        @if (csrf != null)
            <input
                    type="hidden"
                    name="${csrf.getParameterName()}"
                    value="${csrf.getToken()}"
            >
        @endif

        <label>
            ${messages.get("password.current")}
            <input type="password" name="currentPassword" required>
        </label>

        <label>
            ${messages.get("password.new")}
            <input type="password" name="newPassword" minlength="8" required>
        </label>

        <label>
            ${messages.get("password.confirm")}
            <input type="password" name="confirmPassword" minlength="8" required>
        </label>

        <button type="submit">${messages.get("password.submit")}</button>
    </form>
</main>
</body>
</html>