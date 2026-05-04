# POL-772 — Tasks

T001. Create `docs/assets/chrome.js` with pure-logic public API
      (`getPolstTarget`, `getEnv`, `getApiOrigin`, `getAppOrigin`,
      `parsePolstParam`). Implement env→origin map. Implement the URL
      parser per spec. ESM, no deps.

T002. Create `docs/assets/chrome.css` with top-bar styling. Match the
      light/dark token scheme from the existing `docs/index.html`.

T003. Create `docs/assets/chrome-render.js` exporting `renderChrome`
      and `renderEmptyState`. `renderChrome` mounts:
      - env `<select>` (dev/canary/staging/prod) wired to
        `?env=` (change → reload).
      - `<form>` with text `<input>` for `?polst=` (submit → reload).
      - `<a>` "View source" pointing at the GitHub source path.
      `renderEmptyState` renders the paste-prompt copy with the
      `polst.app` link.

T004. Update each per-mode `docs/<mode>/index.html`
      (`iframe`, `script`, `sdk`, `api`, `ios`, `android`, `rn`)
      to:
      - Include the chrome `<link>` and `<script type="module">`
        bootstrapper.
      - Replace the "Coming soon" card with `<div id="chrome">` and
        `<main id="demo">`.
      - Inline a small module that, on `DOMContentLoaded`, calls
        `renderChrome` and, when no polst is selected, calls
        `renderEmptyState`.

T005. Run `node --check` on each new `.js` file.

T006. Spot-check: walk through each acceptance example and confirm
      `parsePolstParam` would return the expected value (this is a
      manual reasoning step — there is no JS test runner in this repo).

## Dependencies

- T001 → T003 (render imports parser).
- T002 → T004 (page references CSS).
- T003 → T004 (page references render module).
- T005 after T001 + T003.
