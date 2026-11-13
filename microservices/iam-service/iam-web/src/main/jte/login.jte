



@param java.util.Map<String, String> messages
@param String lang
@param boolean hasError
@param String errorMessage
@param boolean registered
@param java.util.List<String> socialProviders
@param org.springframework.security.web.csrf.CsrfToken csrf

<!doctype html>
<html lang="${lang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#090e1b">

    <title>${messages.get("login.title")} | IAM Platform</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700;800&family=Noto+Sans+Khmer:wght@400;500;600;700&display=swap" rel="stylesheet">

    <style>
        :root {
            --bg: #090e1b;
            --surface: #111a2b;
            --surface-light: #172238;
            --primary: #818cf8;
            --primary-hover: #6366f1;
            --text: #f8fafc;
            --muted: #94a3b8;
            --border: rgba(148, 163, 184, 0.16);
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
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 32px 20px;
            overflow-x: hidden;
        }

        body:before,
        body:after {
            content: "";
            position: fixed;
            width: 450px;
            height: 450px;
            border-radius: 50%;
            filter: blur(130px);
            opacity: .17;
            pointer-events: none;
        }

        body:before {
            background: #6366f1;
            top: -180px;
            left: -150px;
        }

        body:after {
            background: #2563eb;
            bottom: -200px;
            right: -150px;
        }

        .container {
            width: 100%;
            max-width: 1120px;
            min-height: 680px;
            display: grid;
            grid-template-columns: 1fr 1fr;
            position: relative;
            z-index: 1;
            background: rgba(17, 26, 43, .75);
            border: 1px solid var(--border);
            border-radius: 24px;
            overflow: hidden;
            box-shadow: 0 30px 100px rgba(0, 0, 0, .4);
        }

        /* Left branding section */

        .branding {
            padding: 48px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            overflow: hidden;
            background:
                    radial-gradient(circle at 20% 20%, rgba(99, 102, 241, .23), transparent 40%),
                    linear-gradient(145deg, #141e36, #101827 70%);
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 20px;
            font-weight: 800;
            letter-spacing: -.5px;
        }

        .brand-icon {
            width: 42px;
            height: 42px;
            display: grid;
            place-items: center;
            border-radius: 13px;
            color: white;
            font-size: 22px;
            background: linear-gradient(135deg, #818cf8, #4f46e5);
            box-shadow: 0 8px 24px rgba(99, 102, 241, .3);
        }

        .brand-description {
            max-width: 420px;
            margin-top: auto;
            margin-bottom: auto;
            padding: 70px 0;
        }

        .brand-description .tag {
            display: inline-block;
            padding: 7px 13px;
            border: 1px solid rgba(129, 140, 248, .3);
            background: rgba(99, 102, 241, .1);
            border-radius: 30px;
            color: #a5b4fc;
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 1px;
            text-transform: uppercase;
        }

        .brand-description h1 {
            margin-top: 24px;
            font-size: clamp(36px, 4vw, 52px);
            line-height: 1.15;
            letter-spacing: -2px;
            font-weight: 800;
        }

        .brand-description h1 span {
            color: #818cf8;
        }

        .brand-description p {
            margin-top: 20px;
            color: #9caac0;
            line-height: 1.8;
            font-size: 15px;
        }

        .features {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .feature {
            display: flex;
            align-items: center;
            gap: 12px;
            color: #cbd5e1;
            font-size: 14px;
        }

        .feature-icon {
            width: 30px;
            height: 30px;
            display: grid;
            place-items: center;
            border-radius: 9px;
            background: rgba(129, 140, 248, .12);
            color: #a5b4fc;
        }

        .copyright {
            margin-top: 45px;
            color: #64748b;
            font-size: 12px;
        }

        /* Right authentication section */

        .auth-section {
            padding: 48px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            position: relative;
        }

        .top-bar {
            position: absolute;
            top: 35px;
            right: 40px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .language-label {
            color: var(--muted);
            font-size: 12px;
        }

        .language-switch {
            display: flex;
            align-items: center;
            gap: 5px;
            padding: 5px;
            background: #172238;
            border: 1px solid var(--border);
            border-radius: 9px;
        }

        .language-switch a {
            padding: 6px 9px;
            text-decoration: none;
            color: #94a3b8;
            border-radius: 6px;
            font-size: 12px;
            transition: .2s;
        }

        .language-switch a.active,
        .language-switch a:hover {
            background: #334155;
            color: white;
        }

        .auth-content {
            width: 100%;
            max-width: 390px;
            margin: 35px auto 0;
        }

        .auth-heading h2 {
            font-size: 30px;
            font-weight: 800;
            letter-spacing: -1px;
        }

        .auth-heading p {
            margin-top: 9px;
            color: var(--muted);
            font-size: 14px;
            line-height: 1.7;
        }

        /* Alerts */

        .alert {
            padding: 13px 15px;
            margin-top: 24px;
            border-radius: 10px;
            font-size: 13px;
            line-height: 1.6;
        }

        .alert-error {
            color: #fca5a5;
            background: rgba(239, 68, 68, .1);
            border: 1px solid rgba(239, 68, 68, .25);
        }

        .alert-success {
            color: #86efac;
            background: rgba(34, 197, 94, .1);
            border: 1px solid rgba(34, 197, 94, .25);
        }

        /* Form */

        .login-form {
            margin-top: 30px;
        }

        .form-group {
            margin-bottom: 21px;
        }

        .form-label {
            display: block;
            margin-bottom: 9px;
            font-size: 13px;
            font-weight: 600;
            color: #e2e8f0;
        }

        .input-wrapper {
            position: relative;
        }

        .form-input {
            width: 100%;
            height: 49px;
            padding: 0 15px;
            color: white;
            font-family: inherit;
            font-size: 14px;
            background: #0c1423;
            border: 1px solid var(--border);
            border-radius: 10px;
            outline: none;
            transition: .2s;
        }

        .form-input::placeholder {
            color: #64748b;
        }

        .form-input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(129, 140, 248, .12);
        }

        .password-input {
            padding-right: 65px;
        }

        .password-toggle {
            position: absolute;
            right: 14px;
            top: 50%;
            transform: translateY(-50%);
            border: none;
            background: none;
            color: #94a3b8;
            cursor: pointer;
            font-family: inherit;
            font-size: 12px;
        }

        .password-toggle:hover {
            color: white;
        }

        .submit-btn {
            width: 100%;
            height: 50px;
            border: none;
            border-radius: 10px;
            background: linear-gradient(135deg, #818cf8, #6366f1);
            color: white;
            font-family: inherit;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            transition: .25s;
            box-shadow: 0 8px 20px rgba(99, 102, 241, .2);
        }

        .submit-btn:hover {
            transform: translateY(-2px);
            background: linear-gradient(135deg, #6366f1, #4f46e5);
            box-shadow: 0 10px 28px rgba(99, 102, 241, .3);
        }

        /* Divider */

        .divider {
            display: flex;
            align-items: center;
            gap: 14px;
            margin: 28px 0;
            color: #64748b;
            font-size: 12px;
        }

        .divider:before,
        .divider:after {
            content: "";
            height: 1px;
            flex: 1;
            background: var(--border);
        }

        /* Social login */

        .social-list {
            display: flex;
            flex-direction: column;
            gap: 11px;
        }

        .social-btn {
            min-height: 46px;
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 10px;
            padding: 10px 15px;
            text-decoration: none;
            color: #e2e8f0;
            background: #172238;
            border: 1px solid var(--border);
            border-radius: 10px;
            font-size: 13px;
            font-weight: 600;
            transition: .2s;
        }

        .social-btn:hover {
            background: #23314b;
            border-color: #64748b;
        }

        .social-symbol {
            font-size: 16px;
            font-weight: 800;
        }

        /* Register */

        .register-text {
            margin-top: 30px;
            color: var(--muted);
            font-size: 13px;
            text-align: center;
        }

        .register-text a {
            color: #a5b4fc;
            font-weight: 700;
            text-decoration: none;
        }

        .register-text a:hover {
            color: white;
            text-decoration: underline;
        }

        .security-note {
            margin-top: 30px;
            text-align: center;
            color: #64748b;
            font-size: 11px;
        }

        @media (max-width: 850px) {
            .container {
                max-width: 550px;
                grid-template-columns: 1fr;
            }

            .branding {
                display: none;
            }

            .auth-section {
                min-height: 680px;
                padding: 45px 35px;
            }
        }

        @media (max-width: 480px) {
            body {
                padding: 12px;
            }

            .container {
                border-radius: 17px;
            }

            .auth-section {
                padding: 35px 22px;
            }

            .top-bar {
                top: 20px;
                right: 20px;
            }

            .auth-content {
                margin-top: 55px;
            }

            .auth-heading h2 {
                font-size: 26px;
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

        <div class="brand-description">
            <span class="tag">Identity & Access Management</span>

            <h1>
                Secure your identity.
                <span>Protect your world.</span>
            </h1>

            <p>
                A modern identity and access management platform
                designed to make authentication secure, simple,
                and reliable.
            </p>

            <div class="features">
                <div class="feature">
                    <span class="feature-icon">✓</span>
                    Secure authentication
                </div>

                <div class="feature">
                    <span class="feature-icon">✓</span>
                    OAuth 2.1 & OpenID Connect
                </div>

                <div class="feature">
                    <span class="feature-icon">✓</span>
                    Centralized identity management
                </div>
            </div>
        </div>

        <div class="copyright">
            © 2026 IAM Platform. All rights reserved.
        </div>

    </section>

    <section class="auth-section">

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

        <div class="auth-content">

            <header class="auth-heading">
                <h2>${messages.get("login.title")}</h2>

                <p>
                    Welcome back! Please enter your credentials
                    to access your account.
                </p>
            </header>

            @if (hasError)
                <div class="alert alert-error" role="alert">
                    ✕ &nbsp; ${errorMessage != null ? errorMessage : messages.get("login.invalid")}
                </div>
            @endif

            @if (registered)
                <div class="alert alert-success" role="status">
                    ✓ &nbsp; ${messages.get("login.registered")}
                </div>
            @endif

            <form class="login-form" method="post" action="/login">

                @if (csrf != null)
                    <input
                            type="hidden"
                            name="${csrf.getParameterName()}"
                            value="${csrf.getToken()}">
                @endif

                <div class="form-group">
                    <label class="form-label" for="username">
                        ${messages.get("login.username")}
                    </label>

                    <input
                            class="form-input"
                            type="text"
                            id="username"
                            name="username"
                            placeholder="Enter your username"
                            autocomplete="username"
                            required
                            autofocus>
                </div>

                <div class="form-group">
                    <label class="form-label" for="password">
                        ${messages.get("login.password")}
                    </label>

                    <div class="input-wrapper">
                        <input
                                class="form-input password-input"
                                type="password"
                                id="password"
                                name="password"
                                placeholder="Enter your password"
                                autocomplete="current-password"
                                required>

                        <button
                                class="password-toggle"
                                type="button"
                                id="togglePassword"
                                aria-label="Show password"
                                aria-pressed="false">
                            Show
                        </button>
                    </div>
                </div>

                <button class="submit-btn" type="submit">
                    ${messages.get("login.submit")} →
                </button>

            </form>

            @if (!socialProviders.isEmpty())
                <div class="divider">
                    ${messages.get("login.continueWith")}
                </div>

                <div class="social-list">

                    @for (String provider : socialProviders)
                        <a
                                class="social-btn"
                                href="/login/social/${provider}">

                            <span class="social-symbol">✦</span>

                            ${messages.get("login." + provider)}
                        </a>
                    @endfor

                </div>
            @endif

            <p class="register-text">
                Don't have an account?
                <a href="/register">
                    ${messages.get("login.register")}
                </a>
            </p>

            <div class="security-note">
                🔒 &nbsp; Your connection is protected and encrypted.
            </div>

        </div>

    </section>

</main>

<script>
    const passwordInput = document.getElementById("password");
    const togglePassword = document.getElementById("togglePassword");

    togglePassword.addEventListener("click", () => {
        const isPassword = passwordInput.type === "password";

        passwordInput.type = isPassword ? "text" : "password";
        togglePassword.textContent = isPassword ? "Hide" : "Show";
        togglePassword.setAttribute("aria-label",
            isPassword ? "Hide password" : "Show password");
        togglePassword.setAttribute("aria-pressed", String(isPassword));
    });
</script>

</body>
</html>
