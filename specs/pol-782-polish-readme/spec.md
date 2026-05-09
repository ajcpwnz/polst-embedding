# POL-782 — Repo README rewritten as integrator-facing entry point

**Feature Branch**: `pol-782-polish-readme`
**Created**: 2026-05-09
**Status**: Draft
**Input**: User description: "POL-782 POLISH-README — Repo README rewritten as integrator-facing entry point"

## Goal

Replace the current sandbox-only `README.md` (currently 18 lines, framed
as "sandbox apps for embedding the Polst SDKs") with an integrator-facing
entry point. The audience is a developer who landed on
`github.com/ajcpwnz/polst-embedding` and wants to (a) see the live demo
in one click and (b) find the 30-second starter for their stack.

## Scope

- Rewrite top-level `README.md` from scratch.
- No changes to any demo page, asset, or sandbox file.
- No changes to `ios/README.md` or `android/README.md` (the new top-level
  README links out to them — the per-platform docs remain authoritative).
- No new `LICENSE` file in this ticket — license call-out documents the
  current state ("no formal license file yet, MIT/Apache-2.0 forthcoming;
  demo HTML/JS is OK to copy").

## Behaviour

### Above-the-fold

Live demo URL is the FIRST thing visible after the title. A visitor who
reads only the first 10 lines should know: (1) what this repo is, (2)
the live URL, (3) that it covers 7 integration modes.

### "Pick your integration mode" section

A table or clearly delimited subsections covering all 7 modes:

| Mode | Live demo | Source | 30-second starter |
|------|-----------|--------|-------------------|
| iframe | `/iframe/` | `docs/iframe/index.html` | one-line `<iframe>` |
| script tag | `/script/` | `docs/script/index.html` | `<script>` + `<div data-polst>` |
| JS SDK | `/sdk/` | `docs/sdk/index.html` | `import { PolstClient }` from esm.sh |
| REST API | `/api/` | `docs/api/index.html` | `fetch()` against `/api/rest/v1/...` |
| iOS | `/ios/` | `docs/ios/index.html` | SwiftPM dependency line |
| Android | `/android/` | `docs/android/index.html` | Gradle dependency line |
| React Native | `/rn/` | `docs/rn/index.html` | RN bridge note |

Each starter is copy-paste-able — a single `<iframe>` tag, a single
`<script>` line, a single `import` statement, etc. Not a tutorial; a
ribbon-cutter.

The choice between table layout and subsections is a layout call made
during implementation — whichever stays under the 200-line budget while
staying scannable wins. Subsections likely fit better because each starter
needs its own fenced code block.

### Native SDKs section

A short paragraph delegating iOS/Android run instructions to the per-
platform READMEs:

- `ios/README.md` — SwiftPM, Xcode scheme, simulator run.
- `android/README.md` — Gradle, Android Studio, debug APK.

The top-level README does NOT duplicate the platform-specific commands.

### "How this site works" section

Two-line paragraph explaining the two query params:

- `?polst=<url-or-shortId>` — picks which polst the demo renders against.
  Accepts any polst share URL or a raw 10-char shortId.
- `?env=<canary|staging|prod>` — picks the backend env. Defaults to env
  inferred from `?polst=` URL host, falling back to staging.

### License / legal call-out

A short section clarifying integrator copy rights:

- No formal `LICENSE` file at the repo root yet (future MIT or Apache-2.0).
- The demo HTML, CSS, and JS in `docs/` are explicitly intended to be
  copy-pasted into integrator code — that's their job.
- The native sandbox apps under `ios/` and `android/` are sample apps;
  see their per-platform LICENSE notes (`android/LICENSE` already exists;
  iOS sandbox follows the upstream `polst-ios` SDK's license).

### Footer

A short footer links back to the live site, the issue tracker (Linear
isn't public — link to GitHub Issues), and the per-platform READMEs.

## Acceptance

- `wc -l README.md` ≤ 200 lines.
- Live demo URL `https://ajcpwnz.github.io/polst-embedding/` is the
  first non-title element visible.
- Each of the 7 modes has a copy-paste-able 30-second starter snippet.
- Each mode links to BOTH its live demo URL and its source file in the
  repo.
- iOS and Android sections delegate cleanly to `ios/README.md` and
  `android/README.md` (no duplicated build commands).
- Every relative file path referenced in the README resolves to an
  existing file in the repo (verified by a `for f in $(grep ...)`
  shell loop during implementation).

## Out of scope

- A new `LICENSE` file — separate ticket / decision.
- Changes to demo pages, sandbox apps, or asset files.
- Adding screenshots or hero images.
- A separate "contributing" section — out of audience.

## Dependencies

- POL-771 (FOUND-1 scaffold) — merged.
- POL-772..POL-776 (per-mode demo pages) — merged.
- POL-777 (iOS demo page) — merged on master, present in this branch.
- POL-778 (Android demo page) — merged on master, present in this branch.

## Assumptions

- GitHub Pages remains deployed from `master:/docs`, URL
  `https://ajcpwnz.github.io/polst-embedding/`.
- The CDN URLs used in the script-tag and SDK starters
  (`unpkg.com/@polst-web/widget@latest`, `esm.sh/@polst-web/sdk`) match
  what the demo pages themselves use today and what integrators would
  realistically copy.
- Linear ticket IDs are not exposed in the README (only public GitHub
  history is appropriate for an integrator-facing doc).
