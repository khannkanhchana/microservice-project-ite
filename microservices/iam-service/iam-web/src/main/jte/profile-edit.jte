


@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.web.WebForms.Profile form
@param java.util.List<String> errors
@param String pageError
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#090e1b">

    <title>${messages.get("profile.editTitle")} | IAM Platform</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700;800&family=Noto+Sans+Khmer:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --bg: #090e1b;
            --surface: #111a2b;
            --surface-light: #172238;
            --primary: #818cf8;
            --text: #f8fafc;
            --muted: #94a3b8;
            --border: rgba(148, 163, 184, .16);
            --danger: #f87171;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            min-height: 100vh;
            padding: 30px 20px;
            background: var(--bg);
            color: var(--text);
            font-family: "DM Sans", "Noto Sans Khmer", sans-serif;
            position: relative;
            overflow-x: hidden;
        }

        body::before,
        body::after {
            content: "";
            position: fixed;
            width: 400px;
            height: 400px;
            border-radius: 50%;
            filter: blur(130px);
            opacity: .13;
            pointer-events: none;
        }

        body::before {
            background: #6366f1;
            top: -180px;
            left: -150px;
        }

        body::after {
            background: #2563eb;
            right: -150px;
            bottom: -180px;
        }

        a {
            color: inherit;
            text-decoration: none;
        }

        .page {
            position: relative;
            z-index: 1;
            width: 100%;
            max-width: 850px;
            margin: 0 auto;
        }

        .navbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 15px;
            padding: 17px 22px;
            background: rgba(17, 26, 43, .9);
            border: 1px solid var(--border);
            border-radius: 15px;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 11px;
            font-size: 18px;
            font-weight: 800;
        }

        .brand-icon {
            width: 38px;
            height: 38px;
            display: grid;
            place-items: center;
            border-radius: 11px;
            background: linear-gradient(135deg, #818cf8, #4f46e5);
            font-size: 20px;
        }

        .nav-actions {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .nav-link {
            padding: 8px 12px;
            color: var(--muted);
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            transition: .2s;
        }

        .nav-link:hover {
            color: white;
            background: var(--surface-light);
        }

        .language-switch {
            display: flex;
            gap: 4px;
            padding: 4px;
            background: #0c1423;
            border: 1px solid var(--border);
            border-radius: 9px;
        }

        .language-switch a {
            padding: 6px 8px;
            border-radius: 6px;
            color: var(--muted);
            font-size: 11px;
        }

        .language-switch a.active,
        .language-switch a:hover {
            color: white;
            background: #334155;
        }

        .page-heading {
            margin: 35px 0 25px;
        }

        .eyebrow {
            color: #a5b4fc;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 1.5px;
        }

        .page-heading h1 {
            margin-top: 10px;
            font-size: 30px;
            letter-spacing: -1px;
        }

        .page-heading p {
            margin-top: 8px;
            color: var(--muted);
            font-size: 13px;
            line-height: 1.7;
        }

        .form-card {
            padding: 30px;
            background: rgba(17, 26, 43, .92);
            border: 1px solid var(--border);
            border-radius: 16px;
            box-shadow: 0 15px 40px rgba(0, 0, 0, .12);
        }

        .alert-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-bottom: 22px;
        }

        .alert {
            padding: 13px 15px;
            border: 1px solid rgba(248, 113, 113, .25);
            border-radius: 9px;
            color: #fca5a5;
            background: rgba(239, 68, 68, .1);
            font-size: 12px;
            line-height: 1.6;
        }

        .form-section + .form-section {
            margin-top: 30px;
            padding-top: 26px;
            border-top: 1px solid var(--border);
        }

        .section-heading {
            margin-bottom: 20px;
        }

        .section-heading h2 {
            font-size: 15px;
            font-weight: 700;
        }

        .section-heading p {
            margin-top: 6px;
            color: var(--muted);
            font-size: 11px;
            line-height: 1.6;
        }

        .form-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 19px 16px;
        }

        .form-group {
            min-width: 0;
        }

        .form-group.full {
            grid-column: 1 / -1;
        }

        .form-label {
            display: block;
            margin-bottom: 8px;
            color: #e2e8f0;
            font-size: 12px;
            font-weight: 600;
        }

        .form-input,
        .form-select {
            width: 100%;
            height: 45px;
            padding: 0 13px;
            border: 1px solid var(--border);
            border-radius: 9px;
            outline: none;
            background: #0c1423;
            color: white;
            font-family: inherit;
            font-size: 12px;
            transition: .2s;
        }

        .form-input::placeholder {
            color: #64748b;
        }

        .form-input:focus,
        .form-select:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(129, 140, 248, .12);
        }

        .form-select {
            cursor: pointer;
        }

        .form-select option {
            background: #111a2b;
        }

        .form-hint {
            display: block;
            margin-top: 7px;
            color: #64748b;
            font-size: 10px;
        }

        .form-actions {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            gap: 12px;
            margin-top: 30px;
            padding-top: 22px;
            border-top: 1px solid var(--border);
        }

        .btn {
            min-height: 43px;
            padding: 0 20px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 9px;
            font-family: inherit;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            transition: .2s;
        }

        .btn-cancel {
            border: 1px solid var(--border);
            color: #cbd5e1;
            background: #172238;
        }

        .btn-cancel:hover {
            background: #26354e;
        }

        .btn-save {
            border: none;
            color: white;
            background: linear-gradient(135deg, #818cf8, #6366f1);
            box-shadow: 0 8px 20px rgba(99, 102, 241, .2);
        }

        .btn-save:hover {
            transform: translateY(-2px);
            background: linear-gradient(135deg, #6366f1, #4f46e5);
        }

        .footer {
            margin-top: 30px;
            padding: 20px 0;
            color: #64748b;
            text-align: center;
            font-size: 11px;
        }

        @media (max-width: 600px) {
            body {
                padding: 12px;
            }

            .navbar {
                align-items: flex-start;
                flex-direction: column;
            }

            .page-heading h1 {
                font-size: 25px;
            }

            .form-card {
                padding: 20px;
            }

            .form-grid {
                grid-template-columns: 1fr;
                gap: 17px;
            }

            .form-group.full {
                grid-column: auto;
            }

            .form-actions {
                flex-direction: column-reverse;
                align-items: stretch;
            }

            .btn {
                width: 100%;
            }
        }
    </style>
</head>

<body>

<div class="page">

    <header class="navbar">

        <a href="/" class="brand">
            <span class="brand-icon">✦</span>
            <span>IAM Platform</span>
        </a>

        <nav class="nav-actions">

            <a href="/" class="nav-link">
                ${messages.get("common.home")}
            </a>

            <div class="language-switch">
                @if ("km".equals(lang))
                    <a href="?lang=en">EN</a>
                    <a href="?lang=km" class="active">ខ្មែរ</a>
                @else
                    <a href="?lang=en" class="active">EN</a>
                    <a href="?lang=km">ខ្មែរ</a>
                @endif
            </div>

        </nav>

    </header>

    <section class="page-heading">
        <span class="eyebrow">ACCOUNT SETTINGS</span>

        <h1>${messages.get("profile.editTitle")}</h1>

        <p>
            Update your personal information and profile preferences.
        </p>
    </section>

    <main class="form-card">

        @if (pageError != null || !errors.isEmpty())
            <div class="alert-list" role="alert">

                @if (pageError != null)
                    <div class="alert">
                        ⚠ &nbsp; ${pageError}
                    </div>
                @endif

                @for (String error : errors)
                    <div class="alert">
                        ⚠ &nbsp; ${error}
                    </div>
                @endfor

            </div>
        @endif

        <form method="post" action="/profile/edit">

            @if (csrf != null)
                <input
                        type="hidden"
                        name="${csrf.getParameterName()}"
                        value="${csrf.getToken()}">
            @endif

            <section class="form-section">

                <div class="section-heading">
                    <h2>Personal information</h2>
                    <p>Manage your name and personal details.</p>
                </div>

                <div class="form-grid">

                    <div class="form-group">
                        <label class="form-label" for="givenName">
                            ${messages.get("profile.givenName")}
                        </label>

                        <input
                                class="form-input"
                                id="givenName"
                                name="givenName"
                                type="text"
                                value="${form.getGivenName()}"
                                autocomplete="given-name">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="familyName">
                            ${messages.get("profile.familyName")}
                        </label>

                        <input
                                class="form-input"
                                id="familyName"
                                name="familyName"
                                type="text"
                                value="${form.getFamilyName()}"
                                autocomplete="family-name">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="middleName">
                            ${messages.get("profile.middleName")}
                        </label>

                        <input
                                class="form-input"
                                id="middleName"
                                name="middleName"
                                type="text"
                                value="${form.getMiddleName()}">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="nickname">
                            ${messages.get("profile.nickname")}
                        </label>

                        <input
                                class="form-input"
                                id="nickname"
                                name="nickname"
                                type="text"
                                value="${form.getNickname()}">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="gender">
                            ${messages.get("profile.gender")}
                        </label>

                        <input
                                class="form-input"
                                id="gender"
                                name="gender"
                                type="text"
                                value="${form.getGender()}">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="birthdate">
                            ${messages.get("profile.birthdate")}
                        </label>

                        <input
                                class="form-input"
                                id="birthdate"
                                name="birthdate"
                                type="date"
                                value="${form.getBirthdate()}">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="locale">
                            ${messages.get("profile.locale")}
                        </label>

                        <select class="form-select" id="locale" name="locale">

                            @if (form.getLocale() == null || "en".equals(form.getLocale()))
                                <option value="en" selected>
                                    ${messages.get("common.english")}
                                </option>
                            @else
                                <option value="en">
                                    ${messages.get("common.english")}
                                </option>
                            @endif

                            @if ("km".equals(form.getLocale()))
                                <option value="km" selected>
                                    ${messages.get("common.khmer")}
                                </option>
                            @else
                                <option value="km">
                                    ${messages.get("common.khmer")}
                                </option>
                            @endif

                        </select>
                    </div>

                </div>

            </section>

            <section class="form-section">

                <div class="section-heading">
                    <h2>Contact and preferences</h2>
                    <p>Manage your contact information and regional settings.</p>
                </div>

                <div class="form-grid">

                    <div class="form-group">
                        <label class="form-label" for="phoneNumber">
                            ${messages.get("profile.phoneNumber")}
                        </label>

                        <input
                                class="form-input"
                                id="phoneNumber"
                                name="phoneNumber"
                                type="tel"
                                value="${form.getPhoneNumber()}"
                                autocomplete="tel">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="zoneinfo">
                            ${messages.get("profile.zoneinfo")}
                        </label>

                        <input
                                class="form-input"
                                id="zoneinfo"
                                name="zoneinfo"
                                type="text"
                                value="${form.getZoneinfo()}"
                                placeholder="e.g. Asia/Phnom_Penh">
                    </div>

                </div>

            </section>

            <section class="form-section">

                <div class="section-heading">
                    <h2>Profile links</h2>
                    <p>Provide your profile image and personal website.</p>
                </div>

                <div class="form-grid">

                    <div class="form-group">
                        <label class="form-label" for="pictureUrl">
                            ${messages.get("profile.picture")}
                        </label>

                        <input
                                class="form-input"
                                id="pictureUrl"
                                name="pictureUrl"
                                type="url"
                                value="${form.getPictureUrl()}"
                                placeholder="https://example.com/photo.jpg">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="websiteUrl">
                            ${messages.get("profile.website")}
                        </label>

                        <input
                                class="form-input"
                                id="websiteUrl"
                                name="websiteUrl"
                                type="url"
                                value="${form.getWebsiteUrl()}"
                                placeholder="https://example.com">
                    </div>

                </div>

            </section>

            <section class="form-section">

                <div class="section-heading">
                    <h2>${messages.get("profile.address")}</h2>
                    <p>Manage your address information.</p>
                </div>

                <div class="form-grid">

                    <div class="form-group full">
                        <label class="form-label" for="addressStreetAddress">
                            ${messages.get("profile.streetAddress")}
                        </label>

                        <input
                                class="form-input"
                                id="addressStreetAddress"
                                name="addressStreetAddress"
                                type="text"
                                value="${form.getAddressStreetAddress()}"
                                autocomplete="street-address">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="addressLocality">
                            ${messages.get("profile.locality")}
                        </label>

                        <input
                                class="form-input"
                                id="addressLocality"
                                name="addressLocality"
                                type="text"
                                value="${form.getAddressLocality()}"
                                autocomplete="address-level2">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="addressRegion">
                            ${messages.get("profile.region")}
                        </label>

                        <input
                                class="form-input"
                                id="addressRegion"
                                name="addressRegion"
                                type="text"
                                value="${form.getAddressRegion()}"
                                autocomplete="address-level1">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="addressPostalCode">
                            ${messages.get("profile.postalCode")}
                        </label>

                        <input
                                class="form-input"
                                id="addressPostalCode"
                                name="addressPostalCode"
                                type="text"
                                value="${form.getAddressPostalCode()}"
                                autocomplete="postal-code">
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="addressCountry">
                            ${messages.get("profile.country")}
                        </label>

                        <input
                                class="form-input"
                                id="addressCountry"
                                name="addressCountry"
                                type="text"
                                value="${form.getAddressCountry()}"
                                autocomplete="country-name">
                    </div>

                </div>

            </section>

            <div class="form-actions">

                <a href="/" class="btn btn-cancel">
                    ${messages.get("common.cancel")}
                </a>

                <button type="submit" class="btn btn-save">
                    ${messages.get("common.save")} &nbsp; →
                </button>

            </div>

        </form>

    </main>

    <footer class="footer">
        © 2026 IAM Platform. All rights reserved.
    </footer>

</div>

</body>
</html>
