# POL-783 — Plan

## Summary

Add a Playwright-based smoke job that exercises the deployed GH Pages
demo site against `staging`. The workflow is the deliverable; the test
file is small (one parameterised test per mode); the rest of the repo
stays zero-build. Co-locate everything Playwright-related under
`tests/smoke/` so the demo site (`/docs/`) keeps its
"vanilla HTML, no `package.json`" property.

## Layout choice — why `tests/smoke/`

The repo's invariant is "static site under `/docs/`, zero build at
the root". Adding a root `package.json` would either pollute the demo
deploy (Pages would still serve `/docs/` so technically harmless but
confusing) or invite drift where dev deps creep into the demo's
runtime expectations. Putting the Playwright project in
`tests/smoke/` keeps:

- the demo zero-build,
- the smoke project self-contained (its own `node_modules`,
  `package.json`, gitignore),
- a clear convention if more tests get added later (`tests/<suite>/`).

The workflow `cd`s into `tests/smoke/` for `npm ci`, browser install,
and the test run. Nothing in `/docs/` changes.

## Files

- `.github/workflows/smoke.yml` — new workflow.
  - Triggers: `schedule: "0 12 * * *"` (daily at 12:00 UTC),
    `push` to `master`, `workflow_dispatch`.
  - One `smoke` job on `ubuntu-latest`.
  - Steps: checkout, setup Node 22, `npm ci` in `tests/smoke`, install
    only Chromium (`npx playwright install --with-deps chromium`),
    `npx playwright test`, upload `playwright-report` on failure.
  - Concurrency: `group: smoke-${{ github.ref }}, cancel-in-progress: true`
    so push-triggered runs supersede in-flight ones.
  - Total runtime budget: under 2 min per acceptance criterion. Daily
    scheduled run is a single job, so concurrency isn't a cost issue.
- `tests/smoke/package.json` — minimal Node 22 project. Pins
  `@playwright/test` (caret-pinned to a known stable major). No other
  deps. `engines.node = ">=22"`. Scripts: `test` → `playwright test`,
  `report` → `playwright show-report`.
- `tests/smoke/playwright.config.ts` — Playwright config:
  - `testDir: '.'`,
  - `timeout: 30_000`,
  - `expect.timeout: 10_000`,
  - `retries: 2` — handles transient cold-start latency without per-
    request guard logic.
  - `workers: 1` — five tests sequential, total runtime well under the
    2-minute budget; serial keeps logs readable on failure.
  - `reporter: [['list'], ['html', { open: 'never' }]]`.
  - Single `chromium` project with default device descriptor, base URL
    set from env or hardcoded fallback.
  - `use: { trace: 'retain-on-failure', screenshot: 'only-on-failure' }`.
- `tests/smoke/smoke.spec.ts` — five tests, one per mode. Shared
  helper attaches console / response listeners and returns a snapshot
  on assertion. Selectors per `spec.md` § "Per-mode 'rendered'
  selector".
- `tests/smoke/.gitignore` — `node_modules/`, `playwright-report/`,
  `test-results/`.
- `tests/smoke/README.md` — one-screen "how to run locally":
  `cd tests/smoke && npm ci && npx playwright install chromium && npm test`.

## Selectors per mode

Recap from spec.md, with file references for traceability:

- iframe → `iframe.iframe-preview__frame`
  (`docs/iframe/iframe.js` line ~252).
- script → `[data-polst]`
  (`docs/script/script.js` line ~485 in the marker injection).
- sdk → `.sdk-render__mount`
  (`docs/sdk/sdk.js` line ~224).
- api → `.api-card`
  (`docs/api/api.js` line ~282).
- rn → `.polst-chrome` (chrome bar always present even on the
  text-only RN page; mounted by `chrome-render.js` `renderChrome()`).

These selectors are the minimum proof that the page loaded its JS,
parsed the polst target, and rendered SOMETHING. They do NOT prove
that the polst data is correct — that's outside the smoke contract.

## Stable shortId

Per ticket policy ("manual selection, no seeding"):

- The workflow defines `POLST_SMOKE_SHORTID: 'STG-DEMO-01'` as the
  default with a `# TODO` comment instructing the maintainer to
  replace it with a real staging shortId before treating green/red as
  signal.
- The placeholder is intentionally invalid (10-char alphanumeric is
  the contract; ours has a hyphen but a 10-char placeholder is fine
  for shape). The smoke run will mostly fail until replaced — that is
  the documented initial state.
- Tests read `process.env.POLST_SMOKE_SHORTID` so a maintainer can
  override at run time via `workflow_dispatch` inputs without editing
  the file. (Inputs added to the dispatch trigger.)

## `?env=staging`

The chrome-layer `getEnv()` already honours `?env=`. No app code
changes — the workflow URLs include `&env=staging` and the demo's
runtime resolves origins to staging.

## Failure detection

The shared helper:

```ts
async function loadAndAssert(page, url, mode, selector) {
  const consoleErrors: string[] = [];
  const serverErrors: string[] = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('response', (resp) => {
    if (resp.status() >= 500) serverErrors.push(`${resp.status()} ${resp.url()}`);
  });
  await page.goto(url, { waitUntil: 'networkidle' });
  await page.locator(selector).first().waitFor({ state: 'visible' });
  expect(consoleErrors, `[${mode}] console errors`).toEqual([]);
  expect(serverErrors, `[${mode}] 5xx responses`).toEqual([]);
}
```

`waitUntil: 'networkidle'` plus the visible selector wait covers the
hydration window for every mode. The widget bundle (script demo) and
the SDK fetch (sdk demo) both complete inside that window in normal
operation; if they don't, the selector wait will time out and the
test fails on render rather than on a swallowed error.

## Quality gates

- YAML structural sanity for `.github/workflows/smoke.yml` — verify
  via `node -e` length+`jobs:` check (no `python3 -c yaml` required).
- Visual review of every `.ts` file (no TS toolchain available
  locally; `node --check` doesn't parse TS).
- Confirm acceptance criteria from the ticket body line by line in
  `tasks.md`.
- `tests/smoke/.gitignore` present and excludes Playwright artifacts.

`lint_status` reports as `skipped` because no lint runner is
appropriate at this scope; the workflow YAML structural check is the
meaningful gate. `test_status` reports as `skipped` because actually
running Playwright locally is out of scope per the worker brief — the
workflow file is the deliverable; CI will exercise it.

## Out of scope

- Slack / email failure notifications.
- Multi-env coverage.
- Replacing the existing `pages-smoke.yml` (it covers a different
  layer; both are kept).
- Deeper per-mode behavioural assertions.

## Open questions

None blocking. The placeholder shortId is a documented initial
condition, not a question — the workflow ships, the maintainer
replaces it before treating green/red as authoritative signal.
