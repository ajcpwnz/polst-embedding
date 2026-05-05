# Plan — POL-774 Script-tag widget demo

Mirrors `specs/pol-773-iframe-demo-page/` (the iframe demo) since the page
shape is identical: shared chrome + tabs + two-column grid (preview /
controls + snippet).

## Files

1. `docs/script/index.html` — replace placeholder. Loads the unpkg widget
   bundle as `type="module" async`, mounts shared chrome, sets
   `<html data-polst-auto-observe>` so dynamic re-insertion of the
   `<div data-polst>` re-hydrates via the widget's MutationObserver,
   imports `./script.js` and runs `init()`.
2. `docs/script/script.css` — page-specific styles. Copy of
   `iframe.css` retargeted onto `.script-*` class names so the two
   demos can diverge later without touching shared chrome CSS.
3. `docs/script/script.js` — entry. Reads target via
   `getPolstTarget()`, configures origins via
   `window.Polst.configure({ origins })` BEFORE the marker `<div>` is
   inserted, then mounts the demo: tabs, control panels, live preview,
   snippet panel.

## Re-hydration model

The widget bundle scans for marker elements (`[data-polst]`,
`[data-polst-campaign]`, `[data-polst-brand]`) on DOMContentLoaded and
again when the host carries `data-polst-auto-observe`. To make every
control change show up in the live preview without a full reload:

1. The `<div data-polst*>` lives inside a stable wrapper.
2. On any control change `update()` removes the existing marker
   element, reads the new state, and inserts a fresh marker `<div>` in
   the same wrapper. The widget's MutationObserver picks up the
   insertion and re-hydrates inside its own Shadow DOM.
3. `data-polst-hydrated="1"` is never on the new element so the scan
   matches.

## Origin configuration timing

`window.Polst` is created synchronously when `widget.esm.js` finishes
parsing. Because the bundle is loaded with `type="module" async`, the
order between it and our page module is non-deterministic. We handle
both cases:

- If `window.Polst` is already present when `init()` runs, configure
  immediately.
- Otherwise, poll a `setTimeout` micro-loop (≤200ms total) until
  `window.Polst` lands and configure then. We only insert the marker
  `<div>` AFTER `configure()` returns so the first hydration sees the
  intended origins. Empirically the bundle parses < 200ms even on slow
  links; this is a UX-affordance, not a correctness fence.

## Out of scope

- Shared snippet-copy helper (POL-780).
- Cross-mode embed demos.
- Widget package edits.
