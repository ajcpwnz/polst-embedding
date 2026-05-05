# POL-776 — REST API demo page (raw fetch + request/response viewer)

**Feature Branch**: `pol-776-demo-api`
**Created**: 2026-05-05
**Status**: Draft
**Input**: User description: "POL-776 DEMO-API — REST API demo page (raw fetch + request/response viewer)"

## Goal

Replace the `docs/api/index.html` placeholder with a working page that
shows what an integrator with no SDK does: pure `fetch()` calls against
`/api/rest/v1`, with request/response panels, copy-paste-able snippets,
and a footer link to Swagger UI for the active env.

## Scope

- `docs/api/index.html` — the demo page, replacing the current placeholder.
- `docs/api/api.css` — page-specific layout for the split request/response
  grid, the variant list, and the snippet panel.
- `docs/api/api.js` — page logic: variant list, request execution, response
  rendering, snippet generation, copy button, device-id persistence.

No new chrome-layer additions. No build step. Vanilla ESM. No third-party
JSON pretty-printer — built-in `JSON.stringify(obj, null, 2)` is sufficient.

## Behaviour

### Variants

Each variant is a card with: method label, URL, optional body preview,
"Run" button, copy-paste-able `fetch()` snippet, and a response panel
that fills in after Run.

| # | Method | URL                                                    |
|---|--------|--------------------------------------------------------|
| 1 | GET    | `/api/rest/v1/polsts/<shortId>`                        |
| 2 | GET    | `/api/rest/v1/polsts/<shortId>/results`                |
| 3 | POST   | `/api/rest/v1/polsts/<shortId>/votes`                  |
| 4 | GET    | `/api/rest/v1/brands/<slug>`                           |
| 5 | GET    | `/api/rest/v1/brands/<slug>/feed?limit=20`             |
| 6 | GET    | `/api/rest/v1/campaigns/<id>`                          |

`<shortId>`, `<slug>`, and `<id>` are populated from `getPolstTarget()`
when present. If a variant's required identifier kind doesn't match the
visitor's polst link, the variant card greys out with "Paste a
{kind} link to try this".

### Request panel (left)

For each variant:
- Method badge (GET / POST).
- Full URL (`<apiOrigin><path>` with placeholders substituted).
- Headers section listing the request headers (always visible: `Accept`,
  `X-Device-Id` for vote).
- Body section (POST only): pretty-printed JSON of `{ choice: "A" }`.
- "Run" button.

### Response panel (right)

After Run:
- HTTP status badge (colour-coded: green 2xx / orange 4xx / red 5xx /
  grey network-error).
- Response headers (subset: `Content-Type`, `X-RateLimit-Remaining` if
  present).
- Body: `JSON.stringify(json, null, 2)` in a `<pre>`, OR raw text if the
  response is not JSON.
- Round-trip duration in ms.

### Snippet

Each card includes a `<pre><code>` block with the exact `fetch()` call
the visitor could paste into their own JS:

```js
const res = await fetch(
  "https://api.staging.polst.app/api/rest/v1/polsts/abc123XYZ_",
  { headers: { "Accept": "application/json" } }
);
const data = await res.json();
```

For the vote variant:

```js
const res = await fetch(
  "https://api.staging.polst.app/api/rest/v1/polsts/abc123XYZ_/votes",
  {
    method: "POST",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "X-Device-Id": "<persisted-uuid>",
    },
    body: JSON.stringify({ choice: "A" }),
  }
);
```

The snippet uses the active env's resolved hostname (not a placeholder)
so a copy-paste actually runs against the env the visitor was looking at.

### Device ID

`X-Device-Id` is generated once on first page load via
`crypto.randomUUID()` and persisted to `localStorage` under
`polst-embed-device-id`. The same value is used in both the live request
and the displayed snippet, so the snippet's copy is reproducible against
the visitor's own browser state. No collision with existing storage
keys (the SDK uses its own key in its own demo page).

### Vote variant — POL-768 dependency

POL-768 makes `X-Polst-Idempotency-Key` optional on POST votes. While
POL-768 is in QA but not yet merged to deployed env(s), the vote
variant's snippet does NOT show the idempotency-key header — the demo
models the post-POL-768 surface, which is the actual public contract.

If the active env still requires the header (e.g. canary lags master),
the live Run will 400 with a clear "X-Polst-Idempotency-Key required" body.
The card surfaces that error verbatim and adds a one-line note: "This
endpoint is being made keyless under POL-768 — your env may not have
that fix yet." That one note is the only forward-reference; once POL-768
deploys to all envs, the note can be removed.

### Empty state

If `getPolstTarget()` returns `null`, render `renderEmptyState(#demo)`
instead of the variant grid. Chrome stays mounted.

### Footer

A footer line:

```
Full API reference: <apiOrigin>/api/rest/v1/docs
```

The link is live (clicking opens the active env's Swagger UI). The href
recomputes on env switch (page reload).

## Acceptance

- All 6 variants execute against the active env and show live response
  (or a clear error response for variants whose ID kind doesn't match
  the visitor's polst link).
- Snippets are valid `fetch()` JS — copying one into a browser console
  runs successfully.
- Vote variant works without an `X-Polst-Idempotency-Key` header (after
  POL-768 reaches the active env).
- Swagger UI footer link points to the active env's docs page.
- HTML / JS parse cleanly.

## Out of scope

- Authenticated request flows (Trusted App Integration tokens) — the
  point of this demo is the public anonymous + device-id surface.
- Cross-mode demos (iframe, script, sdk, ios, android, rn).
- Backend changes — POL-768 is its own ticket and ships independently.
- Shared snippet-copy / syntax highlighting (POL-780 / POL-POLISH-COPY).

## Dependencies

- POL-768 (vote idempotency-key optional) — currently in QA. The vote
  variant's headline experience depends on this shipping; until then,
  the variant works only with a friendly error fallback (see Behaviour
  above).
- POL-771 (FOUND-1 scaffold) — merged.
- POL-772 (FOUND-2 chrome with `getApiOrigin()`, `getPolstTarget()`,
  `renderEmptyState()`) — merged.

## Assumptions

- The REST surface at `/api/rest/v1` is stable and matches the OpenAPI
  spec at `apps/backend/openapi/rest-v1.json`.
- Swagger UI is mounted at `<apiOrigin>/api/rest/v1/docs` on every
  deployed env (canary / staging / prod).
- `crypto.randomUUID()` is available in all browsers the demo targets
  (modern evergreen — matches the rest of the demo site's baseline).
- `X-Device-Id` is the canonical anonymous identifier header for the
  REST surface (matches the SDK's behaviour and the schema unique
  constraint at `apps/backend/prisma/schema.prisma:310-311`).
