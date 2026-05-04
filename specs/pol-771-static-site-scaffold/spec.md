# POL-771 — Static site scaffold + GitHub Pages deploy

## Goal
Public landing page at `https://ajcpwnz.github.io/polst-embedding/` with one tile
per embedding mode (iframe, script, sdk, api, ios, android, rn). Each tile links
to a placeholder per-mode page. A smoke workflow confirms the site rebuilt after
push to master.

## User stories

- **As an integrator** visiting the demo URL, I see a tile grid linking to each
  embedding mode so I can pick the path matching my stack.
- **As a maintainer** pushing to master, I get a CI signal confirming the
  GitHub Pages deploy is reachable, so a broken Pages config surfaces quickly.

## Functional requirements

1. `docs/index.html` — vanilla HTML+CSS landing page with seven tiles in this
   order: iframe, script, sdk, api, ios, android, rn. Each tile renders an
   icon (emoji), a title, and a one-line description, and is itself an `<a>`
   linking to the matching subdir (`/iframe/`, `/script/`, etc.).
2. Each per-mode subdir (`docs/iframe/`, `docs/script/`, `docs/sdk/`,
   `docs/api/`, `docs/ios/`, `docs/android/`, `docs/rn/`) contains an
   `index.html` placeholder displaying "Coming soon" plus the future
   DEMO-* ticket reference, so all tiles return 200.
3. `docs/assets/` exists with a `.gitkeep` so FOUND-2 (POL-772) can fill it.
4. `.github/workflows/pages-smoke.yml` — on push to master, sleeps 30s for
   Pages rebuild then `curl -fsSI` the deploy URL; non-2xx fails the run.
5. Root `README.md` references the deploy URL (minimal addition; full rewrite
   is POLISH-README).

## Non-functional

- Zero build step. No package.json. No bundler. No framework.
- System-font stack. Light/dark via `prefers-color-scheme`.
- Static-only — anything that needs JS arrives in POL-772.

## Out of scope

- Per-mode demo content (DEMO-* tickets).
- Env switcher / link parser / shared chrome (POL-772).
- Custom domain.
- Pages source configuration in the GitHub UI (orchestrator handles post-merge).

## Acceptance

- `docs/index.html` renders 7 tiles, each linking to its placeholder page.
- `docs/<mode>/index.html` exists for all 7 modes.
- `.github/workflows/pages-smoke.yml` parses, references the correct URL, and
  fails on non-2xx response.
- `README.md` mentions the demo URL.
