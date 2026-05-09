import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright config for the polst-embedding GH Pages smoke suite.
 *
 * - Single chromium project; demo site is browser-runtime only and the
 *   smoke is about deploy health, not cross-browser parity.
 * - retries: 2 — handles transient cold-start latency from staging or
 *   GH Pages CDN propagation. The test asserts no console.error and no
 *   5xx, both of which are fresh per attempt.
 * - workers: 1 — five tests, sequential keeps logs readable on failure
 *   and total runtime well under the 2 min acceptance budget.
 * - reporter: list (CLI) + html (artifact uploaded on failure).
 * - trace + screenshot on failure for diagnosability in the GHA
 *   artifact.
 */
export default defineConfig({
  testDir: '.',
  testMatch: /.*\.spec\.ts/,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 2,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL:
      process.env.POLST_SMOKE_BASE_URL ??
      'https://ajcpwnz.github.io/polst-embedding',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
