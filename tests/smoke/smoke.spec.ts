import { test, expect, type Page } from '@playwright/test';

/**
 * GH Pages smoke suite.
 *
 * For each non-static demo mode (iframe, script, sdk, api, rn):
 *   - load the deployed page with `?polst=<shortId>&env=staging`,
 *   - record any `console.error` and any response with status >= 500,
 *   - assert the mode-specific render selector becomes visible,
 *   - assert no console.error and no 5xx surfaced during the load.
 *
 * Skipped per ticket: `ios/`, `android/` — text-only pages with no
 * live render.
 *
 * Console warnings, 4xx responses, and CORS preflight failures are
 * NOT treated as failures. Rationale: 4xx is part of normal app
 * behaviour (e.g. a 404 on a missing polst is correct UX). The smoke
 * test is a "deploy is alive end-to-end" probe, not a behavioural
 * suite.
 */

// TODO: replace POLST_SMOKE_SHORTID in `.github/workflows/smoke.yml`
// (or pass via workflow_dispatch input) with a real, stable polst
// shortId on staging. The placeholder below renders an inline error
// in most modes — that is the documented initial state until a
// maintainer wires a real id. The suite still proves "the page
// loaded its JS and rendered SOMETHING" via the per-mode selector.
const SHORT_ID = process.env.POLST_SMOKE_SHORTID ?? 'STG-DEMO-01';

type Mode = {
  name: string;
  path: string;
  selector: string;
};

const MODES: Mode[] = [
  // iframe.js builds <iframe class="iframe-preview__frame">.
  { name: 'iframe', path: '/iframe/', selector: 'iframe.iframe-preview__frame' },
  // script.js injects <div data-polst="..."> for the auto-observer
  // to hydrate via the unpkg widget bundle.
  { name: 'script', path: '/script/', selector: '[data-polst]' },
  // sdk.js mounts a render slot with class sdk-render__mount.
  { name: 'sdk', path: '/sdk/', selector: '.sdk-render__mount' },
  // api.js renders 6 variant cards; at least one must be visible.
  { name: 'api', path: '/api/', selector: '.api-card' },
  // rn page is text/static; the chrome bar is the stable render
  // proof that the deploy is responding and the shared chrome layer
  // booted.
  { name: 'rn', path: '/rn/', selector: '.polst-chrome' },
];

async function loadAndAssert(
  page: Page,
  url: string,
  mode: string,
  selector: string,
): Promise<void> {
  const consoleErrors: string[] = [];
  const serverErrors: string[] = [];

  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });
  page.on('response', (resp) => {
    if (resp.status() >= 500) {
      serverErrors.push(`${resp.status()} ${resp.url()}`);
    }
  });

  await page.goto(url, { waitUntil: 'networkidle' });

  // Per-mode selector visibility is the proof the page rendered.
  // Generous timeout: widget bundle (script demo) loads from unpkg,
  // SDK demo waits on a staging-api fetch.
  await expect(page.locator(selector).first()).toBeVisible({
    timeout: 15_000,
  });

  expect(consoleErrors, `[${mode}] console errors: ${consoleErrors.join(' | ')}`).toEqual([]);
  expect(serverErrors, `[${mode}] 5xx responses: ${serverErrors.join(' | ')}`).toEqual([]);
}

for (const mode of MODES) {
  test(`smoke: ${mode.name} renders against staging`, async ({ page }) => {
    const url = `${mode.path}?polst=${encodeURIComponent(SHORT_ID)}&env=staging`;
    await loadAndAssert(page, url, mode.name, mode.selector);
  });
}
