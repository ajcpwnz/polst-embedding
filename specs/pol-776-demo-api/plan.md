# POL-776 — Plan

## Summary

Replace the placeholder `docs/api/index.html` with a working REST API
demo. The page consumes shared chrome (`bootstrap()`, `getPolstTarget()`,
`getApiOrigin()`, `getEnv()`, `renderEmptyState()`) exactly like POL-774
and POL-775, and renders a vertical list of six variant cards. Each
card has a deterministic snippet panel + Copy button, a "Run" button
that fires a real `fetch()`, a method/URL/headers/body request preview,
and a response panel with status, duration, and pretty-printed body.

## Files

- `docs/api/index.html` — page chrome shell. Mirror the SDK page pattern:
  link `chrome.css`, link new `api.css`, `<div id="chrome">`, demo
  wrapper, `<main id="demo">`. Inline module imports `bootstrap()` and
  the new `init()` from `api.js`. Removes the trailing emoji + h1 lede
  (the variant cards carry the page weight on their own — but keep an
  understated header for parity with siblings).
- `docs/api/api.css` — page-specific layout (`.api-*` classes). Six
  variant cards stacked vertically. Each card has a header (method
  pill, URL, run button), a 2-column body (request preview + snippet on
  the left, response on the right), and inline error / hint slots.
- `docs/api/api.js` — page logic: variants config, request execution,
  response rendering, snippet generation, copy button, device-id
  persistence, footer with Swagger link.

## Variant config (in `api.js`)

```
[
  { key:'polst',   method:'GET',  pathTpl:'/api/rest/v1/polsts/{shortId}',         needs:'polst' },
  { key:'results', method:'GET',  pathTpl:'/api/rest/v1/polsts/{shortId}/results', needs:'polst' },
  { key:'vote',    method:'POST', pathTpl:'/api/rest/v1/polsts/{shortId}/votes',   needs:'polst', body:{choice:'A'} },
  { key:'brand',   method:'GET',  pathTpl:'/api/rest/v1/brands/{slug}',            needs:'brand' },
  { key:'feed',    method:'GET',  pathTpl:'/api/rest/v1/brands/{slug}/feed?limit=20', needs:'brand' },
  { key:'campaign',method:'GET',  pathTpl:'/api/rest/v1/campaigns/{id}',           needs:'campaign' },
]
```

If `needs` doesn't match `getPolstTarget().kind`, the card is greyed
out with "Paste a {needs} link to try this".

## Vote / POL-768

Headers in the displayed snippet: `Accept`, `Content-Type`,
`X-Device-Id`. Idempotency-key header is NOT shown — that matches the
post-POL-768 surface. If the live Run returns 400 with body matching
"X-Polst-Idempotency-Key", surface the response verbatim AND append a
single hint line: "This endpoint is being made keyless under POL-768 —
your env may not have that fix yet."

## Device ID

`crypto.randomUUID()` once on first read, persisted to localStorage at
key `polst-embed-device-id`. Same value used in the live request and in
the displayed snippet (so the displayed `X-Device-Id` is the actual
header that just went over the wire).

## Snippet rule

`buildSnippet(variant, apiOrigin, deviceId, identifier)` returns a
string with the active env's `apiOrigin` literally inlined — never
`<apiOrigin>` and never a JS template-literal expression. Identifier
is also inlined. Copy-paste into a browser console must run.

## Empty state

`bootstrap()` already calls `renderEmptyState(#demo)` when no polst
target is set. The exported `init()` only runs when a target IS
present, mirroring `sdk.js`.

## Footer

A footer line with a real link to `${apiOrigin}/api/rest/v1/docs` for
the active env. Recomputed on env switch via page reload (chrome
already does that on env-select change).

## QA

`node --check docs/api/api.js` (parse). HTML structural review by
re-read. Static server (`python3 -m http.server 8000 --directory docs`)
check that the page mounts; live env Runs aren't reachable from a local
static server because they hit prod / staging / canary remotes which
have CORS configured for those allowlisted demo origins — the demo
intent is to be hosted on the GH Pages site, so the local check is
"page loads, renders cards or empty state, parses cleanly". Mark the
local-testing-passed marker once the page parses + renders.

## Out of scope

- Auth flows.
- Cross-mode demos.
- Backend / POL-768 — separately ticketed.
- Shared snippet-copy helper / syntax highlighting (POL-780).
