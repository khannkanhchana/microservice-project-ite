
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
    <meta name="theme-color" content="#090e1b">
    <title>${messages.get("apps.title")} | IAM Platform</title>

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

        .app-list {
            display: grid;
            gap: 15px;
        }

        .app-card {
            padding: 21px;
            border: 1px solid var(--border);
            border-radius: 15px;
            background: var(--surface);
            transition: border-color .2s, background .2s;
        }

        .app-card:hover {
            border-color: #465477;
            background: var(--surface-hover);
        }

        .app-header {
            display: flex;
            align-items: center;
            gap: 13px;
            margin-bottom: 19px;
        }

        .app-icon {
            display: grid;
            place-items: center;
            width: 45px;
            height: 45px;
            flex-shrink: 0;
            border: 1px solid rgba(129, 140, 248, .2);
            border-radius: 13px;
            background: rgba(129, 140, 248, .12);
            color: var(--accent);
            font-size: 19px;
            font-weight: 800;
        }

        .app-heading {
            min-width: 0;
            flex: 1;
        }

        .app-name {
            overflow-wrap: anywhere;
            font-size: 16px;
            font-weight: 800;
        }

        .app-status {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            margin-top: 4px;
            color: var(--success);
            font-size: 11px;
            font-weight: 700;
        }

        .status-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: var(--success);
        }

        .scope-section {
            padding: 15px;
            border: 1px solid var(--border);
            border-radius: 11px;
            background: rgba(9, 14, 27, .45);
        }

        .scope-title {
            margin: 0 0 12px;
            color: var(--muted);
            font-size: 12px;
            font-weight: 700;
        }

        .scope-list {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin: 0;
            padding: 0;
            list-style: none;
        }

        .scope-item {
            padding: 5px 10px;
            border: 1px solid rgba(129, 140, 248, .25);
            border-radius: 7px;
            background: rgba(129, 140, 248, .08);
            color: #c7d2fe;
            font-family: monospace;
            font-size: 12px;
        }

        .no-scopes {
            color: var(--muted);
            font-size: 12px;
        }

        .app-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 15px;
            flex-wrap: wrap;
            margin-top: 18px;
            padding-top: 15px;
            border-top: 1px solid var(--border);
        }

        .client-id {
            min-width: 0;
            color: var(--muted);
            font-size: 11px;
            overflow-wrap: anywhere;
        }

        .client-id strong {
            color: var(--text);
            font-weight: 600;
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
            padding: 40px 20px;
            border: 1px dashed #35435d;
            border-radius: 15px;
            background: rgba(17, 26, 43, .55);
            text-align: center;
        }

        .empty-icon {
            display: grid;
            place-items: center;
            width: 54px;
            height: 54px;
            margin: 0 auto 14px;
            border-radius: 15px;
            background: rgba(129, 140, 248, .1);
            color: var(--accent);
            font-size: 24px;
        }

        .empty-state p {
            margin: 0;
            color: var(--muted);
            font-size: 14px;
        }

        .security-note {
            margin-top: 26px;
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

            .app-footer {
                align-items: stretch;
                flex-direction: column;
            }

            .app-footer .btn {
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
            <div class="eyebrow">Application Security</div>
            <h1>${messages.get("apps.title")}</h1>
            <p class="subtitle">
                Review applications that have access to your account and manage their permissions.
            </p>
        </div>

        @if (pageError != null)
            <div class="alert" role="alert">
                ${pageError}
            </div>
        @endif

        <section>
            <div class="section-header">
                <h2>${messages.get("apps.title")}</h2>
                <span class="count">${apps.size()} connected</span>
            </div>

            @if (apps.isEmpty())
                <div class="empty-state">
                    <div class="empty-icon">▣</div>
                    <p>${messages.get("apps.empty")}</p>
                </div>
            @else
                <div class="app-list">
                    @for (var app : apps)
                        <article class="app-card">
                            <div class="app-header">
                                <div class="app-icon">A</div>

                                <div class="app-heading">
                                    <div class="app-name">
                                        ${app.clientName()}
                                    </div>
                                    <div class="app-status">
                                        <span class="status-dot"></span>
                                        Connected application
                                    </div>
                                </div>
                            </div>

                            <div class="scope-section">
                                <p class="scope-title">
                                    ${messages.get("apps.scopes")}
                                </p>

                                @if (app.scopes().isEmpty())
                                    <span class="no-scopes">No scopes available</span>
                                @else
                                    <ul class="scope-list">
                                        @for (String scope : app.scopes())
                                            <li class="scope-item">${scope}</li>
                                        @endfor
                                    </ul>
                                @endif
                            </div>

                            <div class="app-footer">
                                <div class="client-id">
                                    <strong>Client ID:</strong>
                                    ${app.clientId()}
                                </div>

                                <form method="post"
                                      action="/profile/apps/${app.clientId()}/revoke"
                                      onsubmit="return confirm('Are you sure you want to revoke access for this application?');">
                                    @if (csrf != null)
                                        <input
                                                type="hidden"
                                                name="${csrf.getParameterName()}"
                                                value="${csrf.getToken()}">
                                    @endif

                                    <button class="btn btn-danger" type="submit">
                                        <span>×</span>
                                        ${messages.get("apps.revoke")}
                                    </button>
                                </form>
                            </div>
                        </article>
                    @endfor
                </div>
            @endif
        </section>

        <div class="security-note">
            Review application permissions regularly. Revoking an application should prevent it from continuing to access resources using the authorization it previously received.
        </div>
    </main>
</div>
</body>
</html>
