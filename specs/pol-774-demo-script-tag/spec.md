# POL-774 — Script-tag (Shadow DOM widget) demo page

**Feature Branch**: `pol-774-demo-script-tag`
**Created**: 2026-05-05
**Status**: Draft
**Input**: User description: "POL-774 DEMO-SCR — Script tag (Shadow DOM widget) demo page"

## Goal

Replace the `docs/script/index.html` placeholder with a working page that
demonstrates the `@polst-web/widget` script-tag pattern with auto-hydration.
End-state: an integrator can copy the script tag and a `<div data-polst="...">`
block straight off the page into their own site and have a working embed.

## Scope

- `docs/script/index.html` — the demo page, replacing the current placeholder.
- `docs/script/script.css` — page-specific layout for the preview / controls /
  snippet grid (mirrors the iframe demo's structure).
- `docs/script/script.js` — page logic: tab selection, control wiring,
  `data-*` attribute composition, live re-hydration on env change, snippet
  rendering, copy button.

No new chrome-layer additions. No build step. Vanilla ESM.

## Behaviour

### Widget bundle

The demo loads the published widget from unpkg:

```html
<script
  src="https://unpkg.com/@polst-web/widget@latest/dist/widget.esm.js"
  type="module" async></script>
```

`@polst-web/widget` is shipped (POL-769) and resolves publicly. No local
mirror, no version pinning at the demo level — `@latest` is the integrator-
facing recommendation we're modelling.

### Origin configuration

Before any `<div data-polst>` is hydrated, the demo calls:

```js
window.Polst.configure({
  origins: {
    apiOrigin: getApiOrigin(getEnv()),
    appOrigin: getAppOrigin(getEnv()),
  },
});
```

This runs from `docs/script/script.js` immediately after `bootstrap()` mounts
the chrome and BEFORE the widget bundle has a chance to auto-hydrate
(verified runtime-overridable at `packages/widget/src/config.ts:48` and
`packages/widget/src/global.ts:355`).

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
     - Left column: the live widget preview (the `<div data-polst>` itself).
     - Right column: control panel + snippet panel stacked.

### Controls

| Tab      | Controls                                                                 |
|----------|--------------------------------------------------------------------------|
| polst    | theme (radio: light/dark/auto), accent (color picker + clear), hideTitle (toggle), hideBrand (toggle) |
| campaign | polst's controls + autoAdvance (toggle, default on)                      |
| brand    | polst's controls + mode (select: polsts/campaigns/mixed)                 |

Defaults match the iframe demo (theme=auto, accent=null, hideTitle=false,
hideBrand=false, autoAdvance=true, mode=polsts).

### Data-attribute composition

Each tab targets a different attribute name to match the widget's
auto-hydration discovery (`packages/widget/src/auto-hydrate.ts`):

- `polst`: `<div data-polst="<shortId>">`
- `campaign`: `<div data-polst-campaign="<id>">`
- `brand`: `<div data-polst-brand="<slug>">`

Customisation flows through `data-*` attributes on the same element:
`data-theme`, `data-accent`, `data-hide-title`, `data-hide-brand`,
`data-auto-advance`, `data-mode`. Only non-default values appear; insertion
order is fixed so the snippet is deterministic.

### Snippet panel

A `<pre><code>` block showing the exact two-line copy-paste block:

```html
<script src="https://unpkg.com/@polst-web/widget@latest/dist/widget.esm.js"
        type="module" async></script>
<div data-polst="<shortId>"
     data-theme="dark"
     data-accent="#ff5722"></div>
```

A "Copy" button next to it uses `navigator.clipboard.writeText` when
available and silently no-ops otherwise. POL-POLISH-COPY (POL-780) will
later replace this with a shared helper.

### Real-time update

A single `update()` recomputes:
- The preview element's `data-*` attributes (and re-hydrates by removing
  + re-inserting the element so `window.Polst` re-runs auto-hydration).
- Snippet text.

It is bound to `change`/`input` on every control and to tab switches.

### Env switcher

When the visitor switches env via the chrome bar, the page reloads with
the new `?env=` param (chrome-driven). On reload, `Polst.configure({ origins })`
runs again with the new env's origins before any hydration. Result: the
preview hits the new env's API.

## Acceptance

- All three widget variants (polst / campaign / brand) render live against
  the active env.
- Every control updates the preview AND the snippet in real time without a
  full page reload (env switcher is the one exception — that always reloads).
- Empty state renders when no polst link is present in the URL.
- Snippet "Copy" copies the exact block currently shown.
- HTML / JS parse cleanly (`node --check` for the JS, manual structural
  review for HTML).

## Out of scope

- Shared "Copy snippet" logic (POL-POLISH-COPY / POL-780 adds it).
- Cross-mode demos (iframe, sdk, ios, android, rn, api).
- Any changes to `@polst-web/widget` itself — if a hydration bug is
  discovered, file it against the widget package, not this demo.
- Any backend changes.

## Dependencies

- POL-769 (widget published to npm) — **shipped 2026-05-04**.
- POL-771 (FOUND-1 static site scaffold) — merged.
- POL-772 (FOUND-2 shared chrome with `bootstrap()`, `getPolstTarget()`,
  `getApiOrigin()`, `getAppOrigin()`, `renderEmptyState()`) — merged.

## Assumptions

- The widget's `window.Polst.configure({ origins })` is idempotent and
  takes effect for any subsequent (or re-run) hydration. Verified at
  `packages/widget/src/config.ts:48`.
- Removing + re-inserting a `<div data-polst>` element triggers the
  widget's `auto-hydrate.ts` MutationObserver path. If it does not, the
  page falls back to a full reload on control changes (acceptable but
  worse UX — track as a follow-up if needed).
- The widget renders inside its own Shadow DOM, so demo-page CSS does
  not leak into it (and vice versa). No CSS isolation work needed on
  the demo side.
