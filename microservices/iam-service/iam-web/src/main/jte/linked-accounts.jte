
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
    <meta name="theme-color" content="#090e1b">
    <title>${messages.get("accounts.title")} | IAM Platform</title>

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
            margin: 0;
            min-height: 100vh;
            background:
                    radial-gradient(ellipse at top left, rgba(99, 102, 241, .12), transparent 38%),
                    var(--bg);
            color: var(--text);
            font-family: "DM Sans", "Noto Sans Khmer", sans-serif;
            line-height: 1.6;
        }

        a {
            color: inherit;
            text-decoration: none;
        }

        button {
            font: inherit;
        }

        .container {
            width: min(100% - 40px, 960px);
            margin: 0 auto;
            padding: 32px 0 64px;
        }

        .topbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 16px;
            flex-wrap: wrap;
            padding-bottom: 24px;
            border-bottom: 1px solid var(--border);
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 11px;
            font-size: 17px;
            font-weight: 800;
            letter-spacing: -.4px;
        }

        .brand-icon {
            display: grid;
            place-items: center;
            width: 38px;
            height: 38px;
            border-radius: 12px;
            background: rgba(129, 140, 248, .15);
            color: var(--accent);
            font-size: 19px;
        }

        .nav-links {
            display: flex;
            align-items: center;
            gap: 18px;
            flex-wrap: wrap;
            color: var(--muted);
            font-size: 13px;
            font-weight: 600;
        }

        .nav-links a:hover {
            color: var(--text);
        }

        .language {
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .language a {
            padding: 5px 9px;
            border: 1px solid transparent;
            border-radius: 7px;
        }

        .language a.active {
            color: var(--accent);
            border-color: rgba(129, 140, 248, .35);
            background: rgba(129, 140, 248, .08);
        }

        .page-heading {
            margin: 42px 0 28px;
        }

        .eyebrow {
            color: var(--accent);
            font-size: 11px;
            font-weight: 800;
            letter-spacing: 1.5px;
            text-transform: uppercase;
        }

        h1 {
            margin: 8px 0;
            font-size: clamp(27px, 5vw, 36px);
            line-height: 1.2;
            letter-spacing: -1px;
        }

        .subtitle {
            margin: 10px 0 0;
            color: var(--muted);
            font-size: 14px;
        }

        .alert {
            margin: 20px 0;
            padding: 13px 16px;
            border: 1px solid rgba(248, 113, 113, .3);
            border-radius: 11px;
            background: rgba(248, 113, 113, .08);
            color: #fca5a5;
            font-size: 14px;
        }

        .section {
            margin-top: 28px;
        }

        .section-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            margin-bottom: 15px;
        }

        h2 {
            margin: 0;
            font-size: 19px;
            letter-spacing: -.4px;
        }

        .count {
            padding: 4px 10px;
            border: 1px solid var(--border);
            border-radius: 20px;
            background: var(--surface);
            color: var(--muted);
            font-size: 12px;
            font-weight: 700;
        }

        .identity-list {
            display: grid;
            gap: 14px;
        }

        .identity-card {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 20px;
            padding: 20px;
            border: 1px solid var(--border);
            border-radius: 15px;
            background: var(--surface);
            transition: border-color .2s, background .2s;
        }

        .identity-card:hover {
            border-color: #465477;
            background: var(--surface-hover);
        }

        .identity-info {
            min-width: 0;
            flex: 1;
        }

        .identity-top {
            display: flex;
            align-items: center;
            gap: 11px;
            flex-wrap: wrap;
            margin-bottom: 12px;
        }

        .provider-icon {
            display: grid;
            place-items: center;
            width: 38px;
            height: 38px;
            border-radius: 11px;
            background: rgba(129, 140, 248, .12);
            color: var(--accent);
            font-size: 16px;
            font-weight: 800;
            text-transform: uppercase;
        }

        .provider-name {
            font-size: 15px;
            font-weight: 800;
            text-transform: capitalize;
        }

        .linked-badge {
            padding: 3px 8px;
            border: 1px solid rgba(52, 211, 153, .25);
            border-radius: 20px;
            background: rgba(52, 211, 153, .08);
            color: var(--success);
            font-size: 11px;
            font-weight: 700;
        }

        .identity-details {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 12px 24px;
        }

        .detail-label {
            display: block;
            margin-bottom: 3px;
            color: var(--muted);
            font-size: 11px;
            font-weight: 600;
        }

        .detail-value {
            overflow-wrap: anywhere;
            color: var(--text);
            font-size: 13px;
            font-weight: 600;
        }

        .unlink-form {
            flex-shrink: 0;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            min-height: 39px;
            padding: 9px 15px;
            border: 1px solid transparent;
            border-radius: 9px;
            cursor: pointer;
            font-size: 12px;
            font-weight: 700;
            transition: .2s;
        }

        .btn-danger {
            border-color: rgba(248, 113, 113, .3);
            background: rgba(248, 113, 113, .08);
            color: #fca5a5;
        }

        .btn-danger:hover {
            border-color: var(--danger);
            background: rgba(248, 113, 113, .15);
        }

        .empty-state {
            padding: 38px 20px;
            border: 1px dashed #35435d;
            border-radius: 15px;
            background: rgba(17, 26, 43, .55);
            text-align: center;
        }

        .empty-icon {
            display: grid;
            place-items: center;
            width: 52px;
            height: 52px;
            margin: 0 auto 13px;
            border-radius: 15px;
            background: rgba(129, 140, 248, .1);
            color: var(--accent);
            font-size: 23px;
        }

        .empty-state p {
            margin: 0;
            color: var(--muted);
            font-size: 14px;
        }

        .provider-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
            gap: 13px;
        }

        .provider-card {
            display: flex;
            align-items: center;
            gap: 12px;
            min-height: 76px;
            padding: 15px;
            border: 1px solid var(--border);
            border-radius: 13px;
            background: var(--surface);
            transition: .2s;
        }

        .provider-card:hover {
            transform: translateY(-2px);
            border-color: var(--accent);
            background: var(--surface-hover);
        }

        .provider-card-icon {
            display: grid;
            place-items: center;
            width: 39px;
            height: 39px;
            flex-shrink: 0;
            border-radius: 11px;
            background: rgba(129, 140, 248, .12);
            color: var(--accent);
            font-size: 15px;
            font-weight: 800;
            text-transform: uppercase;
        }

        .provider-card-content {
            min-width: 0;
        }

        .provider-card-title {
            display: block;
            font-size: 13px;
            font-weight: 700;
            text-transform: capitalize;
        }

        .provider-card-action {
            display: block;
            margin-top: 2px;
            color: var(--muted);
            font-size: 11px;
        }

        .provider-arrow {
            margin-left: auto;
            color: var(--muted);
            font-size: 18px;
        }

        .footer-note {
            margin-top: 28px;
            padding: 15px 17px;
            border: 1px solid var(--border);
            border-radius: 11px;
            color: var(--muted);
            font-size: 12px;
        }

        @media (max-width: 640px) {
            .container {
                width: min(100% - 28px, 960px);
                padding-top: 20px;
            }

            .topbar {
                align-items: flex-start;
            }

            .nav-links {
                gap: 10px;
            }

            .page-heading {
                margin-top: 30px;
            }

            .identity-card {
                align-items: stretch;
                flex-direction: column;
            }

            .identity-details {
                grid-template-columns: 1fr;
                gap: 10px;
            }

            .unlink-form,
            .unlink-form .btn {
                width: 100%;
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

        <nav class="nav-links">
            <a href="/">${messages.get("common.home")}</a>
            <div class="language">
                <a href="?lang=en" class="${"en".equals(lang) ? "active" : ""}">
                    ${messages.get("common.english")}
                </a>
                <a href="?lang=km" class="${"km".equals(lang) ? "active" : ""}">
                    ${messages.get("common.khmer")}
                </a>
            </div>
        </nav>
    </header>

    <main>
        <div class="page-heading">
            <div class="eyebrow">Account Security</div>
            <h1>${messages.get("accounts.title")}</h1>
            <p class="subtitle">
                Manage the external accounts connected to your IAM profile.
            </p>
        </div>

        @if (pageError != null)
            <div class="alert" role="alert">
                ${pageError}
            </div>
        @endif

        <section class="section">
            <div class="section-header">
                <h2>${messages.get("accounts.title")}</h2>
                <span class="count">${identities.size()} linked</span>
            </div>

            @if (identities.isEmpty())
                <div class="empty-state">
                    <div class="empty-icon">◇</div>
                    <p>${messages.get("accounts.empty")}</p>
                </div>
            @else
                <div class="identity-list">
                    @for (var identity : identities)
                        <article class="identity-card">
                            <div class="identity-info">
                                <div class="identity-top">
                                    <div class="provider-icon">
                                        ${identity.provider().substring(0, 1)}
                                    </div>
                                    <span class="provider-name">
                                        ${identity.provider()}
                                    </span>
                                    <span class="linked-badge">Linked</span>
                                </div>

                                <div class="identity-details">
                                    <div>
                                        <span class="detail-label">
                                            ${messages.get("accounts.email")}
                                        </span>
                                        <span class="detail-value">
                                            ${identity.email()}
                                        </span>
                                    </div>

                                    <div>
                                        <span class="detail-label">
                                            ${messages.get("accounts.displayName")}
                                        </span>
                                        <span class="detail-value">
                                            ${identity.displayName()}
                                        </span>
                                    </div>

                                    <div>
                                        <span class="detail-label">
                                            ${messages.get("accounts.linkedAt")}
                                        </span>
                                        <span class="detail-value">
                                            ${identity.linkedAt()}
                                        </span>
                                    </div>
                                </div>
                            </div>

                            <form class="unlink-form"
                                  method="post"
                                  action="/profile/accounts/${identity.uuid()}/unlink"
                                  onsubmit="return confirm('Are you sure you want to unlink this account?');">
                                @if (csrf != null)
                                    <input
                                            type="hidden"
                                            name="${csrf.getParameterName()}"
                                            value="${csrf.getToken()}">
                                @endif

                                <button class="btn btn-danger" type="submit">
                                    <span>×</span>
                                    ${messages.get("accounts.unlink")}
                                </button>
                            </form>
                        </article>
                    @endfor
                </div>
            @endif
        </section>

        <section class="section">
            <div class="section-header">
                <h2>${messages.get("accounts.link")}</h2>
            </div>

            <div class="provider-grid">
                @for (String provider : providers)
                    <a class="provider-card"
                       href="/profile/accounts/link/${provider}">
                        <span class="provider-card-icon">
                            ${provider.substring(0, 1)}
                        </span>

                        <span class="provider-card-content">
                            <span class="provider-card-title">
                                ${messages.get("login." + provider)}
                            </span>
                            <span class="provider-card-action">
                                ${messages.get("accounts.link")}
                            </span>
                        </span>

                        <span class="provider-arrow">→</span>
                    </a>
                @endfor
            </div>
        </section>

        <div class="footer-note">
            Keep your connected accounts secure. Only unlink an identity if you are sure you no longer want to use it to access your account.
        </div>
    </main>
</div>
</body>
</html>
