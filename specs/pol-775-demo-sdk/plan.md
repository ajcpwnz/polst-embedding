# POL-775 — Implementation Plan

## Files to create

- `docs/sdk/sdk.css` — page-specific layout (split grid + tabs + snippet
  panel + render mount). Mirrors `docs/script/script.css` class structure
  (`.sdk-tabs`, `.sdk-grid`, `.sdk-render`, `.sdk-snippet`, etc.) using
  shared `--chrome-*` CSS variables for parity with the script demo.
- `docs/sdk/sdk.js` — vanilla ESM module exporting `init()`. Imports
  `PolstClient` from `https://esm.sh/@polst-web/sdk` and the render
  helpers from `https://esm.sh/@polst-web/sdk/render` (subpath). Imports
  shared chrome (`getEnv`, `getApiOrigin`, `getPolstTarget`,
  `renderEmptyState`).

## File to update

- `docs/sdk/index.html` — drop placeholder copy, link `sdk.css`, mount a
  `<main id="demo">` (already present), call `bootstrap()` and `init()`
  exactly as `docs/script/index.html` does.

## Page layout (mirrors POL-774's structural shell)

```
+---------------------------------------------------+
| chrome (env switcher + ?polst input + view source) |
+---------------------------------------------------+
| icon + h1 + lede                                  |
| [tabs row: Polst / Campaign / Brand / Vote ]      |
| +---------------------+-------------------------+ |
| | Code (left)         | Render (right)          | |
| | <pre><code> exact   | live SDK output         | |
| |   call              |  - Polst tab: widget    | |
| |  +---------------+  |  - Campaign tab: widget | |
| |  Copy button     |  |  - Brand tab: widget    | |
| |                  |  |  - Vote tab: 2 buttons  | |
| |                  |  |    + result panel       | |
| +---------------------+-------------------------+ |
+---------------------------------------------------+
```

Tabs row is visually identical to POL-774 (`.sdk-tab` re-uses the same
`--chrome-accent` highlight). The grid is two-column on desktop and
collapses to one column under 880 px.

## Variant logic

Per active tab the page does the SAME steps:

1. Set `code.textContent` to the **literal** snippet for the tab
   (a constant string in `sdk.js`). The strings are emitted into a
   `Function` constructor at click-time / mount-time so the executed
   code is the displayed code.
2. Run the snippet:
   - polst: `getPolst(shortId)` then `renderPolst(mountEl, {polstId})`.
   - campaign: `getCampaign(campaignId)` then `renderCampaign(mountEl, {campaignId})`.
   - brand: `getBrandFeed(slug)` then `renderBrandFeed(mountEl, {brandSlug, pageSize})`.
   - vote: render two buttons; on click, run
     `client.polsts.vote(shortId, "A" | "B")`, render the result
     payload, then re-fetch + re-render the polst widget so the
     visitor sees the updated state.
3. On error: catch `PolstApiError` (or any throw), classify by status
   (404 / 4xx / network / 5xx), render an inline error block in the
   render column.

## Default tab

`getPolstTarget().kind ∈ {"polst", "campaign", "brand"}` selects the
matching tab. Vote tab is only reachable via manual click and is
disabled when `target.kind !== "polst"` (you cannot vote without a
polst short id).

## SDK loading

```js
import { PolstClient } from "https://esm.sh/@polst-web/sdk";
import { renderPolst, renderCampaign, renderBrandFeed }
  from "https://esm.sh/@polst-web/sdk/render";
```

If esm.sh subpath rewriting trips on the bare `@polst-web/sdk/render`
specifier in any browser engine we hit during QA, fall back to the
fully-qualified pinned URL `https://esm.sh/@polst-web/sdk@0.7.0/render`.
Loaded once at module top-level — no lazy loading, no per-tab dynamic
imports (would defeat the "displayed code = executed code" property).

## Client construction

```js
const client = new PolstClient({ baseUrl: getApiOrigin(getEnv()) });
```

Constructed once on `init()`. Env switching reloads the page (chrome
contract), so the client is naturally re-built with the new origin.

## Snippet rendering

The snippet panel is a `<pre><code>` plus a Copy button in the same
visual style as `docs/script/script.css`. The textContent is a static
string per tab — NOT dynamically built — so a visual diff against the
top of `sdk.js` shows the same characters.

## Constraints honoured

- "Displayed code = executed code" — the per-variant call lives both as
  a literal const string and as the actual call site; the demo runs the
  same string verbatim so the two cannot drift.
- No new chrome-layer additions.
- No build step. Single `<script type="module">` entry from
  `index.html`.
- Vote handling does not pass `X-Device-Id` or any idempotency key —
  the SDK manages those internally (`storage.ts`).

## Out of scope (forward-looking)

- Inline live-edit (changing the shortId in the code block to fetch a
  different polst): not covered by this ticket.
- Cross-tab persistence of last-rendered widget: not covered.
- Light/dark theming controls: that's the script-tag demo's job.
