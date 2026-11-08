<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param kh.edu.istad.iam.web.WebForms.Registration form--%>
<%--@param java.util.List<String> errors--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html>--%>
<%--<html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("register.title")}</title></head><body><main>--%>
<%--<nav><a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav>--%>
<%--<h1>${messages.get("register.title")}</h1>--%>
<%--@for (String error : errors) { <p role="alert">${error}</p> }--%>
<%--<form method="post" action="/register">--%>
<%--@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }--%>
<%--<label>${messages.get("register.username")}<input name="username" value="${form.getUsername()}" required maxlength="50"></label>--%>
<%--<label>${messages.get("register.email")}<input type="email" name="email" value="${form.getEmail()}" required></label>--%>
<%--<label>${messages.get("register.password")}<input type="password" name="password" required minlength="8"></label>--%>
<%--<label>${messages.get("register.confirmPassword")}<input type="password" name="confirmPassword" required minlength="8"></label>--%>
<%--<label>${messages.get("register.givenName")}<input name="givenName" value="${form.getGivenName()}" required></label>--%>
<%--<label>${messages.get("register.familyName")}<input name="familyName" value="${form.getFamilyName()}" required></label>--%>
<%--<label>${messages.get("register.locale")}<select name="locale"><option value="en">${messages.get("common.english")}</option><option value="km">${messages.get("common.khmer")}</option></select></label>--%>
<%--<button type="submit">${messages.get("register.submit")}</button>--%>
<%--</form><a href="/login">${messages.get("register.login")}</a>--%>
<%--</main></body></html>--%>




@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.web.WebForms.Registration form
@param java.util.List<String> errors
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("register.title")}</title>
</head>
<body>
<main>
    <nav>
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("register.title")}</h1>

    @for (String error : errors)
        <p role="alert">${error}</p>
    @endfor

    <form method="post" action="/register">
        @if (csrf != null)
            <input
                    type="hidden"
                    name="${csrf.getParameterName()}"
                    value="${csrf.getToken()}">
        @endif

        <label>
            ${messages.get("register.username")}
            <input
                    type="text"
                    name="username"
                    value="${form.getUsername()}"
                    required
                    maxlength="50">
        </label>

        <label>
            ${messages.get("register.email")}
            <input
                    type="email"
                    name="email"
                    value="${form.getEmail()}"
                    required>
        </label>

        <label>
            ${messages.get("register.password")}
            <input
                    type="password"
                    name="password"
                    required
                    minlength="8">
        </label>

        <label>
            ${messages.get("register.confirmPassword")}
            <input
                    type="password"
                    name="confirmPassword"
                    required
                    minlength="8">
        </label>

        <label>
            ${messages.get("register.givenName")}
            <input
                    type="text"
                    name="givenName"
                    value="${form.getGivenName()}"
                    required>
        </label>

        <label>
            ${messages.get("register.familyName")}
            <input
                    type="text"
                    name="familyName"
                    value="${form.getFamilyName()}"
                    required>
        </label>

        <label>
            ${messages.get("register.locale")}
            <select name="locale">
                <option value="en">
                    ${messages.get("common.english")}
                </option>
                <option value="km">
                    ${messages.get("common.khmer")}
                </option>
            </select>
        </label>

        <button type="submit">
            ${messages.get("register.submit")}
        </button>
    </form>

    <a href="/login">${messages.get("register.login")}</a>
</main>
</body>
</html>

