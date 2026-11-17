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
    <meta name="theme-color" content="#2563eb">
    <title>${messages.get("register.title")} | ISTAD IAM</title>
    <link rel="icon" type="image/svg+xml" href="/favicon.svg">

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Google+Sans:wght@400;500;600;700&family=Google+Sans+Code:wght@400;500&display=swap" rel="stylesheet">

    @template.authStyles()
</head>
<body>

<main class="shell">

    <!-- Brand panel -->
    <section class="brand-panel" aria-hidden="true">
        <div class="brand">
            <img src="/favicon.svg" alt="">
            <div>
                <strong>ISTAD IAM</strong>
                <small>${messages.get("login.brandTag")}</small>
            </div>
        </div>

        <div class="brand-copy">
            <span class="tag">${messages.get("register.brandTag")}</span>
            <h1>
                ${messages.get("register.brandHeadline")}
                <span>${messages.get("register.brandHighlight")}</span>
            </h1>
            <p>${messages.get("register.brandDescription")}</p>

            <ul class="features">
                <li>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                    ${messages.get("register.feature1")}
                </li>
                <li>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                    ${messages.get("register.feature2")}
                </li>
                <li>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                    ${messages.get("register.feature3")}
                </li>
            </ul>
        </div>

    </section>

    <!-- Register panel -->
    <section class="auth-panel">
        <nav class="top-bar" aria-label="${messages.get("common.language")}">
            <div class="lang-switch">
                <a href="?lang=en" class="${lang.equals("en") ? "active" : ""}" hreflang="en"><img class="lang-flag" src="/images/english-flag.png" alt="" width="18" height="12" style="width:18px;height:12px;object-fit:cover;border-radius:2px;vertical-align:-1px;margin-right:6px;box-shadow:0 0 0 1px rgba(0,0,0,.12)">EN</a>
                <a href="?lang=km" class="${lang.equals("km") ? "active" : ""}" hreflang="km"><img class="lang-flag" src="/images/khmer-flag.png" alt="" width="18" height="12" style="width:18px;height:12px;object-fit:cover;border-radius:2px;vertical-align:-1px;margin-right:6px;box-shadow:0 0 0 1px rgba(0,0,0,.12)">ខ្មែរ</a>
            </div>
        </nav>

        <div class="auth-content wide">
            <div class="mobile-brand">
                <img src="/favicon.svg" alt="">
                <span>ISTAD IAM</span>
            </div>

            <header class="auth-heading">
                <h2>${messages.get("register.title")}</h2>
                <p>${messages.get("register.subtitle")}</p>
            </header>

            @if (!errors.isEmpty())
                <div class="alert alert-error" role="alert">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
                    <ul>
                        @for (String error : errors)
                            <li>${error}</li>
                        @endfor
                    </ul>
                </div>
            @endif


            <form id="registerForm" method="post" action="/register" novalidate>
                @if (csrf != null)
                    <input type="hidden" name="${csrf.getParameterName()}" value="${csrf.getToken()}">
                @endif

                <div class="row">
                    <div class="field">
                        <label for="givenName">${messages.get("register.givenName")}</label>
                        <input class="input plain" type="text" id="givenName" name="givenName"
                               value="${form.getGivenName()}" placeholder="${messages.get("register.givenNamePlaceholder")}"
                               autocomplete="given-name" required autofocus>
                    </div>
                    <div class="field">
                        <label for="familyName">${messages.get("register.familyName")}</label>
                        <input class="input plain" type="text" id="familyName" name="familyName"
                               value="${form.getFamilyName()}" placeholder="${messages.get("register.familyNamePlaceholder")}"
                               autocomplete="family-name" required>
                    </div>
                </div>

                <div class="field">
                    <label for="username">${messages.get("register.username")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>
                        <input class="input" type="text" id="username" name="username"
                               value="${form.getUsername()}" placeholder="${messages.get("register.usernamePlaceholder")}"
                               autocomplete="username" autocapitalize="none" spellcheck="false" maxlength="50" required>
                    </div>
                </div>

                <div class="field">
                    <label for="email">${messages.get("register.email")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>
                        <input class="input" type="email" id="email" name="email"
                               value="${form.getEmail()}" placeholder="${messages.get("register.emailPlaceholder")}"
                               autocomplete="email" spellcheck="false" required>
                    </div>
                </div>

                <div class="field">
                    <label for="password">${messages.get("register.password")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>
                        <input class="input has-toggle" type="password" id="password" name="password"
                               placeholder="${messages.get("register.passwordPlaceholder")}"
                               autocomplete="new-password" minlength="8" required>
                        <button class="toggle" type="button" data-target="password" aria-controls="password" aria-pressed="false">
                            ${messages.get("login.showPassword")}
                        </button>
                    </div>
                    <div class="strength" aria-live="polite">
                        <div class="strength-bars" aria-hidden="true"><span></span><span></span><span></span><span></span></div>
                        <span class="strength-label" id="strengthLabel">${messages.get("register.strength")}</span>
                    </div>
                    <p class="caps" id="capsWarning" role="status">${messages.get("login.capsLock")}</p>
                </div>

                <div class="field">
                    <label for="confirmPassword">${messages.get("register.confirmPassword")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3 4 6v6c0 5 3.4 8.6 8 10 4.6-1.4 8-5 8-10V6l-8-3z"/><path d="m9 12 2 2 4-4"/></svg>
                        <input class="input has-toggle" type="password" id="confirmPassword" name="confirmPassword"
                               placeholder="${messages.get("register.confirmPlaceholder")}"
                               autocomplete="new-password" minlength="8" required>
                        <button class="toggle" type="button" data-target="confirmPassword" aria-controls="confirmPassword" aria-pressed="false">
                            ${messages.get("login.showPassword")}
                        </button>
                    </div>
                    <span class="hint" id="matchMessage" aria-live="polite"></span>
                </div>

                <div class="field">
                    <label for="locale">${messages.get("register.locale")}</label>
                    <div class="input-wrap">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/></svg>
                        <select class="input select" id="locale" name="locale">
                            <option value="en" selected="${"en".equals(form.getLocale())}">${messages.get("common.english")}</option>
                            <option value="km" selected="${"km".equals(form.getLocale())}">${messages.get("common.khmer")}</option>
                        </select>
                    </div>
                </div>

                <button class="submit" type="submit" id="submitBtn" data-loading="${messages.get("register.creating")}">
                    <span class="spinner" aria-hidden="true"></span>
                    <span class="label">${messages.get("register.submit")}</span>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg>
                </button>

                <p class="terms">${messages.get("register.terms")}</p>
            </form>

            <p class="register">
                ${messages.get("register.haveAccount")}
                <a href="/login">${messages.get("register.signIn")}</a>
            </p>
        </div>
    </section>
</main>

<div id="i18n" hidden
     data-show="${messages.get("login.showPassword")}"
     data-hide="${messages.get("login.hidePassword")}"
     data-strength="${messages.get("register.strength")}"
     data-s1="${messages.get("register.strength1")}"
     data-s2="${messages.get("register.strength2")}"
     data-s3="${messages.get("register.strength3")}"
     data-s4="${messages.get("register.strength4")}"
     data-match="${messages.get("register.match")}"
     data-no-match="${messages.get("register.noMatch")}"></div>

<script>
    (function () {
        var t = document.getElementById("i18n").dataset;
        var form = document.getElementById("registerForm");
        var password = document.getElementById("password");
        var confirm = document.getElementById("confirmPassword");
        var strengthLabel = document.getElementById("strengthLabel");
        var bars = document.querySelectorAll(".strength-bars span");
        var match = document.getElementById("matchMessage");
        var caps = document.getElementById("capsWarning");
        var submit = document.getElementById("submitBtn");
        var submitLabel = submit.querySelector(".label").textContent;

        document.querySelectorAll(".toggle").forEach(function (button) {
            button.addEventListener("click", function () {
                var input = document.getElementById(button.dataset.target);
                var reveal = input.type === "password";
                input.type = reveal ? "text" : "password";
                button.textContent = reveal ? t.hide : t.show;
                button.setAttribute("aria-pressed", String(reveal));
                input.focus();
            });
        });

        var colors = ["", "#ef4444", "#f59e0b", "#3b82f6", "#22c55e"];
        var labels = [t.strength, t.s1, t.s2, t.s3, t.s4];

        function strength(value) {
            if (!value) return 0;
            var score = 0;
            if (value.length >= 8) score++;
            if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score++;
            if (/\d/.test(value)) score++;
            if (/[^a-zA-Z0-9]/.test(value)) score++;
            return Math.max(score, 1);
        }

        function updateStrength() {
            var score = strength(password.value);
            bars.forEach(function (bar, i) { bar.style.background = i < score ? colors[score] : ""; });
            strengthLabel.textContent = labels[score];
            strengthLabel.style.color = score ? colors[score] : "";
        }

        function updateMatch() {
            if (!confirm.value) {
                match.textContent = "";
                match.className = "hint";
                confirm.classList.remove("valid", "invalid");
                confirm.setCustomValidity("");
                return;
            }
            var ok = password.value === confirm.value;
            match.textContent = ok ? "✓ " + t.match : "✕ " + t.noMatch;
            match.className = "hint " + (ok ? "ok" : "bad");
            confirm.classList.toggle("valid", ok);
            confirm.classList.toggle("invalid", !ok);
            confirm.setCustomValidity(ok ? "" : t.noMatch);
        }

        password.addEventListener("input", function () { updateStrength(); updateMatch(); });
        confirm.addEventListener("input", updateMatch);

        function checkCaps(event) {
            if (event.getModifierState) caps.classList.toggle("show", event.getModifierState("CapsLock"));
        }
        [password, confirm].forEach(function (input) {
            input.addEventListener("keydown", checkCaps);
            input.addEventListener("keyup", checkCaps);
            input.addEventListener("blur", function () { caps.classList.remove("show"); });
        });

        form.addEventListener("submit", function (event) {
            updateMatch();
            if (!form.checkValidity()) {
                event.preventDefault();
                form.reportValidity();
                return;
            }
            submit.disabled = true;
            submit.classList.add("loading");
            submit.querySelector(".label").textContent = submit.dataset.loading;
        });

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
