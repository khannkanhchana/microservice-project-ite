@param java.util.Map<String, String> messages
@param String lang

!{boolean km = "km".equals(lang);}
<div class="lang-menu" id="langMenu">
    <button type="button" class="lang-trigger" id="langTrigger" aria-haspopup="menu" aria-expanded="false"
            aria-label="${messages.get("common.language")}: ${km ? "ភាសាខ្មែរ" : "English"}">
        <img class="lang-flag" src="${km ? "/images/khmer-flag.png" : "/images/english-flag.png"}" alt="" width="20" height="14">
        <span lang="${lang}">${km ? "ខ្មែរ" : "EN"}</span>
        <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
    </button>
    <div class="lang-list" id="langList" role="menu" aria-label="${messages.get("common.language")}" hidden>
        <a role="menuitemradio" aria-checked="${km ? "false" : "true"}" href="?lang=en" hreflang="en" lang="en" class="${!km ? "selected" : ""}">
            <img class="lang-flag" src="/images/english-flag.png" alt="" width="20" height="14">
            <span>English</span>
            @if (!km)<svg class="check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>@endif
        </a>
        <a role="menuitemradio" aria-checked="${km ? "true" : "false"}" href="?lang=km" hreflang="km" lang="km" class="${km ? "selected" : ""}">
            <img class="lang-flag" src="/images/khmer-flag.png" alt="" width="20" height="14">
            <span>ភាសាខ្មែរ</span>
            @if (km)<svg class="check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>@endif
        </a>
    </div>
</div>
<script>
    (function () {
        // Share the choice with the React app (same origin, same key) so it opens in this language after sign-in.
        try { localStorage.setItem("iam.lang", "${lang}"); } catch (e) { /* storage blocked: ignore */ }

        var menu = document.getElementById("langMenu");
        var trigger = document.getElementById("langTrigger");
        var list = document.getElementById("langList");
        var items = Array.prototype.slice.call(list.querySelectorAll("a"));

        function setOpen(open) {
            list.hidden = !open;
            trigger.setAttribute("aria-expanded", String(open));
            menu.classList.toggle("open", open);
            if (open) (list.querySelector(".selected") || items[0]).focus();
        }
        trigger.addEventListener("click", function () { setOpen(list.hidden); });
        document.addEventListener("mousedown", function (e) { if (!menu.contains(e.target)) setOpen(false); });
        list.addEventListener("keydown", function (e) {
            var i = items.indexOf(document.activeElement);
            if (e.key === "Escape") { setOpen(false); trigger.focus(); }
            else if (e.key === "ArrowDown" || e.key === "ArrowUp") {
                e.preventDefault();
                items[(i + (e.key === "ArrowDown" ? 1 : -1) + items.length) % items.length].focus();
            }
        });
    })();
</script>
