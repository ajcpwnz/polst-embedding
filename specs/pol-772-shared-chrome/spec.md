# POL-772 — FOUND-2 — Shared chrome: env switcher, polst-link parser, paste-prompt empty state

## Summary

Every demo page under `docs/<mode>/` must reuse the same top-bar chrome:
an env switcher, a polst-link input, and a "View source" link. Demo
pages share a single `?polst=<url>&env=<env>` query format so URLs are
universal across modes. When no polst is selected, every demo renders
the same empty-state paste prompt.

## Acceptance criteria

- Visiting `/iframe/?polst=https://staging.polst.app/p/abc123` autoselects
  the `staging` env, populates the input, and parses the target as
  `{ kind: 'polst', id: 'abc123' }`.
- Visiting `/sdk/` (no arguments) shows the empty state with the paste
  prompt.
- Changing the env dropdown updates `?env=` in the location bar and
  reloads the demo against the new env.
- Submitting the polst-link input updates `?polst=` in the location bar
  and reloads.

## Public API (shared chrome)

`docs/assets/chrome.js` (vanilla ESM) exports:

- `getPolstTarget(): { kind: 'polst' | 'brand' | 'campaign', id: string } | null`
- `getEnv(): 'dev' | 'canary' | 'staging' | 'prod'`
- `getApiOrigin(env): string`
- `getAppOrigin(env): string`
- `parsePolstParam(raw: string): { target, inferredEnv } | null` (helper,
  exported for unit tests)

`docs/assets/chrome-render.js` exports:

- `renderChrome(containerEl, { sourcePath })`
- `renderEmptyState(containerEl)`

## URL parser rules

`?polst=` is parsed in the following order:

1. If the value parses as a `URL` and the hostname matches a known env
   host (`localhost`, `canary.polst.app`, `staging.polst.app`,
   `polst.app`), the env is **inferred** from the hostname.
2. The path is matched against:
   - `/p/<shortId>` → `{ kind: 'polst', id: shortId }`
   - `/b/<slug>` → `{ kind: 'brand', id: slug }`
   - `/c/<campaignId>` → `{ kind: 'campaign', id: campaignId }`
3. If the value is a raw 12-char alphanumeric string (no protocol), it
   is treated as `{ kind: 'polst', id }`.
4. Otherwise the polst is null and the empty state is rendered.

`?env=` (when explicitly present) overrides any env inferred from a
`?polst=` URL. If neither `?env=` nor a polst-URL hostname yields an
env, the default is `prod`.

## Env → origin map

| env     | app origin                       | api origin                          |
|---------|----------------------------------|-------------------------------------|
| dev     | http://localhost:3000            | http://localhost:8000               |
| canary  | https://canary.polst.app         | https://canary-api.polst.app        |
| staging | https://staging.polst.app        | https://staging-api.polst.app       |
| prod    | https://polst.app                | https://api.polst.app               |

## Empty state

Copy: "Paste a Polst link above to see this embed mode in action. Don't
have one? Create one at polst.app." The `polst.app` portion is a link to
`https://polst.app`.

## Out of scope

- Per-mode demo rendering (each mode ticket owns its `<main id="demo">`
  contents — POL-773 is the first to consume the chrome).
- Build tooling. This repo stays no-build / no-bundler.
- Backend changes; the chrome only reads URLs and computes origins.
