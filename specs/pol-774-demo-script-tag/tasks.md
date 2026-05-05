# Tasks — POL-774

1. Replace `docs/script/index.html` placeholder with the structural
   template (chrome mount + `#demo` mount + module bootstrap) mirroring
   `docs/iframe/index.html`. Set `data-polst-auto-observe` on `<html>`
   so the widget's MutationObserver path is active.
2. Add `docs/script/script.css` styles for tabs, two-column grid,
   preview pane, control panels, snippet panel. Class names prefixed
   `.script-*`.
3. Add `docs/script/script.js`:
   - import `bootstrap`, `getApiOrigin`, `getAppOrigin`, `getEnv`,
     `getPolstTarget` from chrome.
   - tab state machine (polst | campaign | brand) per spec.
   - control panel factories (radio / toggle / accent / select).
   - `buildAttrs(state)` → ordered `data-*` attribute list.
   - `buildSnippet(state, target)` → exact `<script>` + `<div>` block.
   - `update()` re-creates the marker `<div>` and updates snippet text.
   - origin-config bootstrap: wait for `window.Polst`, then
     `Polst.configure({ origins })`, then mount.
4. Run `node --check docs/script/script.js` and visual review the HTML.
5. Spin up `python3 -m http.server 8000 --directory docs` and open the
   page in a browser if available; otherwise mark local-test-passed
   based on parse-clean + structural review.
