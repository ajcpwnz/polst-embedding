# POL-782 — Plan

## Summary

Surgical full rewrite of `README.md`. Single file changed. No new files,
no edits to demo pages or sandbox apps. The new README is structured
top-down so an integrator who reads only the first 30 seconds gets the
live URL + their starter snippet, and an integrator who reads the whole
thing gets links to every source file plus the per-platform native docs.

## Files

- `README.md` — replaced wholesale. Order:
  1. Title + one-line tagline.
  2. **Live demo** call-out — URL on its own line, prominent.
  3. **Pick your integration mode** — one subsection per mode (iframe,
     script tag, JS SDK, REST API, iOS, Android, React Native), each
     with: live demo URL, source file path, fenced 30-second starter.
  4. **Native SDKs** — short paragraph delegating to `ios/README.md` and
     `android/README.md`.
  5. **How this site works** — two-line paragraph on `?polst=` and
     `?env=` query params.
  6. **License** — copy-OK call-out + future LICENSE plan.
  7. Footer — links to live site + source.

## Layout choice — subsections, not a table

A markdown table cannot hold multi-line fenced code blocks cleanly.
Subsections (`### iframe`, `### script tag`, ...) keep each starter
snippet readable AND keep the README scannable when readers GitHub-render
it (subsection headers populate the right-hand TOC).

Each subsection is short (1 paragraph + 2 links + 1 fenced block):

```
### iframe
Drop a single `<iframe>` on any page.

- Live: https://ajcpwnz.github.io/polst-embedding/iframe/
- Source: docs/iframe/index.html

\```html
<iframe src="https://staging.polst.app/embed/polst/<shortId>"
        width="100%" height="600" frameborder="0"></iframe>
\```
```

Eight 12-line subsections (7 modes + a small intro) ≈ 100 lines of mode
content; combined with the rest of the README budget the total stays
well under 200.

## Snippet content per mode

- **iframe** — copy the exact `src` shape `<appOrigin>/embed/<kind>/<id>`
  used by `docs/iframe/iframe.js` `buildSrc()`.
- **script tag** — `<script src="https://unpkg.com/@polst-web/widget@latest/dist/widget.esm.js" type="module" async></script>`
  + `<div data-polst="<shortId>"></div>`. Mirrors `docs/script/script.js`
  `WIDGET_SRC` + `MARKER_ATTR`.
- **JS SDK** — `import { PolstClient }` + `import { renderPolst }` from
  `https://esm.sh/@polst-web/sdk` and `/sdk/render`. Mirrors
  `docs/sdk/sdk.js` `SETUP_SNIPPET`.
- **REST API** — bare `fetch()` against `/api/rest/v1/polsts/<shortId>`.
  Mirrors `docs/api/api.js` variant `polst`.
- **iOS** — SwiftPM dependency line + a `git clone` for the sandbox.
  Defer everything else to `ios/README.md`.
- **Android** — Gradle dependency line + a `./gradlew :example:assembleDebug`
  starter. Defer everything else to `android/README.md`.
- **React Native** — RN demo page is a placeholder (1604-byte HTML,
  no live render); the README starter for RN should say so plainly:
  "RN integration is via the native iOS / Android SDKs (RN demo page
  is currently a stub)".

## "How this site works" copy

Two sentences exactly. First sentence: `?polst=<url-or-shortId>`
controls which polst the demo renders against — accepts share URLs or
raw 10-char nanoid shortIds. Second sentence: `?env=<canary|staging|prod>`
controls which backend env the demo talks to — defaults to the env
inferred from the polst URL host, falling back to staging.

## License section copy

Two-bullet list:

- No formal `LICENSE` file at the repo root yet (MIT or Apache-2.0
  forthcoming). In the meantime, integrators may freely copy any
  HTML/CSS/JS in `docs/` — that's the demo's purpose.
- Native sandbox apps under `ios/` and `android/` follow their per-
  platform license notes; see `android/LICENSE` and the upstream
  `polst-ios` SDK's license referenced from `ios/README.md`.

## QA / quality gates

1. `wc -l README.md` ≤ 200.
2. Every `docs/*/index.html`, `ios/README.md`, `android/README.md`,
   `android/LICENSE` reference resolves to a real file. Verify with:
   ```
   for f in $(grep -oE '\b(docs/[a-z]+/index\.html|ios/README\.md|android/README\.md|android/LICENSE)\b' README.md | sort -u); do
     test -e "$f" && echo OK: "$f" || echo MISSING: "$f"
   done
   ```
3. Read-through pass: from a cold start, the live URL is visible in
   the first ~10 lines; every mode subsection has a starter snippet;
   no markdown rendering bugs.
4. Marker file written: `touch specs/pol-782-polish-readme/.local-testing-passed`.

## Out of scope

- Demo page edits.
- Adding a `LICENSE` file (decision deferred).
- Hero images or screenshots in the README.
- A "Contributing" section.
- Removing or rewriting `ios/README.md` / `android/README.md`.
