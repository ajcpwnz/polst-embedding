# POL-781 — Env health banner in shared chrome

**Feature Branch**: `pol-781-polish-health`
**Created**: 2026-05-09
**Status**: Draft
**Input**: User description: "POL-781 POLISH-HEALTH — Env health banner pinging /api/rest/v1/health"

## Goal

Top-of-page banner across every demo page that shows the active env, the
resolved API hostname, and a live up/down indicator pinging
`<apiOrigin>/api/rest/v1/health` on load and every 30 seconds. Doubles
as a passive uptime signal — visitors see at a glance whether the env
they're poking at is actually responsive.

## Scope

- `docs/assets/chrome.js` — add a pure `pingHealth()` helper plus the
  `installHealthBanner()` mount entry point.
- `docs/assets/chrome-render.js` — wire `installHealthBanner()` into
  `bootstrap()` so every demo page gets the banner without per-page
  changes.
- `docs/assets/chrome.css` — `.chrome-health-banner`, `.chrome-health-dot`,
  `.chrome-health-dot.is-healthy`, `.chrome-health-dot.is-down` styles.

No new files, no per-page HTML changes, no build step. Vanilla ESM. No
runtime deps.

## Behaviour

### Banner

Mounted into a fresh `<div id="chrome-health">` element that
`bootstrap()` injects ABOVE the existing `<div id="chrome">`. Format:

```
[STAGING · staging-api.polst.app · ● API healthy]
```

- Env label: uppercased `getEnv()` value (`PROD`, `STAGING`, `CANARY`).
- Hostname: derived from `getApiOrigin(env)` via `new URL(...).hostname`.
- Status: `● API healthy` (green dot + text) or `✕ API down` (red dot
  + text). While the very first ping is in flight: `… checking`.

The dot is decorative (`aria-hidden="true"`); the text "API healthy" /
"API down" / "checking" carries the meaning. The status node has
`aria-live="polite"` so screen readers announce transitions.

### Health ping

`pingHealth(apiOrigin)` issues `fetch(<apiOrigin>/api/rest/v1/health,
{ method: 'GET', cache: 'no-store', signal })` with a 5-second
`AbortController` timeout. Returns `{ healthy: boolean, checkedAt:
number }`.

- Any 2xx response → `healthy: true`.
- Any non-2xx OR network error OR timeout → `healthy: false`.

Result is cached per `apiOrigin` for 30 seconds. Within the cache
window, calls return the cached result without re-issuing the fetch.

### Auto-refresh

`installHealthBanner(container)` performs an immediate check on mount
and then schedules `setInterval(check, 30_000)`. The interval handle
is stored on the container element to make replace-mount idempotent —
calling `installHealthBanner` again on the same element clears the
prior interval first.

Switching env in the existing chrome env-switcher triggers a full page
reload (current behaviour of `setParamAndReload` in
`chrome-render.js`), so the banner naturally re-pings the new env
after reload — no separate `env-changed` event is needed.

## Acceptance

- Banner is present on all 7 demo pages (`iframe`, `script`, `sdk`,
  `api`, `ios`, `android`, `rn`) without per-page HTML changes.
- Banner ticks green against a healthy env (status text "API healthy",
  green dot).
- Switching env via the chrome env-switcher reloads the page; the
  banner then re-pings the newly-selected env's `/api/rest/v1/health`.
- If staging is broken, the staging banner renders red (status text
  "API down") while a healthy env (e.g. prod) renders green.
- `node --check docs/assets/chrome.js` and
  `node --check docs/assets/chrome-render.js` pass.
- HTML structural review on at least 2 demo pages confirms the chrome
  shell still mounts and the new banner sits above it.

## Out of scope

- Per-page customisation of the banner text or behaviour.
- Cross-tab cache sharing (e.g. `BroadcastChannel`) — each tab pings
  independently. The 30s in-memory cache is per-tab.
- Detailed diagnostics (response time, status code surfacing). The
  banner is a binary up/down indicator only.
- POL-780 (`installCopyHandlers()`) — separate ticket, branches off
  the new master after this lands.

## Dependencies

- POL-771 (FOUND-1 scaffold) — merged.
- POL-772 (FOUND-2 chrome with `getEnv()`, `getApiOrigin()`,
  `bootstrap()`) — merged.
- Backend `/api/rest/v1/health` endpoint at
  `apps/backend/src/rest/index.ts:49` — exists and returns 200 on
  healthy envs.

## Assumptions

- `/api/rest/v1/health` returns a 2xx with any body shape on healthy
  envs. We only inspect `response.ok` (2xx range).
- CORS allows GET from the GitHub Pages demo origin to every env's
  api host. (The demo site already calls these origins from other
  pages, so the CORS configuration is in place.)
- `AbortController` and `fetch` with `cache: 'no-store'` are available
  in every browser the demo targets (modern evergreen — matches the
  rest of the demo site's baseline).
- `getApiOrigin(env)` always returns a parseable URL.
