# POL-783 — Playwright smoke test against deployed gh-pages URL

**Feature Branch**: `pol-783-polish-smoke`
**Created**: 2026-05-09
**Status**: Draft
**Input**: User description: "POL-783 POLISH-SMOKE — Playwright smoke test against deployed gh-pages URL"

## Goal

Add a Playwright-driven smoke job that loads each non-static demo page on
the deployed GitHub Pages site against the `staging` env, asserts that
the demo actually rendered (mode-specific selector visible), and that
nothing logged a `console.error` or returned a 5xx during the load. Acts
as a passive regression test against `staging.polst.app` /
`staging-api.polst.app` — if staging breaks an embed mode, this fails
red.

## Scope

- `.github/workflows/smoke.yml` — new GHA workflow, `cron` daily plus
  `push` to `master`, plus `workflow_dispatch`.
- `tests/smoke/package.json` — Node project pinning Playwright. Only
  Chromium installed.
- `tests/smoke/playwright.config.ts` — Playwright config, `retries: 2`,
  reporter `list`, single `chromium` project, base URL set to the
  GH Pages deployment.
- `tests/smoke/smoke.spec.ts` — one `test()` per demo mode (iframe,
  script, sdk, api, rn). Each test:
  - navigates to the deployed URL with `?polst=<known-shortId>&env=staging`,
  - records every `console.error` and every response with `status >= 500`,
  - asserts the mode-specific render selector is visible,
  - asserts no console errors and no 5xx responses surfaced.
- `tests/smoke/.gitignore` — ignore `node_modules/`,
  `playwright-report/`, `test-results/`.
- `tests/smoke/README.md` — short "how to run locally" pointer.

### Coexistence with `pages-smoke.yml`

The existing `.github/workflows/pages-smoke.yml` is a fast curl-based
HTTP-200 check that runs on push to `master`. It catches pure 404 / 5xx
breakage at the Pages CDN layer in seconds and is much cheaper than a
browser. The new Playwright workflow is a deeper, slower
render-correctness check that catches problems the curl check cannot
(JS exceptions, missing widget bundle, broken SDK calls). The two are
complementary; both are kept. The curl workflow keeps the `Pages smoke`
name; the new one is named `Smoke (Playwright)` to disambiguate in the
Actions UI.

## Behaviour

### Demo URLs tested

| mode   | URL                                                                                    |
|--------|----------------------------------------------------------------------------------------|
| iframe | `https://ajcpwnz.github.io/polst-embedding/iframe/?polst=<shortId>&env=staging`         |
| script | `https://ajcpwnz.github.io/polst-embedding/script/?polst=<shortId>&env=staging`         |
| sdk    | `https://ajcpwnz.github.io/polst-embedding/sdk/?polst=<shortId>&env=staging`            |
| api    | `https://ajcpwnz.github.io/polst-embedding/api/?polst=<shortId>&env=staging`            |
| rn     | `https://ajcpwnz.github.io/polst-embedding/rn/?polst=<shortId>&env=staging`             |

Skipped (per ticket): `ios/`, `android/` — both are static text pages
with no live render.

### Per-mode "rendered" selector

Selectors derived by reading each page's `*.js` mount code:

| mode   | selector                          | rationale                                                         |
|--------|-----------------------------------|-------------------------------------------------------------------|
| iframe | `iframe.iframe-preview__frame`    | `iframe.js` builds `<iframe class="iframe-preview__frame">`.       |
| script | `[data-polst]`                    | `script.js` injects `<div data-polst="<id>">`. Widget hydrates it. |
| sdk    | `.sdk-render__mount`              | `sdk.js` mounts a render target with this class.                   |
| api    | `.api-card`                       | `api.js` renders 6 variant cards; at least one must be present.    |
| rn     | `.polst-chrome`                   | rn page is text/static; chrome bar is the stable render proof.     |

### `?env=staging` resolution

Confirmed in `docs/assets/chrome.js` `getEnv()` (lines 201–208):
explicit `?env=<env>` query param wins over hostname inference. So
loading the GH Pages URL with `?env=staging` causes
`getApiOrigin('staging')` to return `https://staging-api.polst.app` and
the demo points its requests at staging. No app changes required.

### Stable shortId

The ticket explicitly says: pick one manually, no seeding. We use a
placeholder constant `STG_SHORT_ID = 'STG-DEMO-01'` in the spec file
(invalid by construction — backend shortIds are 10 chars from
`A-Za-z0-9_-` per `apps/backend/src/utils/shortId.ts`). The workflow
reads the env var `POLST_SMOKE_SHORTID` if set, falling back to that
placeholder. The expected initial state on first merge is RED on
real-data assertions for any mode that needs the polst payload to
render — that is acceptable per the chain brief: the workflow exists,
the structure is correct, and the user replaces the placeholder
out-of-band with a real staging shortId before treating green/red as
signal. A `# TODO` in the workflow file makes this explicit.

### Failure model

Each test fails if any of the following hold after `page.goto()` plus a
short settle window (`waitForLoadState('networkidle')` + `waitForSelector`
on the per-mode selector with a generous timeout):

- The mode selector is not visible within the timeout.
- One or more `console.error` events fired during the navigation /
  hydration window.
- One or more responses returned `status >= 500` during the navigation /
  hydration window.

Console warnings, 4xx responses, and CORS preflight failures are NOT
treated as failures. Rationale: 4xx is part of normal app behaviour
(e.g. a missing polst returns 404 from the API and is rendered as an
inline error by the SDK demo — that is correct UX, not a regression).
The smoke test asserts "the deploy is alive end-to-end", not "every
endpoint returns success".

### Retries

Playwright's `retries: 2` retries failed tests up to 2 times before
marking failed. This handles transient cold-start latency from staging
or GH Pages CDN propagation. Per-test `console.error` and 5xx counters
reset each retry.

## Acceptance

- Daily smoke run (`schedule: "0 12 * * *"`) completes in under
  2 minutes.
- A real staging regression that breaks an embed mode shows red in the
  Actions UI.
- Transient 5xx is absorbed by `retries: 2`.
- `pages-smoke.yml` and `smoke.yml` coexist; both names are
  distinguishable in the Actions tab.
- Workflow YAML is structurally valid (parses; has `jobs:`, `steps:`).
- Spec / plan / tasks tracked under `specs/pol-783-polish-smoke/`.

## Out of scope

- Slack / email notifications on failure (ticket flags as "optional
  polish"). Failure visibility is the GHA red dot, surfaced via GitHub
  notifications already.
- Per-env smoke (canary, prod). `staging` only — staging is the
  pre-prod validation surface and the only env this passive regression
  is meant to police.
- Seeding a known polst into staging via DB writes. Per project
  convention, demo data is manual.
- Deeper assertions on per-mode behaviour (vote works, feed paginates).
  Those belong in dedicated e2e suites, not the smoke job.

## Dependencies

- POL-771 / POL-772 (FOUND scaffold + chrome) — merged.
- POL-773 / POL-774 / POL-775 / POL-776 (iframe / script / sdk / api
  demo pages) — merged. Smoke test asserts their public DOM contracts.
- The polst monorepo's pinned Node 22 engine — workflow uses
  `actions/setup-node@v4` with `node-version: '22'` to match.

## Assumptions

- GH Pages serves `https://ajcpwnz.github.io/polst-embedding/` from
  `master:/docs/`.
- `staging.polst.app` and `staging-api.polst.app` accept anonymous
  cross-origin requests from `ajcpwnz.github.io` (already required for
  the demo site to function in any env).
- A human will eventually set `POLST_SMOKE_SHORTID` (or replace the
  placeholder in the workflow) with a real staging short id. Until
  then, full-render assertions for sdk/api modes will fail — that is
  an explicit, documented initial state, not a bug.
