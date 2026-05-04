# POL-773 — Iframe embed demo page

## Goal

Replace the iframe demo placeholder with a working page that lets visitors
exercise the `<iframe>` embedding mode against any of the three Polst embed
routes (polst / campaign / brand), with live customisation controls and a
copyable snippet panel that mirrors the iframe's current configuration.

## Scope

- `docs/iframe/index.html` — the demo page, replacing the current placeholder.
- `docs/iframe/iframe.css` — page-specific layout for the tabs / preview /
  controls / snippet grid.
- `docs/iframe/iframe.js` — page logic: tab selection, control wiring,
  iframe `src` composition, snippet rendering, copy button.

No new chrome-layer additions. No build step. Vanilla ESM.

## Behaviour

### Tab routing
- Tabs: `polst | campaign | brand`.
- Default tab is auto-selected from `getPolstTarget().kind` when present.
- If `getPolstTarget()` returns `null`, render the shared empty state via
  `renderEmptyState(#demo)` instead of the demo. The chrome bar stays
  mounted so the visitor can paste a polst link.

### Layout
- Top: shared chrome (mounted by `bootstrap()` into `#chrome`).
- Below the chrome inside `#demo`:
  1. Tabs row (3 buttons).
  2. Two-column grid:
     - Left column: the live `<iframe>` preview.
     - Right column: control panel + snippet panel stacked.

### Controls

| Tab      | Controls                                                                 |
|----------|--------------------------------------------------------------------------|
| polst    | theme (radio: light/dark/auto), accent (color picker + clear), hideTitle (toggle), hideBrand (toggle) |
| campaign | polst's controls + autoAdvance (toggle, default on)                      |
| brand    | polst's controls + mode (select: polsts/campaigns/mixed)                 |

Per `embedConfig.ts`:
- `theme` default `auto`
- `accent` default `null` (omit param entirely when unset)
- `hideTitle` / `hideBrand` default `false`
- `autoAdvance` default `true` — campaign only
- `mode` default `polsts` — brand only

### URL composition
- `appOrigin = getAppOrigin(getEnv())`
- `polst`: `${appOrigin}/embed/polst/<id>?<query>`
- `campaign`: `${appOrigin}/embed/campaign/<id>?<query>`
- `brand`: `${appOrigin}/embed/brand/<id>?<query>`

Only non-default params appear in the query string. Order of insertion is
fixed (theme, accent, hideTitle, hideBrand, autoAdvance, mode) so the
snippet is deterministic.

### Snippet panel
- `<pre><code>` block showing the exact current `<iframe ...></iframe>` HTML.
- "Copy" button next to it. POL-POLISH-COPY will replace this with a
  shared helper. For now it uses `navigator.clipboard.writeText` when
  available and silently no-ops otherwise.

### Real-time update
A single `update()` recomputes:
- iframe `src`
- snippet text

It is bound to `change`/`input` on every control and to tab switches.

### Tab switching
Switching tabs swaps which control panel is visible and which embed
route is targeted. The visitor's polst-link only resolves to a single
`(kind, id)` pair, so when the link is e.g. a `polst` link and the
visitor switches to `campaign`/`brand`, we still try to render — the
embed route may 404 in that case, which is the visitor's correct
feedback. (We do not silently substitute IDs.)

## Acceptance

- All three embed routes render live against the active env (the iframe
  loads `${appOrigin}/embed/<kind>/<id>?...`).
- Every control updates the iframe and snippet in real time without
  reloading the page.
- Empty state renders when no polst link is present in the URL.
- HTML / JS parse cleanly (`node --check` for the JS, manual structural
  review for HTML).

## Out of scope

- Shared "Copy snippet" logic (POL-POLISH-COPY adds it).
- Cross-mode demos (script, sdk, ios, android, rn, api).
- Any backend changes.
