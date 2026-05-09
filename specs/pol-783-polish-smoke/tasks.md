# POL-783 — Tasks

1. Create `tests/smoke/` directory with:
   - `package.json` — Node 22, single dep `@playwright/test`. Scripts
     `test`, `report`. `engines.node` pinned.
   - `playwright.config.ts` — `retries: 2`, `workers: 1`, single
     chromium project, list + html reporters, trace + screenshot on
     failure.
   - `smoke.spec.ts` — five tests (iframe, script, sdk, api, rn) using
     a shared `loadAndAssert(page, url, mode, selector)` helper.
     Selectors per `plan.md`. URLs include `?polst=<shortId>&env=staging`
     where the shortId reads from `process.env.POLST_SMOKE_SHORTID`
     with a placeholder fallback.
   - `.gitignore` — `node_modules/`, `playwright-report/`,
     `test-results/`.
   - `README.md` — three-line "how to run locally" pointer.
2. Create `.github/workflows/smoke.yml`:
   - Triggers: `push` to `master`, `workflow_dispatch` with optional
     `shortId` input. (No `schedule` — owner declined daily cron.)
   - Single `smoke` job on `ubuntu-latest`.
   - Steps: checkout → `actions/setup-node@v4` (node 22) →
     `npm ci` in `tests/smoke` → `npx playwright install --with-deps
     chromium` → `npx playwright test` → upload `tests/smoke/playwright-
     report` artifact on failure (`if: failure()`).
   - `concurrency` group keyed on ref with cancel-in-progress.
   - `env.POLST_SMOKE_SHORTID` defaults to a placeholder, with
     `# TODO` comment instructing maintainer to replace before
     treating signal as authoritative. `workflow_dispatch` input
     overrides at runtime.
3. Quality gates:
   - YAML structural sanity check on `.github/workflows/smoke.yml`
     (parses, has `jobs:`, `steps:`, key triggers).
   - Visual review of all three TS files (balanced braces, no obvious
     syntax issues).
   - Acceptance-criteria line-by-line cross-check against ticket body.
4. Write `specs/pol-783-polish-smoke/.local-testing-passed` marker
   after gates pass.
5. `git add -A && git commit -m "POL-783: gh-pages playwright smoke
   workflow"`. Capture SHA. Emit `committed` progress event.
