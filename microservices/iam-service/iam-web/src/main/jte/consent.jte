<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param String clientId--%>
<%--@param String state--%>
<%--@param java.util.List<String> scopes--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("consent.title")}</title></head><body><main>--%>
<%--<nav><a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav><h1>${messages.get("consent.title")}</h1><p>${messages.get("consent.description")}</p><p>${messages.get("consent.client")}: ${clientId}</p><form method="post" action="/oauth2/authorize">--%>
<%--@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }<input type="hidden" name="client_id" value="${clientId}"><input type="hidden" name="state" value="${state}"><fieldset><legend>${messages.get("consent.scopes")}</legend>@for (String scope : scopes) { <label><input type="checkbox" name="scope" value="${scope}" checked>${scope}</label> }</fieldset>--%>
<%--<button name="consent_action" value="authorize">${messages.get("consent.allow")}</button><button name="consent_action" value="cancel">${messages.get("consent.cancel")}</button></form></main></body></html>--%>



@param java.util.Map<String, String> messages
@param String lang
@param String clientId
@param String state
@param java.util.List<String> scopes
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("consent.title")}</title>
</head>
<body>
<main>
    <nav>
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("consent.title")}</h1>

    <p>${messages.get("consent.description")}</p>

    <p>
        ${messages.get("consent.client")}: ${clientId}
    </p>

    <form method="post" action="/oauth2/authorize">
        @if (csrf != null)
            <input
                    type="hidden"
                    name="${csrf.getParameterName()}"
                    value="${csrf.getToken()}">
        @endif

        <input type="hidden" name="client_id" value="${clientId}">
        <input type="hidden" name="state" value="${state}">

        <fieldset>
            <legend>${messages.get("consent.scopes")}</legend>

            @for (String scope : scopes)
                <label>
                    <input
                            type="checkbox"
                            name="scope"
                            value="${scope}"
                            checked>
                    ${scope}
                </label>
            @endfor
        </fieldset>

        <button type="submit" name="consent_action" value="authorize">
            ${messages.get("consent.allow")}
        </button>

        <button type="submit" name="consent_action" value="cancel">
            ${messages.get("consent.cancel")}
        </button>
    </form>
</main>
</body>
</html>
