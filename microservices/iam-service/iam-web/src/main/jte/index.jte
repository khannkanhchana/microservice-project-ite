
@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.application.result.UserResult user
@param kh.edu.istad.iam.application.result.UserAuthoritiesResult authorities
@param String pageError
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#090e1b">

    <title>${messages.get("profile.title")} | IAM Platform</title>

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
            --success: #4ade80;
            --danger: #f87171;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            min-height: 100vh;
            background: var(--bg);
            color: var(--text);
            font-family: "DM Sans", "Noto Sans Khmer", sans-serif;
            padding: 30px 20px;
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
            max-width: 1050px;
            margin: 0 auto;
        }

        .navbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 20px;
            padding: 17px 22px;
            background: rgba(17, 26, 43, .9);
            border: 1px solid var(--border);
            border-radius: 15px;
            backdrop-filter: blur(12px);
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
            justify-content: flex-end;
            flex-wrap: wrap;
            gap: 8px;
        }

        .nav-link {
            padding: 9px 12px;
            color: var(--muted);
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            transition: .2s;
        }

        .nav-link:hover,
        .nav-link.active {
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

        .logout-form {
            display: inline;
        }

        .logout-btn {
            padding: 9px 13px;
            border: 1px solid rgba(248, 113, 113, .25);
            border-radius: 8px;
            color: #fca5a5;
            background: rgba(239, 68, 68, .08);
            font-family: inherit;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: .2s;
        }

        .logout-btn:hover {
            background: rgba(239, 68, 68, .17);
        }

        .page-heading {
            margin: 35px 0 25px;
        }

        .page-heading .eyebrow {
            color: #a5b4fc;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 1.5px;
        }

        .page-heading h1 {
            margin-top: 10px;
            font-size: 32px;
            letter-spacing: -1px;
        }

        .page-heading p {
            margin-top: 8px;
            color: var(--muted);
            font-size: 13px;
            line-height: 1.7;
        }

        .alert {
            margin-bottom: 22px;
            padding: 14px 16px;
            border: 1px solid rgba(248, 113, 113, .25);
            border-radius: 10px;
            color: #fca5a5;
            background: rgba(239, 68, 68, .1);
            font-size: 13px;
            line-height: 1.6;
        }

        .profile-grid {
            display: grid;
            grid-template-columns: 300px minmax(0, 1fr);
            gap: 20px;
            align-items: start;
        }

        .card {
            padding: 25px;
            background: rgba(17, 26, 43, .92);
            border: 1px solid var(--border);
            border-radius: 16px;
            box-shadow: 0 15px 40px rgba(0, 0, 0, .12);
        }

        .identity-card {
            text-align: center;
        }

        .avatar {
            width: 80px;
            height: 80px;
            margin: 0 auto 18px;
            display: grid;
            place-items: center;
            border: 1px solid rgba(165, 180, 252, .3);
            border-radius: 25px;
            color: #c7d2fe;
            background: linear-gradient(135deg, #312e81, #4338ca);
            font-size: 30px;
            font-weight: 800;
        }

        .identity-card h2 {
            font-size: 18px;
            overflow-wrap: anywhere;
        }

        .identity-card .username {
            margin-top: 6px;
            color: var(--muted);
            font-size: 12px;
            overflow-wrap: anywhere;
        }

        .status-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
            margin-top: 25px;
            padding-top: 20px;
            border-top: 1px solid var(--border);
        }

        .status-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            font-size: 12px;
        }

        .status-label {
            color: var(--muted);
        }

        .status {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 5px 9px;
            border-radius: 20px;
            font-size: 10px;
            font-weight: 700;
        }

        .status.success {
            color: #86efac;
            background: rgba(34, 197, 94, .12);
        }

        .status.warning {
            color: #fcd34d;
            background: rgba(245, 158, 11, .12);
        }

        .section-title {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 10px;
            margin-bottom: 20px;
        }

        .section-title h2 {
            font-size: 16px;
        }

        .section-title p {
            margin-top: 5px;
            color: var(--muted);
            font-size: 11px;
        }

        .edit-link {
            padding: 8px 12px;
            color: #c7d2fe;
            background: rgba(99, 102, 241, .12);
            border: 1px solid rgba(129, 140, 248, .2);
            border-radius: 8px;
            font-size: 11px;
            font-weight: 600;
        }

        .edit-link:hover {
            background: rgba(99, 102, 241, .22);
        }

        .details-list {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 20px;
        }

        .detail-item {
            min-width: 0;
        }

        .detail-item dt {
            margin-bottom: 8px;
            color: var(--muted);
            font-size: 11px;
        }

        .detail-item dd {
            color: #e2e8f0;
            font-size: 13px;
            font-weight: 600;
            overflow-wrap: anywhere;
        }

        .access-card {
            grid-column: 2;
        }

        .access-section + .access-section {
            margin-top: 24px;
            padding-top: 22px;
            border-top: 1px solid var(--border);
        }

        .access-heading {
            margin-bottom: 14px;
            font-size: 13px;
            font-weight: 700;
        }

        .tag-list {
            display: flex;
            flex-wrap: wrap;
            gap: 9px;
        }

        .tag-item {
            padding: 8px 12px;
            border: 1px solid rgba(129, 140, 248, .2);
            border-radius: 8px;
            color: #c7d2fe;
            background: rgba(99, 102, 241, .1);
            font-size: 11px;
            font-weight: 600;
            overflow-wrap: anywhere;
        }

        .permission-item {
            border-color: rgba(148, 163, 184, .2);
            color: #cbd5e1;
            background: rgba(148, 163, 184, .08);
        }

        .empty-state {
            color: var(--muted);
            font-size: 12px;
        }

        .footer {
            margin-top: 30px;
            padding: 20px 0;
            color: #64748b;
            text-align: center;
            font-size: 11px;
        }

        @media (max-width: 800px) {
            .profile-grid {
                grid-template-columns: 1fr;
            }

            .access-card {
                grid-column: auto;
            }

            .identity-card {
                display: flex;
                align-items: center;
                gap: 18px;
                text-align: left;
            }

            .avatar {
                flex-shrink: 0;
                margin: 0;
            }

            .identity-info {
                min-width: 0;
            }

            .status-list {
                margin-left: auto;
                margin-top: 0;
                padding-top: 0;
                padding-left: 18px;
                border-top: none;
                border-left: 1px solid var(--border);
            }
        }

        @media (max-width: 600px) {
            body {
                padding: 12px;
            }

            .navbar {
                align-items: flex-start;
                flex-direction: column;
            }

            .nav-actions {
                justify-content: flex-start;
            }

            .page-heading h1 {
                font-size: 26px;
            }

            .card {
                padding: 20px;
            }

            .identity-card {
                flex-wrap: wrap;
            }

            .status-list {
                width: 100%;
                margin-left: 0;
                padding-top: 16px;
                padding-left: 0;
                border-top: 1px solid var(--border);
                border-left: none;
            }

            .details-list {
                grid-template-columns: 1fr;
                gap: 17px;
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

            <a href="/profile/edit" class="nav-link">
                ${messages.get("common.editProfile")}
            </a>

            <a href="/profile/password" class="nav-link">
                ${messages.get("common.password")}
            </a>

            <a href="/profile/accounts" class="nav-link">
                ${messages.get("common.linkedAccounts")}
            </a>

            <a href="/profile/apps" class="nav-link">
                ${messages.get("common.connectedApps")}
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

            <form method="post" action="/logout" class="logout-form">

                @if (csrf != null)
                    <input
                            type="hidden"
                            name="${csrf.getParameterName()}"
                            value="${csrf.getToken()}">
                @endif

                <button type="submit" class="logout-btn">
                    ${messages.get("common.logout")}
                </button>

            </form>

        </nav>

    </header>

    <section class="page-heading">
        <span class="eyebrow">ACCOUNT MANAGEMENT</span>

        <h1>${messages.get("profile.title")}</h1>

        <p>
            View your account information, security status,
            roles, and permissions.
        </p>
    </section>

    @if (pageError != null)
        <div class="alert" role="alert">
            ⚠ &nbsp; ${pageError}
        </div>
    @endif

    @if (user != null && authorities != null)

        <div class="profile-grid">

            <section class="card identity-card">

                <div class="avatar">✦</div>

                <div class="identity-info">
                    <h2>${user.username()}</h2>

                    <p class="username">
                        ${user.email()}
                    </p>
                </div>

                <div class="status-list">

                    <div class="status-row">
                        <span class="status-label">
                            ${messages.get("profile.enabled")}
                        </span>

                        @if (user.enabled())
                            <span class="status success">
                                ● Active
                            </span>
                        @else
                            <span class="status warning">
                                ● Disabled
                            </span>
                        @endif
                    </div>

                    <div class="status-row">
                        <span class="status-label">
                            ${messages.get("profile.emailVerified")}
                        </span>

                        @if (user.emailVerified())
                            <span class="status success">
                                ● Verified
                            </span>
                        @else
                            <span class="status warning">
                                ● Unverified
                            </span>
                        @endif
                    </div>

                </div>

            </section>

            <section class="card">

                <div class="section-title">
                    <div>
                        <h2>${messages.get("profile.title")}</h2>

                        <p>Personal account information</p>
                    </div>

                    <a href="/profile/edit" class="edit-link">
                        ${messages.get("common.editProfile")}
                    </a>
                </div>

                <dl class="details-list">

                    <div class="detail-item">
                        <dt>${messages.get("register.username")}</dt>
                        <dd>${user.username()}</dd>
                    </div>

                    <div class="detail-item">
                        <dt>${messages.get("profile.email")}</dt>
                        <dd>${user.email()}</dd>
                    </div>

                    <div class="detail-item">
                        <dt>${messages.get("profile.locale")}</dt>
                        <dd>${user.locale()}</dd>
                    </div>

                    <div class="detail-item">
                        <dt>${messages.get("profile.enabled")}</dt>
                        <dd>
                            @if (user.enabled())
                                Enabled
                            @else
                                Disabled
                            @endif
                        </dd>
                    </div>

                    <div class="detail-item">
                        <dt>${messages.get("profile.emailVerified")}</dt>
                        <dd>
                            @if (user.emailVerified())
                                Verified
                            @else
                                Not verified
                            @endif
                        </dd>
                    </div>

                </dl>

            </section>

            <section class="card access-card">

                <div class="section-title">
                    <div>
                        <h2>Access control</h2>
                        <p>Your assigned roles and effective permissions</p>
                    </div>
                </div>

                <div class="access-section">

                    <h3 class="access-heading">
                        ${messages.get("profile.roles")}
                    </h3>

                    <div class="tag-list">

                        @for (var role : authorities.roles())
                            <span class="tag-item">
                                ${role.name()}
                            </span>
                        @endfor

                        @if (authorities.roles().isEmpty())
                            <p class="empty-state">
                                No roles assigned.
                            </p>
                        @endif

                    </div>

                </div>

                <div class="access-section">

                    <h3 class="access-heading">
                        ${messages.get("profile.permissions")}
                    </h3>

                    <div class="tag-list">

                        @for (String permission : authorities.effectivePermissions())
                            <span class="tag-item permission-item">
                                ${permission}
                            </span>
                        @endfor

                        @if (authorities.effectivePermissions().isEmpty())
                            <p class="empty-state">
                                No permissions assigned.
                            </p>
                        @endif

                    </div>

                </div>

            </section>

        </div>

    @else

        <section class="card">
            <p class="empty-state">
                Profile information is currently unavailable.
            </p>
        </section>

    @endif

    <footer class="footer">
        © 2026 IAM Platform. All rights reserved.
    </footer>

</div>

</body>
</html>
