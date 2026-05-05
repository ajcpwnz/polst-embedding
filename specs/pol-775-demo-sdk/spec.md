# POL-775 — JS SDK (PolstClient + render) demo page

**Feature Branch**: `pol-775-demo-sdk`
**Created**: 2026-05-05
**Status**: Draft
**Input**: User description: "POL-775 DEMO-SDK — JS SDK (PolstClient + render) demo page"

## Goal

Replace the `docs/sdk/index.html` placeholder with a working page that
demonstrates `@polst-web/sdk` programmatic usage. Left panel shows the
exact code an integrator would write; right panel shows the live rendered
output of running it. Variants for polst, campaign, brand feed, and vote.

## Scope

- `docs/sdk/index.html` — the demo page, replacing the current placeholder.
- `docs/sdk/sdk.css` — page-specific layout for the split (code | render)
  layout and per-variant tabs.
- `docs/sdk/sdk.js` — page logic: tab selection, code-block rendering,
  PolstClient instantiation, variant execution, error display.

No new chrome-layer additions. No build step. Vanilla ESM.

## Behaviour

### SDK consumption

The page imports the SDK directly from a public CDN:

```js
import { PolstClient } from "https://esm.sh/@polst-web/sdk";
import { renderPolst, renderCampaign, renderBrandFeed }
  from "https://esm.sh/@polst-web/sdk/render";
```

`@polst-web/sdk@0.7.0+` is published (POL-770) so no local bundle, no
build step. The "until POL-770 ships, bundle locally" workaround is
explicitly NOT used — modelling the integrator-facing pattern is the
point of this demo.

### Client construction

```js
const client = new PolstClient({ baseUrl: getApiOrigin(getEnv()) });
```

`baseUrl` is read from FOUND-2 chrome (`packages/sdk/src/PolstClient.ts:88,229-234`).
The client is recreated whenever the env changes (page reload via chrome
switcher).

### Tab routing

- Tabs: `polst | campaign | brand | vote`.
- Default tab auto-selected from `getPolstTarget().kind` (vote tab is only
  reachable manually since `kind` never resolves to "vote").
- If `getPolstTarget()` returns `null`, render `renderEmptyState(#demo)`.

### Layout

- Top: shared chrome.
- Below the chrome inside `#demo`:
  1. Tabs row (4 buttons).
  2. Two-column grid:
     - Left column: code block (`<pre><code>`) showing the exact SDK call.
     - Right column: live render area (an empty `<div>` mounted by the
       per-variant render call).

### Variants

| Tab      | Code shown (verbatim, what runs)                                                  | Render call                                                                                     |
|----------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| polst    | `const polst = await client.getPolst(shortId);`<br>`renderPolst(mountEl, { polstId: polst.shortId });` | Mounts the public widget for the polst.                                                         |
| campaign | `const campaign = await client.getCampaign(campaignId);`<br>`renderCampaign(mountEl, { campaignId: campaign.id });` | Mounts the public widget for the campaign.                                                      |
| brand    | `const feed = await client.getBrandFeed(slug);`<br>`renderBrandFeed(mountEl, { brandSlug: slug, pageSize: feed.items.length });` | Mounts the brand-feed widget.                                                                   |
| vote     | `const result = await client.polsts.vote(shortId, "A");`                          | Two buttons (Vote A / Vote B). Each click runs the displayed call with the appropriate option, displays `{ optionACount, optionBCount }`, then re-fetches polst data and re-renders the widget so the visitor sees the updated state. |

**Render API note** — `renderPolst`, `renderCampaign`, and `renderBrandFeed`
are widget mounters; they take an options object (not the fetched data).
Calling `client.getPolst(...)` first proves the SDK's read surface works
against the active env; the result's id is then passed to the renderer.
The displayed code block must show BOTH the fetch call AND the render
call — the integrator pattern is "fetch then mount".

**Vote method** — the public surface for voting is
`client.polsts.vote(shortId, choice)` (namespaced). The legacy
ticket body referenced `client.vote(...)`; that method does NOT
exist in `@polst-web/sdk@0.7.x`. The displayed code is the canonical
namespaced form so the snippet is byte-identical to what runs.

The code block is the EXACT string shown to the user — what's executed
right of the divider is identical, character-for-character, to what's
displayed on the left. (No commented-out scaffolding, no extra wrappers
not shown.)

### Vote handling

`client.vote(shortId, choice)` auto-handles `X-Device-Id` internally
(`packages/sdk/src/storage.ts`). After POL-768 ships, the SDK's
idempotency-key handling is also internal — until then the SDK already
generates one per call, so the demo "just works" either way. No demo-side
header generation.

After a vote, the polst tab re-fetches and re-renders so the visitor sees
the updated count.

### Error states

Each variant catches errors and renders them inline in the right panel:

- 404 (invalid shortId / campaignId / slug) — "Not found" message with the
  attempted ID.
- Network drop / 5xx — "Request failed" message with the underlying error
  text.
- 4xx other — "Bad request" message.

The shared chrome does not swallow errors — the SDK demo owns its own
error UI so the visitor sees what an integrator would see.

### Env switcher

Page-reload-based (handled by chrome). After reload the new `client` is
constructed from the new env's `apiOrigin`.

## Acceptance

- Live polst / campaign / brand-feed render via SDK calls against the
  active env.
- Vote button works against the active env; after voting, the polst
  tab's render shows the updated state.
- Error states render gracefully for invalid IDs and simulated network
  failure.
- The code-block snippet is identical to the code that actually runs
  (verbatim — no scaffolding hidden from the visitor).
- HTML / JS parse cleanly.

## Out of scope

- Modifications to `@polst-web/sdk` or `@polst-web/sdk/render` — if a
  rendering bug is found, file against the SDK package.
- Cross-mode demos (iframe, script, ios, android, rn, api).
- Authenticated / Trusted App Integration token flows — anonymous +
  device-id only (matches the public integrator surface).
- Any backend changes.

## Dependencies

- POL-770 (`@polst-web/sdk` published to npm) — **shipped 2026-05-04**.
- POL-768 (vote idempotency-key optional) — currently in QA. Until merged,
  the SDK still sends a generated key internally so the vote variant
  works regardless. After merge, no behavioural change on the demo side
  (the SDK abstracts the header).
- POL-771 (FOUND-1 scaffold) — merged.
- POL-772 (FOUND-2 chrome) — merged.

## Assumptions

- `esm.sh` resolves both `@polst-web/sdk` and `@polst-web/sdk/render`
  subpath imports without a build step. (If subpath resolution fails on
  esm.sh, fall back to unpkg's bare-specifier-friendly URL.)
- The SDK's `render*` helpers mount into a passed-in DOM element and own
  their own DOM updates. They do NOT manage visitor-driven controls (those
  are not part of this demo — the SDK demo is about the API shape, not
  customisation; that's the script-tag demo's job).
- Device-id persistence is handled by the SDK via `localStorage`
  (`packages/sdk/src/storage.ts`). No demo-side persistence work.
