<%--@param java.util.Map<String, String> messages--%>
<%--@param String lang--%>
<%--@param kh.edu.istad.iam.web.WebForms.Profile form--%>
<%--@param java.util.List<String> errors--%>
<%--@param String pageError--%>
<%--@param org.springframework.security.web.csrf.CsrfToken csrf--%>
<%--<!doctype html><html lang="${lang}"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${messages.get("profile.editTitle")}</title></head><body><main>--%>
<%--<nav><a href="/">${messages.get("common.home")}</a> | <a href="?lang=en">${messages.get("common.english")}</a> | <a href="?lang=km">${messages.get("common.khmer")}</a></nav>--%>
<%--<h1>${messages.get("profile.editTitle")}</h1>@if (pageError != null) { <p role="alert">${pageError}</p> }@for (String error : errors) { <p role="alert">${error}</p> }--%>
<%--<form method="post" action="/profile/edit">--%>
<%--@if (csrf != null) { <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}"> }--%>
<%--<label>${messages.get("profile.givenName")}<input name="givenName" value="${form.getGivenName()}"></label><label>${messages.get("profile.familyName")}<input name="familyName" value="${form.getFamilyName()}"></label>--%>
<%--<label>${messages.get("profile.middleName")}<input name="middleName" value="${form.getMiddleName()}"></label><label>${messages.get("profile.nickname")}<input name="nickname" value="${form.getNickname()}"></label>--%>
<%--<label>${messages.get("profile.gender")}<input name="gender" value="${form.getGender()}"></label><label>${messages.get("profile.birthdate")}<input type="date" name="birthdate" value="${form.getBirthdate()}"></label>--%>
<%--<label>${messages.get("profile.locale")}<select name="locale"><option value="en">${messages.get("common.english")}</option><option value="km">${messages.get("common.khmer")}</option></select></label>--%>
<%--<label>${messages.get("profile.phoneNumber")}<input name="phoneNumber" value="${form.getPhoneNumber()}"></label><label>${messages.get("profile.zoneinfo")}<input name="zoneinfo" value="${form.getZoneinfo()}"></label>--%>
<%--<label>${messages.get("profile.picture")}<input name="pictureUrl" value="${form.getPictureUrl()}"></label><label>${messages.get("profile.website")}<input name="websiteUrl" value="${form.getWebsiteUrl()}"></label>--%>
<%--<fieldset><legend>${messages.get("profile.address")}</legend><label>${messages.get("profile.streetAddress")}<input name="addressStreetAddress" value="${form.getAddressStreetAddress()}"></label><label>${messages.get("profile.locality")}<input name="addressLocality" value="${form.getAddressLocality()}"></label><label>${messages.get("profile.region")}<input name="addressRegion" value="${form.getAddressRegion()}"></label><label>${messages.get("profile.postalCode")}<input name="addressPostalCode" value="${form.getAddressPostalCode()}"></label><label>${messages.get("profile.country")}<input name="addressCountry" value="${form.getAddressCountry()}"></label></fieldset>--%>
<%--<button>${messages.get("common.save")}</button><a href="/">${messages.get("common.cancel")}</a></form></main></body></html>--%>



@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.web.WebForms.Profile form
@param java.util.List<String> errors
@param String pageError
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${messages.get("profile.editTitle")}</title>
</head>
<body>
<main>
    <nav>
        <a href="/">${messages.get("common.home")}</a> |
        <a href="?lang=en">${messages.get("common.english")}</a> |
        <a href="?lang=km">${messages.get("common.khmer")}</a>
    </nav>

    <h1>${messages.get("profile.editTitle")}</h1>

    @if (pageError != null)
        <p role="alert">${pageError}</p>
    @endif

    @for (String error : errors)
        <p role="alert">${error}</p>
    @endfor

    <form method="post" action="/profile/edit">
        @if (csrf != null)
            <input
                    type="hidden"
                    name="${csrf.getParameterName()}"
                    value="${csrf.getToken()}">
        @endif

        <label>
            ${messages.get("profile.givenName")}
            <input name="givenName" value="${form.getGivenName()}">
        </label>

        <label>
            ${messages.get("profile.familyName")}
            <input name="familyName" value="${form.getFamilyName()}">
        </label>

        <label>
            ${messages.get("profile.middleName")}
            <input name="middleName" value="${form.getMiddleName()}">
        </label>

        <label>
            ${messages.get("profile.nickname")}
            <input name="nickname" value="${form.getNickname()}">
        </label>

        <label>
            ${messages.get("profile.gender")}
            <input name="gender" value="${form.getGender()}">
        </label>

        <label>
            ${messages.get("profile.birthdate")}
            <input type="date" name="birthdate" value="${form.getBirthdate()}">
        </label>

        <label>
            ${messages.get("profile.locale")}
            <select name="locale">
                <option value="en" selected="${form.getLocale() == null || form.getLocale().equals("en")}">
                    ${messages.get("common.english")}
                </option>
                <option value="km" selected="${form.getLocale() != null && form.getLocale().equals("km")}">
                    ${messages.get("common.khmer")}
                </option>
            </select>
        </label>

        <label>
            ${messages.get("profile.phoneNumber")}
            <input name="phoneNumber" value="${form.getPhoneNumber()}">
        </label>

        <label>
            ${messages.get("profile.zoneinfo")}
            <input name="zoneinfo" value="${form.getZoneinfo()}">
        </label>

        <label>
            ${messages.get("profile.picture")}
            <input name="pictureUrl" value="${form.getPictureUrl()}">
        </label>

        <label>
            ${messages.get("profile.website")}
            <input name="websiteUrl" value="${form.getWebsiteUrl()}">
        </label>

        <fieldset>
            <legend>${messages.get("profile.address")}</legend>

            <label>
                ${messages.get("profile.streetAddress")}
                <input name="addressStreetAddress" value="${form.getAddressStreetAddress()}">
            </label>

            <label>
                ${messages.get("profile.locality")}
                <input name="addressLocality" value="${form.getAddressLocality()}">
            </label>

            <label>
                ${messages.get("profile.region")}
                <input name="addressRegion" value="${form.getAddressRegion()}">
            </label>

            <label>
                ${messages.get("profile.postalCode")}
                <input name="addressPostalCode" value="${form.getAddressPostalCode()}">
            </label>

            <label>
                ${messages.get("profile.country")}
                <input name="addressCountry" value="${form.getAddressCountry()}">
            </label>
        </fieldset>

        <button type="submit">${messages.get("common.save")}</button>
        <a href="/">${messages.get("common.cancel")}</a>
    </form>
</main>
</body>
</html>
