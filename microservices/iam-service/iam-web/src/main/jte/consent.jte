
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
    <meta name="theme-color" content="#090e1b">
    <title>${messages.get("consent.title")} | IAM Platform</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700;800&family=Noto+Sans+Khmer:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            color-scheme: dark;
            --bg: #090e1b;
            --surface: #111a2b;
            --surface-hover: #172238;
            --border: #26344c;
            --text: #e5eaf5;
            --muted: #94a3b8;
            --accent: #818cf8;
            --accent-hover: #6366f1;
            --danger: #f87171;
            --success: #34d399;
        }

        * {
            box-sizing: border-box;
        }

        body {
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            margin: 0;
            padding: 28px 16px;
            background:
                    radial-gradient(ellipse at top, rgba(99, 102, 241, .14), transparent 45%),
                    var(--bg);
            color: var(--text);
            font-family: "DM Sans", "Noto Sans Khmer", sans-serif;
            line-height: 1.6;
        }

        button {
            font: inherit;
        }

        .container {
            width: 100%;
            max-width: 510px;
        }

        .topbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 20px;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 10px;
            color: var(--text);
            font-size: 16px;
            font-weight: 800;
            letter-spacing: -.3px;
            text-decoration: none;
        }

        .brand-icon {
            display: grid;
            place-items: center;
            width: 36px;
            height: 36px;
            border-radius: 11px;
            background: rgba(129, 140, 248, .15);
            color: var(--accent);
            font-size: 18px;
        }

        .language {
            display: flex;
            align-items: center;
            gap: 7px;
            color: var(--muted);
            font-size: 12px;
            font-weight: 600;
        }

        .language a {
            padding: 5px 9px;
            border: 1px solid transparent;
            border-radius: 7px;
            color: inherit;
            text-decoration: none;
        }

        .language a.active {
            border-color: rgba(129, 140, 248, .35);
            background: rgba(129, 140, 248, .08);
            color: var(--accent);
        }

        .card {
            padding: 32px;
            border: 1px solid var(--border);
            border-radius: 19px;
            background: rgba(17, 26, 43, .96);
            box-shadow: 0 24px 70px rgba(0, 0, 0, .28);
        }

        .security-icon {
            display: grid;
            place-items: center;
            width: 58px;
            height: 58px;
            margin-bottom: 22px;
            border: 1px solid rgba(129, 140, 248, .25);
            border-radius: 17px;
            background: rgba(129, 140, 248, .12);
            color: var(--accent);
            font-size: 25px;
        }

        .eyebrow {
            color: var(--accent);
            font-size: 11px;
            font-weight: 800;
            letter-spacing: 1.5px;
            text-transform: uppercase;
        }

        h1 {
            margin: 8px 0 12px;
            font-size: clamp(25px, 5vw, 31px);
            line-height: 1.25;
            letter-spacing: -.8px;
        }

        .description {
            margin: 0 0 24px;
            color: var(--muted);
            font-size: 14px;
        }

        .client-box {
            display: flex;
            align-items: center;
            gap: 13px;
            margin-bottom: 25px;
            padding: 15px;
            border: 1px solid var(--border);
            border-radius: 12px;
            background: #0d1525;
        }

        .client-icon {
            display: grid;
            place-items: center;
            width: 40px;
            height: 40px;
            flex-shrink: 0;
            border-radius: 11px;
            background: rgba(129, 140, 248, .12);
            color: var(--accent);
            font-size: 17px;
        }

        .client-info {
            min-width: 0;
        }

        .client-label {
            display: block;
            margin-bottom: 3px;
            color: var(--muted);
            font-size: 11px;
            font-weight: 600;
        }

        .client-value {
            display: block;
            overflow-wrap: anywhere;
            color: var(--text);
            font-size: 13px;
            font-weight: 700;
        }

        fieldset {
            min-width: 0;
            margin: 0;
            padding: 0;
            border: 0;
        }

        legend {
            margin-bottom: 12px;
            color: var(--text);
            font-size: 14px;
            font-weight: 700;
        }

        .scope-list {
            display: grid;
            gap: 10px;
        }

        .scope-item {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            padding: 13px 14px;
            border: 1px solid var(--border);
            border-radius: 11px;
            background: #0d1525;
            cursor: pointer;
            transition: border-color .2s, background .2s;
        }

        .scope-item:hover {
            border-color: #465477;
            background: var(--surface-hover);
        }

        .scope-item:has(input:checked) {
            border-color: rgba(129, 140, 248, .45);
            background: rgba(129, 140, 248, .07);
        }

        .scope-item input {
            width: 17px;
            height: 17px;
            flex-shrink: 0;
            margin: 3px 0 0;
            accent-color: var(--accent);
            cursor: pointer;
        }

        .scope-name {
            display: block;
            overflow-wrap: anywhere;
            color: var(--text);
            font-family: monospace;
            font-size: 13px;
            font-weight: 600;
        }

        .scope-description {
            display: block;
            margin-top: 3px;
            color: var(--muted);
            font-size: 11px;
        }

        .scope-count {
            margin-top: 12px;
            color: var(--muted);
            font-size: 11px;
        }

        .divider {
            height: 1px;
            margin: 24px 0;
            background: var(--border);
        }

        .actions {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 11px;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-height: 45px;
            padding: 11px 15px;
            border: 1px solid transparent;
            border-radius: 10px;
            cursor: pointer;
            font-size: 13px;
            font-weight: 700;
            transition: .2s;
        }

        .btn-allow {
            background: var(--accent);
            color: #0b1020;
        }

        .btn-allow:hover {
            background: var(--accent-hover);
            color: white;
        }

        .btn-cancel {
            border-color: var(--border);
            background: transparent;
            color: var(--text);
        }

        .btn-cancel:hover {
            border-color: var(--danger);
            background: rgba(248, 113, 113, .08);
            color: #fca5a5;
        }

        .security-note {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            margin-top: 20px;
            color: var(--muted);
            font-size: 11px;
        }

        .security-note-icon {
            color: var(--success);
            font-size: 15px;
        }

        .footer {
            margin-top: 22px;
            color: #64748b;
            font-size: 11px;
            text-align: center;
        }

        @media (max-width: 480px) {
            .card {
                padding: 23px 19px;
            }

            .actions {
                grid-template-columns: 1fr;
            }

            .topbar {
                align-items: flex-start;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <header class="topbar">
        <a href="/" class="brand">
            <span class="brand-icon">◆</span>
            <span>IAM Platform</span>
        </a>

        <nav class="language">
            <a href="?lang=en" class="${"en".equals(lang) ? "active" : ""}">
                ${messages.get("common.english")}
            </a>
            <a href="?lang=km" class="${"km".equals(lang) ? "active" : ""}">
                ${messages.get("common.khmer")}
            </a>
        </nav>
    </header>

    <main class="card">
        <div class="security-icon">♧</div>

        <div class="eyebrow">Authorization Request</div>

        <h1>${messages.get("consent.title")}</h1>

        <p class="description">
            ${messages.get("consent.description")}
        </p>

        <div class="client-box">
            <div class="client-icon">A</div>

            <div class="client-info">
                <span class="client-label">
                    ${messages.get("consent.client")}
                </span>
                <span class="client-value">${clientId}</span>
            </div>
        </div>

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

                <div class="scope-list">
                    @for (String scope : scopes)
                        <label class="scope-item">
                            <input
                                    type="checkbox"
                                    name="scope"
                                    value="${scope}"
                                    checked>

                            <span>
                                <span class="scope-name">${scope}</span>
                                <span class="scope-description">
                                    Permission requested by this application
                                </span>
                            </span>
                        </label>
                    @endfor
                </div>

                <div class="scope-count">
                    ${scopes.size()} permissions requested
                </div>
            </fieldset>

            <div class="divider"></div>

            <div class="actions">
                <button
                        class="btn btn-allow"
                        type="submit"
                        name="consent_action"
                        value="authorize">
                    ${messages.get("consent.allow")}
                </button>

                <button
                        class="btn btn-cancel"
                        type="submit"
                        name="consent_action"
                        value="cancel">
                    ${messages.get("consent.cancel")}
                </button>
            </div>
        </form>

        <div class="security-note">
            <span class="security-note-icon">✓</span>
            <span>
                Only approve permissions you understand and trust. You can manage connected applications from your account profile.
            </span>
        </div>
    </main>

    <footer class="footer">
        IAM Platform · Secure Identity and Access Management
    </footer>
</div>
</body>
</html>
