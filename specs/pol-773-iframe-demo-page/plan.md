# Plan — POL-773

## Files

- `docs/iframe/index.html` — replace placeholder. Mount chrome + demo,
  load `iframe.js` as a module.
- `docs/iframe/iframe.css` — grid + tabs + control panel + snippet
  panel styling. Reuses `--chrome-*` CSS variables from `chrome.css`.
- `docs/iframe/iframe.js` — single ESM module:
  - `mountIframeDemo(rootEl, target)` — renders tabs + preview +
    controls + snippet, wires up update flow.
  - Helpers: `buildSrc()`, `buildSnippet()`, `paramsForTab()`.

## Approach

1. `index.html` keeps the existing `#chrome` + `#demo` container
   convention. `bootstrap()` still mounts chrome and renders the empty
   state when no polst link is present.
2. `iframe.js` runs after `bootstrap()` returns. It checks
   `getPolstTarget()`; on `null`, do nothing (empty state already
   rendered). Otherwise replace `#demo`'s contents with the demo UI.
3. The demo UI renders three tabs and a single visible control-panel
   per tab. The default tab is the target's `kind`. A single state
   object holds `{ tab, theme, accent, hideTitle, hideBrand,
   autoAdvance, mode }`. `update()` rebuilds the iframe `src` + snippet
   from state.
4. Param composition omits any param that equals its default.
5. The snippet is a `<pre><code>` block; the copy button uses
   `navigator.clipboard.writeText` when available.

## Validation

- `node --check docs/iframe/iframe.js`
- Visual inspection of HTML structure (no build to run).
- Confirm `iframe.js` does not import from any forbidden surfaces
  (only `../assets/chrome.js` for `getEnv`/`getAppOrigin`/
  `getPolstTarget`).
