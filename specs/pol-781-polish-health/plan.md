# POL-781 — Plan

## Summary

Add an env health banner that mounts above the existing shared chrome
on every demo page. The banner pings `<apiOrigin>/api/rest/v1/health`
on load and every 30s, displays env + hostname + status, and uses a
30s in-memory cache to avoid spamming the endpoint. Wiring goes into
`bootstrap()` so per-page HTML stays untouched. POL-780 (next ticket,
also on `chrome.js`) will rebase on top of this commit; the additions
sit in a clearly delimited "Health banner" section to keep diffs
isolated.

## Files

- `docs/assets/chrome.js` — add a clearly-fenced "Health banner"
  section near the bottom exporting:
  - `pingHealth(apiOrigin, opts?)` — pure async fetcher with 30s
    in-memory cache + 5s `AbortController` timeout. Returns
    `{ healthy, checkedAt }`.
  - `installHealthBanner(containerEl, opts?)` — DOM mount that
    composes env label + hostname + status, performs an immediate
    `pingHealth()` and schedules a 30s `setInterval`. Idempotent: a
    second mount on the same element clears the prior interval.

  Update the file's top-of-file comment to acknowledge that the
  Health banner section deliberately includes DOM access (mirrors
  what POL-780 will do for copy handlers).

- `docs/assets/chrome-render.js` — extend `bootstrap()` to:
  1. Locate or inject a `<div id="chrome-health">` element directly
     above `<div id="chrome">` (or at the top of `<body>` if `#chrome`
     is missing).
  2. Call `installHealthBanner(healthEl)`.

  The injection is opt-out via `opts.skipHealthBanner` for safety,
  but no current page sets it.

- `docs/assets/chrome.css` — add styles for `.chrome-health-banner`
  (flex row, sticky-friendly, sits above `.polst-chrome`),
  `.chrome-health-banner__env` (uppercase mono-ish label),
  `.chrome-health-banner__sep` (subtle dot/middot divider),
  `.chrome-health-banner__host` (muted),
  `.chrome-health-banner__status` (live region wrapper),
  `.chrome-health-dot` (8px circle, base grey for "checking"),
  `.chrome-health-dot.is-healthy` (green),
  `.chrome-health-dot.is-down` (red).

## API shapes

```js
// chrome.js — Health banner section
const HEALTH_CACHE_MS = 30_000;
const HEALTH_TIMEOUT_MS = 5_000;
const HEALTH_REFRESH_MS = 30_000;
const _healthCache = new Map(); // apiOrigin -> { healthy, checkedAt }

export async function pingHealth(apiOrigin, opts = {}) {
  const now = Date.now();
  const cached = _healthCache.get(apiOrigin);
  if (cached && now - cached.checkedAt < HEALTH_CACHE_MS && !opts.force) {
    return cached;
  }
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), HEALTH_TIMEOUT_MS);
  let healthy = false;
  try {
    const res = await fetch(`${apiOrigin}/api/rest/v1/health`, {
      method: 'GET',
      cache: 'no-store',
      signal: controller.signal,
    });
    healthy = res.ok;
  } catch {
    healthy = false;
  } finally {
    clearTimeout(timer);
  }
  const result = { healthy, checkedAt: Date.now() };
  _healthCache.set(apiOrigin, result);
  return result;
}

export function installHealthBanner(containerEl, opts = {}) {
  if (!containerEl) return () => {};
  // build DOM, run check, schedule interval, return teardown
}
```

## Banner DOM

```
<div id="chrome-health" class="chrome-health-banner">
  <span class="chrome-health-banner__env">STAGING</span>
  <span class="chrome-health-banner__sep" aria-hidden="true">·</span>
  <span class="chrome-health-banner__host">staging-api.polst.app</span>
  <span class="chrome-health-banner__sep" aria-hidden="true">·</span>
  <span class="chrome-health-banner__status" aria-live="polite">
    <span class="chrome-health-dot" aria-hidden="true"></span>
    <span class="chrome-health-banner__label">checking…</span>
  </span>
</div>
```

State transitions toggle `is-healthy` / `is-down` on the dot and
swap the label text between `API healthy`, `API down`, and
`checking…`.

## bootstrap() wiring

```js
export function bootstrap(opts) {
  const run = () => {
    const chromeEl = document.getElementById('chrome');
    const demoEl = document.getElementById('demo');
    if (!opts || !opts.skipHealthBanner) {
      const healthEl = ensureHealthMount(chromeEl);
      if (healthEl) installHealthBanner(healthEl);
    }
    if (chromeEl) renderChrome(chromeEl, opts);
    if (demoEl && !getPolstTarget()) renderEmptyState(demoEl);
  };
  // ...same readyState gating as before
}
```

`ensureHealthMount` returns an existing `#chrome-health` element if
present, otherwise creates one and inserts it before `#chrome` (or
prepends to `<body>` if `#chrome` is absent).

## QA

- `node --check docs/assets/chrome.js`
- `node --check docs/assets/chrome-render.js`
- HTML structural review on `docs/iframe/index.html` and
  `docs/sdk/index.html` — confirm the existing `<div id="chrome">`
  shell is unchanged and the banner mounts above it via JS.
- Live ping is not exercised offline (CORS + remote envs); the
  static-server check is purely "page parses + chrome + banner DOM
  appears". Banner will show "checking…" forever in a sandboxed
  static-only test, which is acceptable: green/red transitions are
  validated by the GH Pages deployment against real envs.
- Marker file written after the parse + structural reviews pass.

## Out of scope

- POL-780's `installCopyHandlers()` — separate ticket, will land
  after this commit.
- Cross-tab cache sharing.
- Surfacing detailed diagnostics in the banner UI.
