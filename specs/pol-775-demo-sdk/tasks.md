# POL-775 — Tasks

Ordered, dependency-aware. Each task is a standalone editable unit.

- [x] T1. `docs/sdk/sdk.css` — page-specific layout (tabs, grid, render
       panel, snippet panel, error block) using shared `--chrome-*` vars.
- [x] T2. `docs/sdk/sdk.js` — vanilla ESM module:
       - Imports `PolstClient`, `renderPolst`, `renderCampaign`,
         `renderBrandFeed` from `https://esm.sh/@polst-web/sdk[@/render]`.
       - Imports shared chrome (`getEnv`, `getApiOrigin`, `getPolstTarget`,
         `renderEmptyState`).
       - Defines per-tab snippet constants (the code-block shows the same
         characters that execute).
       - Implements tab switching, default-tab selection from
         `getPolstTarget()`, render-column updates, error rendering, vote
         re-render flow, and Copy-snippet button.
       - Exports `init()` for `index.html`.
- [x] T3. `docs/sdk/index.html` — drop placeholder copy, link `sdk.css`,
       wire `bootstrap()` + `init()` exactly like `docs/script/index.html`.
- [x] T4. Local parse + visual review (`node --check docs/sdk/sdk.js`),
       open the page with a static server, verify the polst / campaign /
       brand tabs render against canary (no real network exec required —
       the page is static).
- [x] T5. Create `.local-testing-passed` marker.
- [x] T6. Commit (`POL-775: ...`).

## Out of scope

- Lint / Jest tests: this repo has no scripts for the docs/ surface.
  Final report says `"lint_status": "skipped"`, `"test_status": "skipped"`.
- Backend / SDK package modifications.
- Cross-mode demo synchronisation.
