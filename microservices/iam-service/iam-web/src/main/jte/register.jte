
@param java.util.Map<String, String> messages
@param String lang
@param kh.edu.istad.iam.web.WebForms.Registration form
@param java.util.List<String> errors
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#090e1b">

    <title>${messages.get("register.title")} | IAM Platform</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700;800&family=Noto+Sans+Khmer:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --bg: #090e1b;
            --surface: #111a2b;
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
            padding: 35px 20px;
            display: flex;
            justify-content: center;
            align-items: center;
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
            width: 450px;
            height: 450px;
            border-radius: 50%;
            filter: blur(130px);
            opacity: .16;
            pointer-events: none;
        }

        body::before {
            background: #6366f1;
            top: -180px;
            left: -160px;
        }

        body::after {
            background: #2563eb;
            right: -160px;
            bottom: -180px;
        }

        .container {
            position: relative;
            z-index: 1;
            width: 100%;
            max-width: 1120px;
            display: grid;
            grid-template-columns: .85fr 1.15fr;
            background: rgba(17, 26, 43, .88);
            border: 1px solid var(--border);
            border-radius: 24px;
            overflow: hidden;
            box-shadow: 0 30px 100px rgba(0, 0, 0, .4);
        }

        .branding {
            padding: 48px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            background:
                    radial-gradient(circle at 20% 20%, rgba(99, 102, 241, .25), transparent 40%),
                    linear-gradient(145deg, #141e36, #101827);
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 20px;
            font-weight: 800;
        }

        .brand-icon {
            width: 42px;
            height: 42px;
            display: grid;
            place-items: center;
            border-radius: 13px;
            background: linear-gradient(135deg, #818cf8, #4f46e5);
            font-size: 22px;
            box-shadow: 0 8px 24px rgba(99, 102, 241, .3);
        }

        .brand-content {
            margin: auto 0;
            padding: 70px 0;
        }

        .tag {
            display: inline-block;
            padding: 7px 13px;
            border: 1px solid rgba(129, 140, 248, .3);
            background: rgba(99, 102, 241, .1);
            border-radius: 30px;
            color: #a5b4fc;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 1px;
        }

        .brand-content h1 {
            margin-top: 25px;
            font-size: clamp(34px, 4vw, 48px);
            line-height: 1.17;
            letter-spacing: -1.8px;
        }

        .brand-content h1 span {
            color: #818cf8;
        }

        .brand-content p {
            margin-top: 20px;
            color: #9caac0;
            font-size: 14px;
            line-height: 1.9;
        }

        .benefits {
            display: flex;
            flex-direction: column;
            gap: 18px;
            margin-top: 28px;
        }

        .benefit {
            display: flex;
            align-items: center;
            gap: 12px;
            color: #cbd5e1;
            font-size: 13px;
        }

        .benefit-icon {
            width: 32px;
            height: 32px;
            display: grid;
            place-items: center;
            border-radius: 9px;
            color: #a5b4fc;
            background: rgba(129, 140, 248, .12);
        }

        .copyright {
            color: #64748b;
            font-size: 11px;
        }

        .register-section {
            padding: 45px 55px;
            position: relative;
        }

        .top-bar {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            gap: 10px;
        }

        .language-label {
            color: var(--muted);
            font-size: 12px;
        }

        .language-switch {
            display: flex;
            gap: 5px;
            padding: 5px;
            border: 1px solid var(--border);
            background: #172238;
            border-radius: 9px;
        }

        .language-switch a {
            padding: 6px 9px;
            border-radius: 6px;
            text-decoration: none;
            color: #94a3b8;
            font-size: 12px;
        }

        .language-switch a.active,
        .language-switch a:hover {
            color: white;
            background: #334155;
        }

        .register-content {
            max-width: 490px;
            margin: 35px auto 0;
        }

        .heading h2 {
            font-size: 30px;
            letter-spacing: -1px;
            font-weight: 800;
        }

        .heading p {
            margin-top: 9px;
            color: var(--muted);
            font-size: 13px;
            line-height: 1.7;
        }

        .error-list {
            margin-top: 22px;
            display: flex;
            flex-direction: column;
            gap: 9px;
        }

        .error-message {
            padding: 12px 14px;
            color: #fca5a5;
            background: rgba(239, 68, 68, .1);
            border: 1px solid rgba(239, 68, 68, .25);
            border-radius: 9px;
            font-size: 12px;
            line-height: 1.6;
        }

        .register-form {
            margin-top: 27px;
        }

        .form-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 19px 15px;
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
            height: 46px;
            padding: 0 13px;
            color: white;
            font-family: inherit;
            font-size: 13px;
            background: #0c1423;
            border: 1px solid var(--border);
            border-radius: 9px;
            outline: none;
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

        .password-wrapper {
            position: relative;
        }

        .password-wrapper .form-input {
            padding-right: 62px;
        }

        .toggle-password {
            position: absolute;
            top: 50%;
            right: 12px;
            transform: translateY(-50%);
            border: none;
            background: none;
            color: #94a3b8;
            cursor: pointer;
            font-size: 11px;
            font-family: inherit;
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
            height: 49px;
            margin-top: 25px;
            border: none;
            border-radius: 10px;
            color: white;
            background: linear-gradient(135deg, #818cf8, #6366f1);
            font-family: inherit;
            font-size: 13px;
            font-weight: 700;
            cursor: pointer;
            box-shadow: 0 8px 20px rgba(99, 102, 241, .2);
            transition: .25s;
        }

        .submit-btn:hover {
            transform: translateY(-2px);
            background: linear-gradient(135deg, #6366f1, #4f46e5);
            box-shadow: 0 10px 25px rgba(99, 102, 241, .3);
        }

        .login-link {
            margin-top: 25px;
            text-align: center;
            color: var(--muted);
            font-size: 12px;
        }

        .login-link a {
            color: #a5b4fc;
            text-decoration: none;
            font-weight: 700;
        }

        .login-link a:hover {
            color: white;
            text-decoration: underline;
        }

        .security-note {
            margin-top: 25px;
            color: #64748b;
            font-size: 10px;
            text-align: center;
        }

        @media (max-width: 950px) {
            .container {
                max-width: 650px;
                grid-template-columns: 1fr;
            }

            .branding {
                display: none;
            }

            .register-section {
                padding: 40px;
            }
        }

        @media (max-width: 520px) {
            body {
                padding: 12px;
                align-items: flex-start;
            }

            .container {
                border-radius: 17px;
            }

            .register-section {
                padding: 25px 20px;
            }

            .register-content {
                margin-top: 30px;
            }

            .heading h2 {
                font-size: 25px;
            }

            .form-grid {
                grid-template-columns: 1fr;
                gap: 17px;
            }

            .form-group.full {
                grid-column: auto;
            }
        }
    </style>
</head>

<body>

<main class="container">

    <section class="branding">

        <div class="brand">
            <div class="brand-icon">✦</div>
            <span>IAM Platform</span>
        </div>

        <div class="brand-content">

            <span class="tag">JOIN OUR PLATFORM</span>

            <h1>
                One identity.
                <span>Endless possibilities.</span>
            </h1>

            <p>
                Create your account and experience a secure,
                centralized identity platform built around
                simplicity, privacy, and trust.
            </p>

            <div class="benefits">
                <div class="benefit">
                    <span class="benefit-icon">✓</span>
                    Secure account management
                </div>

                <div class="benefit">
                    <span class="benefit-icon">✓</span>
                    Modern OAuth 2.1 authentication
                </div>

                <div class="benefit">
                    <span class="benefit-icon">✓</span>
                    Your identity, under your control
                </div>
            </div>

        </div>

        <div class="copyright">
            © 2026 IAM Platform. All rights reserved.
        </div>

    </section>

    <section class="register-section">

        <nav class="top-bar" aria-label="Language selection">

            <span class="language-label">
                ${messages.get("common.language")}
            </span>

            <div class="language-switch">
                <a href="?lang=en"
                   class="${lang.equals("en") ? "active" : ""}">
                    EN
                </a>

                <a href="?lang=km"
                   class="${lang.equals("km") ? "active" : ""}">
                    ខ្មែរ
                </a>
            </div>

        </nav>

        <div class="register-content">

            <header class="heading">
                <h2>${messages.get("register.title")}</h2>

                <p>
                    Create your account to get started.
                    It only takes a few minutes.
                </p>
            </header>

            @if (!errors.isEmpty())
                <div class="error-list" role="alert">
                    @for (String error : errors)
                        <div class="error-message">
                            ✕ &nbsp; ${error}
                        </div>
                    @endfor
                </div>
            @endif

            <form class="register-form" method="post" action="/register">

                @if (csrf != null)
                    <input
                            type="hidden"
                            name="${csrf.getParameterName()}"
                            value="${csrf.getToken()}">
                @endif

                <div class="form-grid">

                    <div class="form-group">
                        <label class="form-label" for="username">
                            ${messages.get("register.username")}
                        </label>

                        <input
                                class="form-input"
                                type="text"
                                id="username"
                                name="username"
                                value="${form.getUsername()}"
                                placeholder="Enter username"
                                autocomplete="username"
                                maxlength="50"
                                required>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="email">
                            ${messages.get("register.email")}
                        </label>

                        <input
                                class="form-input"
                                type="email"
                                id="email"
                                name="email"
                                value="${form.getEmail()}"
                                placeholder="name@example.com"
                                autocomplete="email"
                                required>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="givenName">
                            ${messages.get("register.givenName")}
                        </label>

                        <input
                                class="form-input"
                                type="text"
                                id="givenName"
                                name="givenName"
                                value="${form.getGivenName()}"
                                placeholder="First name"
                                autocomplete="given-name"
                                required>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="familyName">
                            ${messages.get("register.familyName")}
                        </label>

                        <input
                                class="form-input"
                                type="text"
                                id="familyName"
                                name="familyName"
                                value="${form.getFamilyName()}"
                                placeholder="Last name"
                                autocomplete="family-name"
                                required>
                    </div>

                    <div class="form-group">
                        <label class="form-label" for="password">
                            ${messages.get("register.password")}
                        </label>

                        <div class="password-wrapper">
                            <input
                                    class="form-input"
                                    type="password"
                                    id="password"
                                    name="password"
                                    placeholder="Create password"
                                    autocomplete="new-password"
                                    minlength="8"
                                    required>

                            <button
                                    class="toggle-password"
                                    type="button"
                                    data-target="password">
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
                            ${messages.get("register.confirmPassword")}
                        </label>

                        <div class="password-wrapper">
                            <input
                                    class="form-input"
                                    type="password"
                                    id="confirmPassword"
                                    name="confirmPassword"
                                    placeholder="Confirm password"
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

                    <div class="form-group full">
                        <label class="form-label" for="locale">
                            ${messages.get("register.locale")}
                        </label>

                        <select class="form-select" id="locale" name="locale">

                            @if ("en".equals(form.getLocale()))
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

                <button class="submit-btn" type="submit">
                    ${messages.get("register.submit")} →
                </button>

            </form>

            <p class="login-link">
                Already have an account?
                <a href="/login">
                    ${messages.get("register.login")}
                </a>
            </p>

            <div class="security-note">
                🔒 Your information is protected by secure authentication.
            </div>

        </div>

    </section>

</main>

<script>
    const password = document.getElementById("password");
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

        const activeColors = [
            "",
            "#ef4444",
            "#f59e0b",
            "#3b82f6",
            "#22c55e"
        ];

        strengthBars.forEach((bar, index) => {
            bar.style.background = index < score
                ? activeColors[score]
                : "#334155";
        });

        strengthLabel.textContent = value.length === 0
            ? labels[0]
            : labels[score];
    }

    function checkPasswordMatch() {
        const first = password.value;
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

    password.addEventListener("input", () => {
        checkStrength(password.value);
        checkPasswordMatch();
    });

    confirmPassword.addEventListener("input", checkPasswordMatch);
</script>

</body>
</html>
