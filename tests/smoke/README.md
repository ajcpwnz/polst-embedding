# polst-embedding smoke tests

Playwright-driven smoke check that loads each non-static demo page on
the deployed GitHub Pages site against the `staging` Polst env and
asserts the page rendered with no `console.error` and no `5xx`.

CI: `.github/workflows/smoke.yml` runs daily plus on every push to
`master`.

## Run locally

```bash
cd tests/smoke
npm ci
npx playwright install chromium
npm test
```

To override the shortId (the placeholder in CI is intentionally
invalid — see TODO in `smoke.spec.ts` and `smoke.yml`):

```bash
POLST_SMOKE_SHORTID=AbCdEfGhIj npm test
```

To point the suite at a different deploy URL:

```bash
POLST_SMOKE_BASE_URL=http://localhost:8000 npm test
```

## What is asserted

Per mode (iframe, script, sdk, api, rn):

- the mode-specific render selector becomes visible within 15s,
- no `console.error` events fired during the load,
- no responses returned `status >= 500`.

Console warnings, 4xx responses, and CORS preflight failures are NOT
failures — they're normal app behaviour. See `specs/pol-783-polish-
smoke/spec.md` for the full failure model.

## Skipped modes

`ios/` and `android/` are static text pages with no live render and
are intentionally not in the smoke matrix.
