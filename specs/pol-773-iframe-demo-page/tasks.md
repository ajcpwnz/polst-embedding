# Tasks — POL-773

1. Replace `docs/iframe/index.html` with the demo page shell:
   - Keep `#chrome` and `#demo` containers.
   - Add a `<link rel="stylesheet" href="iframe.css">`.
   - Load `iframe.js` as `type=module` after the chrome bootstrap call,
     so it runs once chrome / empty-state are mounted.
2. Add `docs/iframe/iframe.css` — tabs, two-column grid, controls,
   snippet block. Use `--chrome-*` variables for colour parity.
3. Add `docs/iframe/iframe.js`:
   - Export `mountIframeDemo()`.
   - Helpers `buildSrc(state, appOrigin, target)` and
     `buildSnippet(src)`.
   - Wire change/input handlers; bind tab buttons; bind copy button.
4. Run `node --check docs/iframe/iframe.js` to validate JS syntax.
5. Targeted commit: `git add docs/ specs/pol-773-iframe-demo-page/`.

## Phase progress events

Emit `specify_complete`, `clarify_complete`, `plan_complete`,
`tasks_complete`, `analyze_complete`, `implement_complete`, then
`committed`. Skip clarify content (auto-resolved — no underspecified
fields per spec). Skip analyze content (the spec is small and internally
consistent; cross-artifact check is trivial).
