# POL-772 — Plan

## Files

### New

- `docs/assets/chrome.js` — pure logic. `getPolstTarget`, `getEnv`,
  `getApiOrigin`, `getAppOrigin`, `parsePolstParam`. No DOM access.
  Vanilla ESM, no deps.
- `docs/assets/chrome-render.js` — DOM. `renderChrome(container,
  { sourcePath })` mounts the top-bar; `renderEmptyState(container)`
  renders the paste-prompt placeholder. Imports from `./chrome.js`.
- `docs/assets/chrome.css` — top-bar styling (env dropdown, link
  input, "View source" link), dark-mode aware to match the index.

### Modified

- `docs/iframe/index.html`, `docs/script/index.html`,
  `docs/sdk/index.html`, `docs/api/index.html`, `docs/ios/index.html`,
  `docs/android/index.html`, `docs/rn/index.html`:
  - Add `<link rel="stylesheet" href="../assets/chrome.css">`.
  - Add `<script type="module" src="../assets/chrome.js"></script>` (so
    the module is parsed, but the actual mount happens via
    chrome-render).
  - Replace the "Coming soon" card with a top-level `<div id="chrome">`
    and a `<main id="demo">`. A small inline module per page calls
    `renderChrome(document.getElementById('chrome'), { sourcePath: ... })`
    and, when `getPolstTarget()` is null, `renderEmptyState(demo)`.

## Acceptance verification (no test runner)

- `node --check docs/assets/chrome.js` — syntax check.
- `node --check docs/assets/chrome-render.js` — syntax check.
- Manual read-through of acceptance examples against `parsePolstParam`
  to confirm:
  - `https://polst.app/p/abc123abc12` → `{ kind: 'polst', id, env: 'prod' }`.
  - `https://staging.polst.app/p/abc123abc12` → env inferred `staging`.
  - `https://polst.app/b/some-brand` → `{ kind: 'brand', id: 'some-brand' }`.
  - `https://polst.app/c/c-12345` → `{ kind: 'campaign' }`.
  - `abc123abc123` (12 alphanumeric) → `{ kind: 'polst' }`.
  - random string → null.

## Out of scope

- Per-mode demo wiring (POL-773 onward).
- Tests beyond `node --check`. polst-embedding has no JS test runner.
