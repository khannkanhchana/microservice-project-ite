
@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.web.WebForms.Password form
@param java.util.List<String> errors
@param String success
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#090e1b">

    <title>${messages.get("password.title")} | IAM Platform</title>

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

        .content-grid {
            display: grid;
            grid-template-columns: .8fr 1.2fr;
            gap: 20px;
            align-items: start;
        }

        .card {
            padding: 26px;
            background: rgba(17, 26, 43, .92);
            border: 1px solid var(--border);
            border-radius: 16px;
            box-shadow: 0 15px 40px rgba(0, 0, 0, .12);
        }

        .security-card {
            background:
                    radial-gradient(circle at 20% 10%, rgba(99, 102, 241, .17), transparent 55%),
                    rgba(17, 26, 43, .92);
        }

        .security-icon {
            width: 54px;
            height: 54px;
            display: grid;
            place-items: center;
            border-radius: 16px;
            color: #c7d2fe;
            background: rgba(99, 102, 241, .15);
            font-size: 25px;
        }

        .security-card h2 {
            margin-top: 20px;
            font-size: 17px;
        }

        .security-card p {
            margin-top: 10px;
            color: var(--muted);
            font-size: 12px;
            line-height: 1.8;
        }

        .security-tips {
            display: flex;
            flex-direction: column;
            gap: 15px;
            margin-top: 25px;
            padding-top: 20px;
            border-top: 1px solid var(--border);
        }

        .tip {
            display: flex;
            align-items: flex-start;
            gap: 10px;
            color: #cbd5e1;
            font-size: 11px;
            line-height: 1.6;
        }

        .tip-icon {
            color: #818cf8;
            font-size: 14px;
        }

        .form-card h2 {
            margin-bottom: 7px;
            font-size: 17px;
        }

        .form-description {
            margin-bottom: 24px;
            color: var(--muted);
            font-size: 11px;
            line-height: 1.7;
        }

        .alert-list {
            display: flex;
            flex-direction: column;
            gap: 10px;
            margin-bottom: 20px;
        }

        .alert {
            padding: 12px 14px;
            border: 1px solid rgba(248, 113, 113, .25);
            border-radius: 9px;
            color: #fca5a5;
            background: rgba(239, 68, 68, .1);
            font-size: 12px;
            line-height: 1.6;
        }

        .success-alert {
            padding: 13px 15px;
            margin-bottom: 20px;
            border: 1px solid rgba(74, 222, 128, .25);
            border-radius: 9px;
            color: #86efac;
            background: rgba(34, 197, 94, .1);
            font-size: 12px;
            line-height: 1.6;
        }

        .form-group {
            margin-bottom: 19px;
        }

        .form-label {
            display: block;
            margin-bottom: 8px;
            color: #e2e8f0;
            font-size: 12px;
            font-weight: 600;
        }

        .password-wrapper {
            position: relative;
        }

        .form-input {
            width: 100%;
            height: 46px;
            padding: 0 65px 0 13px;
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

        .form-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(129, 140, 248, .12);
        }

        .toggle-password {
            position: absolute;
            top: 50%;
            right: 12px;
            transform: translateY(-50%);
            border: none;
            background: none;
            color: #94a3b8;
            font-family: inherit;
            font-size: 11px;
            cursor: pointer;
        }

        .toggle-password:hover {
            color: white;
        }

        .strength-container {
            margin-top: 9px;
        }

        .strength-bars {
            display: flex;
            gap: 5px;
        }

        .strength-bars span {
            flex: 1;
            height: 4px;
            background: #334155;
            border-radius: 10px;
            transition: .2s;
        }

        .strength-label {
            display: block;
            margin-top: 7px;
            color: var(--muted);
            font-size: 10px;
        }

        .match-message {
            display: block;
            margin-top: 7px;
            font-size: 10px;
        }

        .match-success {
            color: var(--success);
        }

        .match-error {
            color: var(--danger);
        }

        .submit-btn {
            width: 100%;
            height: 47px;
            margin-top: 5px;
            border: none;
            border-radius: 9px;
            color: white;
            background: linear-gradient(135deg, #818cf8, #6366f1);
            font-family: inherit;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            box-shadow: 0 8px 20px rgba(99, 102, 241, .2);
            transition: .2s;
        }

        .submit-btn:hover {
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

        @media (max-width: 750px) {
            .content-grid {
                grid-template-columns: 1fr;
            }

            .security-card {
                order: 2;
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

            .page-heading h1 {
                font-size: 25px;
            }

            .card {
                padding: 20px;
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
        <span class="eyebrow">ACCOUNT SECURITY</span>

        <h1>${messages.get("password.title")}</h1>

        <p>
            Keep your account secure by using a strong and unique password.
        </p>
    </section>

    <main class="content-grid">

        <section class="card form-card">

            <h2>Change your password</h2>

            <p class="form-description">
                Enter your current password and choose a new password.
            </p>

            @if (success != null)
                <div class="success-alert" role="status">
                    ✓ &nbsp; ${success}
                </div>
            @endif

            @if (!errors.isEmpty())
                <div class="alert-list" role="alert">
                    @for (String error : errors)
                        <div class="alert">
                            ⚠ &nbsp; ${error}
                        </div>
                    @endfor
                </div>
            @endif

            <form method="post" action="/profile/password">

                @if (csrf != null)
                    <input
                            type="hidden"
                            name="${csrf.getParameterName()}"
                            value="${csrf.getToken()}">
                @endif

                <div class="form-group">

                    <label class="form-label" for="currentPassword">
                        ${messages.get("password.current")}
                    </label>

                    <div class="password-wrapper">
                        <input
                                class="form-input"
                                id="currentPassword"
                                type="password"
                                name="currentPassword"
                                placeholder="Enter current password"
                                autocomplete="current-password"
                                required>

                        <button
                                class="toggle-password"
                                type="button"
                                data-target="currentPassword">
                            Show
                        </button>
                    </div>

                </div>

                <div class="form-group">

                    <label class="form-label" for="newPassword">
                        ${messages.get("password.new")}
                    </label>

                    <div class="password-wrapper">
                        <input
                                class="form-input"
                                id="newPassword"
                                type="password"
                                name="newPassword"
                                placeholder="Enter new password"
                                autocomplete="new-password"
                                minlength="8"
                                required>

                        <button
                                class="toggle-password"
                                type="button"
                                data-target="newPassword">
                            Show
                        </button>
                    </div>

                    <div class="strength-container">

                        <div class="strength-bars">
                            <span></span>
                            <span></span>
                            <span></span>
                            <span></span>
                        </div>

                        <span class="strength-label" id="strengthLabel">
                            Password strength
                        </span>

                    </div>

                </div>

                <div class="form-group">

                    <label class="form-label" for="confirmPassword">
                        ${messages.get("password.confirm")}
                    </label>

                    <div class="password-wrapper">
                        <input
                                class="form-input"
                                id="confirmPassword"
                                type="password"
                                name="confirmPassword"
                                placeholder="Confirm new password"
                                autocomplete="new-password"
                                minlength="8"
                                required>

                        <button
                                class="toggle-password"
                                type="button"
                                data-target="confirmPassword">
                            Show
                        </button>
                    </div>

                    <span
                            class="match-message"
                            id="matchMessage"
                            aria-live="polite">
                    </span>

                </div>

                <button type="submit" class="submit-btn">
                    ${messages.get("password.submit")} →
                </button>

            </form>

        </section>

        <aside class="card security-card">

            <div class="security-icon">♧</div>

            <h2>Protect your account</h2>

            <p>
                A strong password helps protect your personal information
                and prevents unauthorized access to your IAM account.
            </p>

            <div class="security-tips">

                <div class="tip">
                    <span class="tip-icon">✓</span>
                    Use at least 8 characters.
                </div>

                <div class="tip">
                    <span class="tip-icon">✓</span>
                    Combine uppercase and lowercase letters.
                </div>

                <div class="tip">
                    <span class="tip-icon">✓</span>
                    Include numbers and special characters.
                </div>

                <div class="tip">
                    <span class="tip-icon">✓</span>
                    Avoid reusing passwords from other websites.
                </div>

                <div class="tip">
                    <span class="tip-icon">✓</span>
                    Never share your password with anyone.
                </div>

            </div>

        </aside>

    </main>

    <footer class="footer">
        © 2026 IAM Platform. All rights reserved.
    </footer>

</div>

<script>
    const newPassword = document.getElementById("newPassword");
    const confirmPassword = document.getElementById("confirmPassword");
    const strengthLabel = document.getElementById("strengthLabel");
    const strengthBars = document.querySelectorAll(".strength-bars span");
    const matchMessage = document.getElementById("matchMessage");

    document.querySelectorAll(".toggle-password").forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.target);
            const visible = input.type === "password";

            input.type = visible ? "text" : "password";
            button.textContent = visible ? "Hide" : "Show";
            button.setAttribute("aria-pressed", String(visible));
        });
    });

    function checkStrength(value) {
        let score = 0;

        if (value.length >= 8) score++;
        if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
        if (/\d/.test(value)) score++;
        if (/[^a-zA-Z0-9]/.test(value)) score++;

        const labels = [
            "Password strength",
            "Weak password",
            "Fair password",
            "Good password",
            "Strong password"
        ];

        const colors = [
            "",
            "#ef4444",
            "#f59e0b",
            "#3b82f6",
            "#22c55e"
        ];

        strengthBars.forEach((bar, index) => {
            bar.style.background = index < score
                ? colors[score]
                : "#334155";
        });

        strengthLabel.textContent = value.length === 0
            ? labels[0]
            : labels[score];
    }

    function checkPasswordMatch() {
        const first = newPassword.value;
        const second = confirmPassword.value;

        if (!second) {
            matchMessage.textContent = "";
            matchMessage.className = "match-message";
            confirmPassword.setCustomValidity("");
            return;
        }

        const matched = first === second;

        matchMessage.textContent = matched
            ? "✓ Passwords match"
            : "✕ Passwords do not match";

        matchMessage.className = matched
            ? "match-message match-success"
            : "match-message match-error";

        confirmPassword.setCustomValidity(
            matched ? "" : "Passwords do not match"
        );
    }

    newPassword.addEventListener("input", () => {
        checkStrength(newPassword.value);
        checkPasswordMatch();
    });

    confirmPassword.addEventListener("input", checkPasswordMatch);
</script>

</body>
</html>
