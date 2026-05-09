# POL-777 — Plan

## Summary

Replace the placeholder `docs/ios/index.html` with a static
information page for the iOS native SDK. The page mounts the shared
chrome via `bootstrap()`, and renders six text/markup sections:
header + lede, "what you get" bullets, screenshot grid, "try it"
steps, upstream-SDK callout, and a SwiftUI integration snippet. No
JavaScript beyond the chrome bootstrap import. No live render. No
runtime SDK call.

Symmetric to the Android page (`docs/android/index.html`) — same
structural shape and inline `<style>` block, swapped for iOS idioms.

## Files

- `docs/ios/index.html` — replace placeholder. Same chrome shell,
  inline `<style>` block ported from `docs/android/index.html` (only
  iOS-specific class-name swaps and the `.demo-wrap` width/colours
  carry over verbatim), same `bootstrap()` import. Body adds the new
  sections in order. `.demo-wrap` widens to ~880px to accommodate the
  screenshot grid (matches Android).
- `docs/assets/ios/README.md` — describe what each placeholder
  PNG/GIF should be replaced with, and where the real captures will
  come from (the iOS Simulator running `ios/PolstSDKSandbox`).
- `docs/assets/ios/swiftui-single.png` — 8x8 transparent placeholder
  PNG.
- `docs/assets/ios/uikit-view.png` — 8x8 transparent placeholder PNG.
- `docs/assets/ios/offline-replay.png` — 8x8 transparent placeholder
  PNG (real asset will eventually be a GIF).

No new shared chrome additions. No new CSS file — the tiny page-local
styles fit in the inline `<style>` block.

## Section markup

1. **Header**: `<span class="icon">🍎</span><h1>iOS SDK</h1><p
   class="lede">…</p>` — mirror Android shape.
2. **What you get**: `<section class="ios-features"><h2>What you get</h2>
   <ul>…</ul></section>` — six bullets per spec (SwiftUI + UIKit,
   native rendering, offline cache, vote replay queue, Keychain
   storage, env via `PolstClient`).
3. **Screenshots**: `<section class="ios-shots"><h2>Screenshots</h2>
   <div class="ios-grid">` with three `<figure><img …><figcaption>…
   </figcaption></figure>` blocks. CSS gives `.ios-grid` a simple
   responsive `grid-template-columns: repeat(auto-fit, minmax(220px,
   1fr))` layout.
4. **Try it**: `<section class="ios-tryit"><h2>Try it locally</h2>
   <ol>…</ol></section>` — four numbered steps. Includes:
   - Inline `<code>` for the clone command and the `open
     ios/PolstSDKSandbox.xcodeproj` command.
   - A relative link to `../../ios/README.md` (the sandbox README at
     the repo root).
   - An external link to
     [`polst-ios`](https://github.com/ajcpwnz/polst-ios).
5. **Upstream-SDK note**: `<aside class="ios-callout">` with a
   paragraph explaining that the iOS sandbox depends on the upstream
   `polst-ios` Swift package via SwiftPM, fixes go upstream first.
6. **Snippet**: `<section class="ios-snippet"><h2>Minimal SwiftUI
   integration</h2><pre><code class="language-swift">…</code></pre>
   </section>` — inline Swift source. The `&` glyph in
   `git@github.com` is fine inside an attribute, but no `&`/`<`
   characters appear in the snippet body itself, so no entity
   escaping needed.

## CSS additions (inline `<style>` in `index.html`)

Mirror the Android page's inline block, swapping the `.and-*`
class-name prefix for `.ios-*`:

- `.demo-wrap` width raised to 880px.
- `.demo-wrap h2` styling (carried over from Android).
- `.demo-wrap p.lede`, `.demo-wrap ul/ol/li`, `.demo-wrap code` —
  carry over from Android verbatim.
- `.ios-grid` — CSS grid with `auto-fit, minmax(220px, 1fr)`, gap 16px.
- `.ios-grid figure` — zero margin, frame with rounded border + small
  padding, light/dark friendly via existing `--bg`/`--fg` tokens.
- `.ios-grid img` — `width:100%`, `aspect-ratio: 16/10`,
  `object-fit:cover`, `display:block`.
- `.ios-callout` — light-bg box (use `color-mix` against `--fg`),
  rounded, `border-left: 3px solid var(--muted)`, `padding: 12px 16px`.
- `pre` — overflow-x:auto, `padding:16px`, `border-radius:8px`,
  `background: color-mix(in srgb, var(--fg) 6%, transparent)`,
  `font-family: ui-monospace, SFMono-Regular, Menlo, Consolas,
  monospace`, `font-size: 0.875rem`, `line-height:1.5`.

All other styling inherits from `chrome.css`.

## Placeholder PNG generation

Three 8x8 transparent PNGs. Generated programmatically with `node`
(no external deps): use a deterministic minimal PNG byte string at
8x8 to avoid any browser-side "image too small" weirdness. Generate
via a single inline `node -e "..."` invocation that writes via
`fs.writeFileSync`. The three files are byte-identical placeholders;
only the filenames differ so the HTML's three `<img src>`s resolve
and the layout works. Mirrors the Android approach.

## Quality gates

- `node --check` — no JS files touched on this page (the inline
  `<script type="module">` is just a one-line bootstrap import,
  identical to the Android page; not a separate `.js` file).
  Therefore N/A.
- HTML structural review — re-read the written file once after
  writing, confirm:
  - `<head>` has `<link rel="stylesheet" href="../assets/chrome.css">`.
  - `<body>` has `<div id="chrome"></div>` and the bootstrap import
    (`bootstrap({ sourcePath: 'docs/ios/index.html' })`).
  - All six sections present, in order.
  - All three `<img>` tags have non-empty `alt` text.
  - All links have `href` and resolve (relative for the README; full
    https URL for upstream SDK).
  - The Swift snippet is inside `<pre><code class="language-swift">`,
    not split across block boundaries.
- Acceptance criteria from spec section verified by the same re-read.

## Out of scope

- Any JS execution.
- Live screenshots — placeholders only; capture follow-up tracked
  separately by whoever does the sandbox QA.
- Snippet copy buttons / syntax highlighting (POL-780).
- Health banner (POL-781).
- README update (POL-782).
