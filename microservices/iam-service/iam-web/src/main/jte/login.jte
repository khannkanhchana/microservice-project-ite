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
    <meta name="theme-color" content="#253c94">
    <title>${messages.get("login.title")} | ISTAD IAM</title>
    <link rel="icon" type="image/png" href="/images/istad-logo.png">

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Google+Sans:wght@400;500;600;700&family=Kantumruy+Pro:wght@400;500;600;700&family=Google+Sans+Code:wght@400;500&display=swap" rel="stylesheet">

    @template.authStyles()
</head>
<body>

<main class="shell">

    <!-- Brand panel -->
    <section class="brand-panel" aria-hidden="true">
        <div class="brand">
            <img src="/images/istad-logo.png" alt="ISTAD">
            <div>
                <strong>ISTAD IAM</strong>
                <small>${messages.get("login.brandTag")}</small>
            </div>
        </div>

        <div class="brand-copy">
            <span class="tag">OAuth 2.1 · OpenID Connect</span>
            <h1>
                ${messages.get("login.brandHeadline")}
                <span>${messages.get("login.brandHighlight")}</span>
            </h1>
            <p>${messages.get("login.brandDescription")}</p>

            <ul class="features">
                <li>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                    ${messages.get("login.feature1")}
                </li>
                <li>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                    ${messages.get("login.feature2")}
                </li>
                <li>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                    ${messages.get("login.feature3")}
                </li>
            </ul>
        </div>
    </section>

    <!-- Auth panel -->
    <section class="auth-panel">
        <nav class="top-bar" aria-label="${messages.get("common.language")}">
            @template.langSwitch(messages = messages, lang = lang)
        </nav>

        <div class="auth-content">
            <div class="mobile-brand">
                <img src="/images/istad-logo.png" alt="ISTAD">
                <span>ISTAD IAM</span>
            </div>

            <header class="auth-heading">
                <h2>${messages.get("login.title")}</h2>
                <p>${messages.get("login.subtitle")}</p>
            </header>

            @if (hasError)
                <div class="alert alert-error" role="alert">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
                    <span>${errorMessage != null ? errorMessage : messages.get("login.invalid")}</span>
                </div>
            @endif

            @if (registered)
                <div class="alert alert-success" role="status">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m8 12 3 3 5-6"/></svg>
                    <span>${messages.get("login.registered")}</span>
                </div>
            @endif

            <form id="loginForm" method="post" action="/login" class="${hasError ? "has-error" : ""}" novalidate>
                @if (csrf != null)
                    <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
                @endif

                <div class="field">
                    <label for="username">${messages.get("login.username")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>
                        <input class="input" type="text" id="username" name="username"
                               placeholder="${messages.get("login.usernamePlaceholder")}"
                               autocomplete="username" autocapitalize="none" spellcheck="false" required autofocus>
                    </div>
                </div>

                <div class="field">
                    <label for="password">${messages.get("login.password")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>
                        <input class="input has-toggle" type="password" id="password" name="password"
                               placeholder="${messages.get("login.passwordPlaceholder")}"
                               autocomplete="current-password" required>
                        <button class="toggle" type="button" id="togglePassword" aria-controls="password" aria-pressed="false"
                                data-show="${messages.get("login.showPassword")}" data-hide="${messages.get("login.hidePassword")}">
                            ${messages.get("login.showPassword")}
                        </button>
                    </div>
                    <p class="caps" id="capsWarning" role="status">${messages.get("login.capsLock")}</p>
                </div>

                <button class="submit" type="submit" id="submitBtn" data-loading="${messages.get("login.signingIn")}">
                    <span class="spinner" aria-hidden="true"></span>
                    <span class="label">${messages.get("login.submit")}</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
                </button>
            </form>

            @if (!socialProviders.isEmpty())
                <div class="divider">${messages.get("login.or")}</div>

                <div class="social-grid ${socialProviders.size() == 1 ? "single" : ""}">
                    @for (String provider : socialProviders)
                        <a class="social-btn" data-provider="${provider}" href="/login/social/${provider}">
                            <img class="logo-${provider}" src="/images/${provider}.png" alt="" width="20" height="20">
                            <span>${messages.get("login." + provider)}</span>
                        </a>
                    @endfor
                </div>
            @endif

            <p class="register">
                ${messages.get("login.noAccount")}
                <a href="/register">${messages.get("login.register")}</a>
            </p>

            <p class="secure-note">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3 4 6v6c0 5 3.4 8.6 8 10 4.6-1.4 8-5 8-10V6l-8-3z"/></svg>
                ${messages.get("login.secureNote")}
            </p>
        </div>
    </section>
</main>

<script>
    (function () {
        var password = document.getElementById("password");
        var toggle = document.getElementById("togglePassword");
        var caps = document.getElementById("capsWarning");
        var form = document.getElementById("loginForm");
        var submit = document.getElementById("submitBtn");
        var submitLabel = submit.querySelector(".label").textContent;

        toggle.addEventListener("click", function () {
            var reveal = password.type === "password";
            password.type = reveal ? "text" : "password";
            toggle.textContent = reveal ? toggle.dataset.hide : toggle.dataset.show;
            toggle.setAttribute("aria-pressed", String(reveal));
            password.focus();
        });

        function checkCaps(event) {
            if (event.getModifierState) caps.classList.toggle("show", event.getModifierState("CapsLock"));
        }
        password.addEventListener("keydown", checkCaps);
        password.addEventListener("keyup", checkCaps);
        password.addEventListener("blur", function () { caps.classList.remove("show"); });

        form.addEventListener("submit", function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                form.reportValidity();
                return;
            }
            submit.disabled = true;
            submit.classList.add("loading");
            submit.querySelector(".label").textContent = submit.dataset.loading;
        });

        // Restore the button if the page is shown again from the back/forward cache.
        window.addEventListener("pageshow", function (event) {
            if (event.persisted) {
                submit.disabled = false;
                submit.classList.remove("loading");
                submit.querySelector(".label").textContent = submitLabel;
            }
        });
    })();
</script>

</body>
</html>
