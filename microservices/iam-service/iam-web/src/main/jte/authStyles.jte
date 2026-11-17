    <style>
        :root {
            --bg: #f4f6fb;
            --card: #ffffff;
            --ink: #0f172a;
            --muted: #64748b;
            --subtle: #94a3b8;
            --border: #e2e8f0;
            --input: #ffffff;
            --primary: #2563eb;
            --primary-hover: #1d4ed8;
            --ring: rgba(37, 99, 235, .18);
            --social: #ffffff;
            --social-hover: #f8fafc;
            --danger-bg: #fef2f2; --danger-border: #fecaca; --danger-ink: #b91c1c;
            --ok-bg: #f0fdf4; --ok-border: #bbf7d0; --ok-ink: #15803d;
            --warn-ink: #b45309;
            --shadow: 0 24px 60px -20px rgba(15, 23, 42, .18), 0 2px 6px rgba(15, 23, 42, .04);
        }

        @media (prefers-color-scheme: dark) {
            :root {
                --bg: #0b1120;
                --card: #111827;
                --ink: #f1f5f9;
                --muted: #94a3b8;
                --subtle: #64748b;
                --border: #1f2a3d;
                --input: #0b1220;
                --primary: #3b82f6;
                --primary-hover: #2563eb;
                --ring: rgba(59, 130, 246, .25);
                --social: #0f1729;
                --social-hover: #162036;
                --danger-bg: rgba(239, 68, 68, .1); --danger-border: rgba(239, 68, 68, .3); --danger-ink: #fca5a5;
                --ok-bg: rgba(34, 197, 94, .1); --ok-border: rgba(34, 197, 94, .3); --ok-ink: #86efac;
                --warn-ink: #fbbf24;
                --shadow: 0 30px 80px -20px rgba(0, 0, 0, .6);
            }
            .logo-github { filter: invert(1); }
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            min-height: 100vh;
            background: var(--bg);
            color: var(--ink);
            font-family: "Google Sans", system-ui, sans-serif;
            -webkit-font-smoothing: antialiased;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 32px 16px;
        }

        html[lang="km"] body { line-height: 1.6; }

        .shell {
            width: 100%;
            max-width: 1040px;
            display: grid;
            grid-template-columns: 1.05fr 1fr;
            background: var(--card);
            border: 1px solid var(--border);
            border-radius: 24px;
            overflow: hidden;
            box-shadow: var(--shadow);
        }

        /* ---------- Brand panel ---------- */
        .brand-panel {
            position: relative;
            padding: 44px;
            display: flex;
            flex-direction: column;
            color: #fff;
            overflow: hidden;
            background:
                radial-gradient(600px circle at 0% 0%, rgba(255, 255, 255, .16), transparent 45%),
                radial-gradient(500px circle at 100% 100%, rgba(14, 165, 233, .45), transparent 50%),
                linear-gradient(150deg, #1e40af 0%, #2563eb 55%, #0284c7 100%);
        }

        .brand-panel::after {
            content: "";
            position: absolute;
            inset: 0;
            background-image:
                linear-gradient(rgba(255, 255, 255, .06) 1px, transparent 1px),
                linear-gradient(90deg, rgba(255, 255, 255, .06) 1px, transparent 1px);
            background-size: 36px 36px;
            mask-image: linear-gradient(180deg, transparent, #000 40%, transparent);
            pointer-events: none;
        }

        .brand-panel > * { position: relative; z-index: 1; }

        .brand { display: flex; align-items: center; gap: 12px; }
        .brand img { width: 40px; height: 40px; border-radius: 11px; box-shadow: 0 8px 20px rgba(0, 0, 0, .2); }
        .brand strong { display: block; font-size: 16px; font-weight: 700; }
        .brand small { display: block; font-size: 12px; opacity: .75; }

        .brand-copy { margin: auto 0; padding: 56px 0 40px; max-width: 420px; }

        .tag {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 12px;
            border-radius: 999px;
            background: rgba(255, 255, 255, .14);
            border: 1px solid rgba(255, 255, 255, .22);
            font-size: 12px;
            font-weight: 600;
            letter-spacing: .3px;
        }
        .tag::before { content: ""; width: 7px; height: 7px; border-radius: 50%; background: #4ade80; box-shadow: 0 0 0 3px rgba(74, 222, 128, .25); }

        .brand-copy h1 {
            margin-top: 22px;
            font-size: clamp(30px, 3.4vw, 42px);
            line-height: 1.15;
            letter-spacing: -1px;
            font-weight: 800;
        }
        .brand-copy h1 span { display: block; color: #bae6fd; }
        .brand-copy p { margin-top: 16px; font-size: 15px; line-height: 1.7; color: rgba(255, 255, 255, .82); }

        .features { list-style: none; margin-top: 30px; display: grid; gap: 12px; }
        .features li { display: flex; align-items: center; gap: 12px; font-size: 14px; color: rgba(255, 255, 255, .92); }
        .features svg {
            flex: none;
            width: 28px; height: 28px; padding: 6px;
            border-radius: 8px;
            background: rgba(255, 255, 255, .14);
            border: 1px solid rgba(255, 255, 255, .18);
        }

        .provider-strip { display: flex; align-items: center; gap: 10px; }
        .provider-strip span { font-size: 12px; opacity: .75; margin-right: 4px; }
        .provider-strip img {
            width: 34px; height: 34px; padding: 7px;
            border-radius: 50%;
            background: #fff;
            object-fit: contain;
            box-shadow: 0 4px 12px rgba(0, 0, 0, .15);
        }

        /* ---------- Auth panel ---------- */
        .auth-panel { padding: 40px 48px; display: flex; flex-direction: column; }

        .top-bar { display: flex; justify-content: flex-end; }

        .lang-switch {
            display: inline-flex;
            padding: 3px;
            gap: 2px;
            border: 1px solid var(--border);
            border-radius: 10px;
            background: var(--bg);
        }
        .lang-switch a {
            padding: 5px 11px;
            border-radius: 7px;
            font-size: 12px;
            font-weight: 600;
            color: var(--muted);
            text-decoration: none;
            transition: background .15s, color .15s;
        }
        .lang-switch a:hover { color: var(--ink); }
        .lang-switch a.active { background: var(--card); color: var(--ink); box-shadow: 0 1px 3px rgba(15, 23, 42, .12); }

        .auth-content { width: 100%; max-width: 380px; margin: auto; padding: 28px 0 8px; }

        .mobile-brand { display: none; align-items: center; gap: 10px; margin-bottom: 22px; font-weight: 700; }
        .mobile-brand img { width: 36px; height: 36px; border-radius: 10px; }

        .auth-heading h2 { font-size: 26px; font-weight: 800; letter-spacing: -.6px; }
        .auth-heading p { margin-top: 6px; color: var(--muted); font-size: 14px; line-height: 1.6; }

        .alert {
            display: flex;
            gap: 10px;
            align-items: flex-start;
            margin-top: 20px;
            padding: 12px 14px;
            border-radius: 12px;
            font-size: 13px;
            line-height: 1.5;
            border: 1px solid;
        }
        .alert svg { flex: none; width: 18px; height: 18px; margin-top: 1px; }
        .alert-error { background: var(--danger-bg); border-color: var(--danger-border); color: var(--danger-ink); }
        .alert-success { background: var(--ok-bg); border-color: var(--ok-border); color: var(--ok-ink); }

        /* Social */
        .social-grid { margin-top: 24px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .social-grid.single { grid-template-columns: 1fr; }

        .social-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 10px;
            height: 46px;
            padding: 0 14px;
            border: 1px solid var(--border);
            border-radius: 12px;
            background: var(--social);
            color: var(--ink);
            font-size: 14px;
            font-weight: 600;
            text-decoration: none;
            transition: background .15s, border-color .15s, transform .15s, box-shadow .15s;
        }
        .social-btn img { width: 20px; height: 20px; object-fit: contain; }
        .social-btn:hover { background: var(--social-hover); border-color: var(--subtle); transform: translateY(-1px); box-shadow: 0 6px 14px -6px rgba(15, 23, 42, .2); }
        .social-btn:focus-visible { outline: none; box-shadow: 0 0 0 4px var(--ring); border-color: var(--primary); }
        .social-btn[data-provider="google"]:hover { border-color: #4285f4; }
        .social-btn[data-provider="github"]:hover { border-color: #6e7681; }
        .social-btn[data-provider="facebook"]:hover { border-color: #1877f2; }
        .social-btn[data-provider="telegram"]:hover { border-color: #29a9eb; }

        .divider {
            display: flex;
            align-items: center;
            gap: 14px;
            margin: 24px 0 20px;
            color: var(--subtle);
            font-size: 12px;
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: .6px;
        }
        .divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: var(--border); }

        /* Form */
        .field { margin-bottom: 16px; }
        .field label { display: block; margin-bottom: 7px; font-size: 13px; font-weight: 600; }

        .input-wrap { position: relative; }
        .input-wrap > svg {
            position: absolute;
            left: 14px; top: 50%;
            width: 18px; height: 18px;
            transform: translateY(-50%);
            color: var(--subtle);
            pointer-events: none;
            transition: color .15s;
        }
        .input-wrap:focus-within > svg { color: var(--primary); }

        .input {
            width: 100%;
            height: 46px;
            padding: 0 14px 0 42px;
            border: 1px solid var(--border);
            border-radius: 12px;
            background: var(--input);
            color: var(--ink);
            font: inherit;
            font-size: 14px;
            outline: none;
            transition: border-color .15s, box-shadow .15s;
        }
        .input::placeholder { color: var(--subtle); }
        .input:focus { border-color: var(--primary); box-shadow: 0 0 0 4px var(--ring); }
        .input.has-toggle { padding-right: 72px; }
        .has-error .input { border-color: var(--danger-border); }

        .toggle {
            position: absolute;
            right: 8px; top: 50%;
            transform: translateY(-50%);
            height: 30px;
            padding: 0 10px;
            border: 0;
            border-radius: 8px;
            background: transparent;
            color: var(--muted);
            font: inherit;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
        }
        .toggle:hover { background: var(--bg); color: var(--ink); }
        .toggle:focus-visible { outline: 2px solid var(--primary); }

        .caps { display: none; margin-top: 7px; font-size: 12px; color: var(--warn-ink); }
        .caps.show { display: block; }

        .submit {
            position: relative;
            width: 100%;
            height: 48px;
            margin-top: 6px;
            border: 0;
            border-radius: 12px;
            background: var(--primary);
            color: #fff;
            font: inherit;
            font-size: 14px;
            font-weight: 700;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            box-shadow: 0 10px 22px -10px rgba(37, 99, 235, .7);
            transition: background .15s, transform .15s, box-shadow .15s;
        }
        .submit:hover { background: var(--primary-hover); transform: translateY(-1px); }
        .submit:focus-visible { outline: none; box-shadow: 0 0 0 4px var(--ring); }
        .submit svg { width: 16px; height: 16px; transition: transform .15s; }
        .submit:hover svg { transform: translateX(3px); }
        .submit[disabled] { opacity: .75; cursor: progress; transform: none; }
        .spinner {
            display: none;
            width: 16px; height: 16px;
            border: 2px solid rgba(255, 255, 255, .4);
            border-top-color: #fff;
            border-radius: 50%;
            animation: spin .7s linear infinite;
        }
        .submit.loading .spinner { display: block; }
        .submit.loading svg { display: none; }
        @keyframes spin { to { transform: rotate(360deg); } }

        .register { margin-top: 22px; text-align: center; font-size: 14px; color: var(--muted); }
        .register a { color: var(--primary); font-weight: 600; text-decoration: none; }
        .register a:hover { text-decoration: underline; }

        .secure-note {
            margin-top: 26px;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
            font-size: 12px;
            color: var(--subtle);
        }
        .secure-note svg { width: 14px; height: 14px; }

        /* ---------- Register page ---------- */
        .auth-content.wide { max-width: 460px; }
        .social-caption { margin-top: 22px; font-size: 13px; font-weight: 600; color: var(--muted); }
        .social-caption + .social-grid { margin-top: 10px; }
        .social-grid.icons { grid-template-columns: repeat(4, 1fr); }
        .social-grid.icons .social-btn span { display: none; }
        .social-grid.icons .social-btn img { width: 22px; height: 22px; }

        .alert ul { list-style: none; display: grid; gap: 4px; }

        .row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
        .input.plain { padding-left: 14px; }

        .select {
            appearance: none;
            cursor: pointer;
            background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
            background-repeat: no-repeat;
            background-position: right 12px center;
            background-size: 18px;
            padding-right: 40px;
        }

        .strength { margin-top: 8px; display: flex; align-items: center; gap: 10px; }
        .strength-bars { flex: 1; display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; }
        .strength-bars span { height: 4px; border-radius: 99px; background: var(--border); transition: background .2s; }
        .strength-label { min-width: 64px; text-align: right; font-size: 12px; color: var(--muted); }

        .hint { display: block; min-height: 18px; margin-top: 6px; font-size: 12px; }
        .hint.ok { color: var(--ok-ink); }
        .hint.bad { color: var(--danger-ink); }
        .input.invalid { border-color: var(--danger-border); }
        .input.valid { border-color: var(--ok-border); }

        .terms { margin-top: 14px; font-size: 12px; line-height: 1.6; color: var(--subtle); text-align: center; }

        @media (max-width: 480px) {
            .row { grid-template-columns: 1fr; gap: 0; }
            .social-grid.icons { grid-template-columns: repeat(2, 1fr); }
            .social-grid.icons .social-btn span { display: inline; }
        }

        @media (prefers-reduced-motion: reduce) {
            * { transition: none !important; animation-duration: 1.5s !important; }
        }

        @media (max-width: 880px) {
            .shell { max-width: 480px; grid-template-columns: 1fr; }
            .brand-panel { display: none; }
            .mobile-brand { display: flex; }
        }

        @media (max-width: 480px) {
            body { padding: 0; align-items: stretch; background: var(--card); }
            .shell { border: 0; border-radius: 0; box-shadow: none; }
            .auth-panel { padding: 20px 16px 28px; }
            .social-grid { grid-template-columns: 1fr; }
        }
    </style>
