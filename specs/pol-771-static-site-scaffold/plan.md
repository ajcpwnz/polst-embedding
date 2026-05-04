# POL-771 — Implementation plan

## Stack

- Vanilla HTML5 + CSS3. No JS in this ticket (POL-772 introduces shared chrome JS).
- System font stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, ...`).
- Light/dark via `@media (prefers-color-scheme: dark)`.
- Emoji icons (zero asset pipeline).

## Structure

```
docs/
  index.html              # tile landing page
  assets/
    .gitkeep              # FOUND-2 will populate
  iframe/index.html       # placeholder per mode
  script/index.html
  sdk/index.html
  api/index.html
  ios/index.html
  android/index.html
  rn/index.html
.github/workflows/
  pages-smoke.yml         # post-deploy reachability check
README.md                 # reference deploy URL
```

## index.html design

- `<header>` with site title + one-line tagline.
- `<main>` containing a CSS-grid `<section>` of seven `<a class="tile">` cards.
- Each tile: emoji icon, `<h2>` title, `<p>` one-liner.
- Tiles in order: iframe, script, sdk, api, ios, android, rn.
- Inline `<style>` block (single file, no external CSS — keeps deploy simple).
- `<footer>` with link to repo.

### Tile content

| # | Path       | Icon | Title              | Description                                          |
|---|------------|------|--------------------|------------------------------------------------------|
| 1 | /iframe/   | 🪟   | iframe             | Drop-in iframe embed for any HTML page.              |
| 2 | /script/   | 📜   | script tag         | One-line `<script>` widget with auto-mount.          |
| 3 | /sdk/      | 🧩   | JS SDK             | Programmatic mount via `@polst-web/widget`.          |
| 4 | /api/      | 🔌   | REST API           | Raw HTTP — `/api/rest/v1/*` endpoints.               |
| 5 | /ios/      | 🍎   | iOS SDK            | Swift package, native UIKit/SwiftUI views.           |
| 6 | /android/  | 🤖   | Android SDK        | Kotlin module, Compose-friendly views.               |
| 7 | /rn/       | ⚛️   | React Native       | RN wrapper bridging the native SDKs.                 |

## Per-mode placeholder

Each `docs/<mode>/index.html` is a minimal page:
- Same header/footer style as the landing page (kept inline; no shared chrome
  yet — that's POL-772).
- Single `<main>` block: "Coming soon — DEMO-`<XXX>` will land this demo."
- Back-link to `../`.

## Pages smoke workflow

`.github/workflows/pages-smoke.yml`:

```yaml
name: Pages smoke
on:
  push:
    branches: [master]
permissions:
  contents: read
jobs:
  smoke:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Wait for Pages rebuild
        run: sleep 30
      - name: Curl deploy URL
        run: curl -fsSI https://ajcpwnz.github.io/polst-embedding/
```

`-f` fails on non-2xx; `-s` silent; `-S` show error; `-I` HEAD only; `-L` not
needed (GitHub Pages serves the URL directly). Single 30s grace is sufficient
per the ticket's prescription.

## README addition

Append a short "Demo site" section pointing at the live URL. Full rewrite is
deferred to POLISH-README.

## Validation

- `python3 -c "import html.parser ..."` parse check on each HTML file (skipped
  — vanilla HTML, parse-only via browser at deploy time).
- `actionlint` on the workflow if available; otherwise rely on push-triggered
  validation.
- No app is "affected" per `tricycle.config.yml` — lint is skipped.

## Risks / mitigations

- **Pages not yet enabled** — smoke run will fail until orchestrator turns on
  Pages post-merge. Acceptable; the workflow's first green run is the proof.
- **30s grace too short** — easy follow-up to bump if it flakes. Out of scope
  to over-engineer here.
