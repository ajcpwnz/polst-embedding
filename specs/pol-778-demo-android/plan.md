# POL-778 — Plan

## Summary

Replace the placeholder `docs/android/index.html` with a static
information page for the Android native SDK. The page mounts the
shared chrome via `bootstrap()`, and renders six text/markup sections:
header + lede, "what you get" bullets, screenshot grid, "try it"
steps, vendored-mirror note, and a Compose integration snippet. No
JavaScript beyond the chrome bootstrap import. No live render. No
runtime SDK call.

Symmetric to the iOS page (`docs/ios/index.html`) — same structural
shape, swapped for Android idioms.

## Files

- `docs/android/index.html` — replace placeholder. Same chrome shell,
  same `<style>` block (mirror iOS for visual parity), same
  `bootstrap()` import. Body adds the new sections in order.
  `.demo-wrap` widens to ~880px to accommodate the screenshot grid
  (iOS page uses 720px; widening here is fine because the screenshot
  trio is the visual anchor).
- `docs/assets/android/README.md` — describe what each placeholder
  PNG/GIF should be replaced with, and where the real captures will
  come from (the sandbox app at `android/example/`).
- `docs/assets/android/compose-single.png` — 8x8 transparent
  placeholder PNG.
- `docs/assets/android/xml-view.png` — 8x8 transparent placeholder PNG.
- `docs/assets/android/offline-replay.png` — 8x8 transparent
  placeholder PNG (real asset will eventually be a GIF).

No new shared chrome additions. No new CSS file — the tiny page-local
styles fit in the inline `<style>` block.

## Section markup

1. **Header**: `<span class="icon">🤖</span><h1>Android SDK</h1><p
   class="lede">…</p>` — mirror iOS shape.
2. **What you get**: `<section class="and-features"><h2>What you get</h2>
   <ul>…</ul></section>` — six bullets per spec.
3. **Screenshots**: `<section class="and-shots"><h2>Screenshots</h2>
   <div class="and-grid">` with three `<figure><img …><figcaption>…
   </figcaption></figure>` blocks. CSS gives `.and-grid` a simple
   responsive `grid-template-columns: repeat(auto-fit, minmax(220px,
   1fr))` layout.
4. **Try it**: `<section class="and-tryit"><h2>Try it locally</h2>
   <ol>…</ol></section>` — four numbered steps. Includes:
   - Inline `<code>` for the clone command and the gradle command.
   - A relative link to `../../android/README.md` (the sandbox README
     at the repo root).
   - An external link to
     [`polst-android`](https://github.com/ajcpwnz/polst-android).
5. **Vendored note**: `<aside class="and-callout">` with a paragraph
   explaining that `android/sdk/` is a vendored mirror, fixes go
   upstream first.
6. **Snippet**: `<section class="and-snippet"><h2>Minimal Compose
   integration</h2><pre><code class="language-kotlin">…</code></pre>
   </section>` — inline Kotlin source, `&lt;` / `&amp;` escaped where
   needed (none for this snippet, but worth noting).

## CSS additions (inline `<style>` in `index.html`)

- `.demo-wrap` width raised to 880px.
- `.and-grid` — CSS grid with `auto-fit, minmax(220px, 1fr)`, gap 16px.
- `.and-grid figure` — zero margin, frame with rounded border + small
  padding, light/dark friendly via existing `--bg`/`--fg` tokens.
- `.and-grid img` — `width:100%`, `aspect-ratio: 16/10`,
  `object-fit:cover`, `display:block`.
- `.and-callout` — light-bg box (use `color-mix` against `--fg`),
  rounded, `border-left: 3px solid var(--muted)`, `padding: 12px 16px`.
- `pre` — overflow-x:auto, `padding:16px`, `border-radius:8px`,
  `background: color-mix(in srgb, var(--fg) 6%, transparent)`,
  `font-family: ui-monospace, SFMono-Regular, Menlo, Consolas,
  monospace`, `font-size: 0.875rem`, `line-height:1.5`.

All other styling inherits from `chrome.css` and the existing inline
block in the iOS page.

## Placeholder PNG generation

Three 8x8 transparent PNGs. Generated programmatically with `node`
(no external deps): use the deterministic minimal PNG byte string
documented in many "1x1 transparent PNG" snippets, but at 8x8 to
avoid any browser-side "image too small" weirdness. Generate via a
single inline `node -e "..."` invocation that writes via
`fs.writeFileSync`. Keep the three identical bytes (each file is the
same transparent square; they only need different filenames so the
HTML's three `<img src>`s resolve and the layout works).

## Quality gates

- `node --check` — no JS files touched on this page (the inline
  `<script type="module">` is just a one-line bootstrap import,
  identical to the iOS page; not a separate `.js` file). Therefore
  N/A.
- HTML structural review — re-read the written file once after
  writing, confirm:
  - `<head>` has `<link rel="stylesheet" href="../assets/chrome.css">`.
  - `<body>` has `<div id="chrome"></div>` and the bootstrap import.
  - All six sections present, in order.
  - All three `<img>` tags have non-empty `alt` text.
  - All links have `href` and resolve (relative for the README; full
    https URL for upstream SDK).
  - The Kotlin snippet is inside `<pre><code>`, not split across
    block boundaries.
- Acceptance criteria from spec section verified by the same re-read.

## Out of scope

- Any JS execution.
- Live screenshots — placeholders only; capture follow-up tracked
  separately by whoever does the sandbox QA.
- Snippet copy buttons / syntax highlighting (POL-780).
- Health banner (POL-781).
- README update (POL-782).
