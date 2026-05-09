# POL-780 — Snippet copy buttons + per-demo "ready to paste" code blocks

**Feature Branch**: `pol-780-polish-copy`
**Created**: 2026-05-09
**Status**: Draft
**Input**: User description: "POL-780 POLISH-COPY — Snippet copy buttons + per-demo 'ready to paste' code blocks"

## Goal

Every demo page has at least one clearly-labeled "Copy this snippet" code
block representing the bare-minimum integration code for that mode.
Click-to-copy. Visual feedback on success ("Copied!" for ~2s, then back
to "Copy"). Lightweight syntax highlighting so the snippet looks like
code, not plain text.

## Scope

- `docs/assets/chrome.js` — add `installCopyHandlers()`, a self-contained
  mount helper that scans the document for `<button data-copy-target="#id">`
  elements and wires them to `navigator.clipboard.writeText` with a 2s
  "Copied!" affordance. Lives in its own clearly-fenced section, mirroring
  the "Health banner" section pattern POL-781 established.
- `docs/assets/chrome.js` — add `installPrismHighlighting()`, a small
  helper that lazily injects the Prism CSS link + JS bundle (with the
  language addons we use: html / javascript / swift / kotlin / bash /
  json) and runs `Prism.highlightAll()` once loaded.
- `docs/assets/chrome-render.js` — wire both helpers into `bootstrap()`
  so every demo page gets copy + highlighting without per-page changes.
- `docs/assets/chrome.css` — add `.copy-button` styles (small hover-able
  button, "is-copied" success state). Keep the existing per-page snippet
  panels (iframe, script, sdk, api) untouched — they already include
  their own per-page copy buttons; the new helper is additive and
  targets the static snippet blocks on `ios`, `android`, plus any
  static block we add elsewhere.
- `docs/ios/index.html` — add `id`, `class="language-swift"`, and an
  adjacent `<button data-copy-target="#...">Copy</button>` to the
  existing SwiftUI snippet block. Same for any additional static blocks.
- `docs/android/index.html` — same treatment for the Kotlin snippet.
- `docs/iframe/index.html`, `docs/script/index.html`, `docs/sdk/index.html`,
  `docs/api/index.html` — already render dynamic snippets with per-page
  copy buttons. No structural change required. (Acceptance: each demo
  page has at least one copyable snippet — the existing per-page buttons
  already satisfy this. No regressions.)
- `docs/rn/index.html` — add a static "minimal RN integration" snippet
  with `id`, `class="language-javascript"`, and a copy button so the RN
  page also has at least one copyable snippet.

## Behaviour

### Copy handlers

`installCopyHandlers(rootEl?)` scans `rootEl` (default `document`) for
`button[data-copy-target]` elements. For each match:

1. Resolve the target via `document.querySelector(button.dataset.copyTarget)`.
   The selector is expected to point at a `<pre><code>` (or any
   `textContent`-bearing) element. Skip the wiring if the selector
   resolves to nothing.
2. Bind a `click` handler that:
   - Reads `target.textContent` (raw, pre-highlight content).
   - Calls `navigator.clipboard.writeText(text)`.
   - On success: set the button's `textContent` to `Copied!`, add an
     `is-copied` class, and after 2000ms revert to the original label and
     remove the class.
   - On failure (rejected promise, no Clipboard API, insecure context):
     leave the button label unchanged. The user-facing demo audience runs
     on `https://*.github.io` (secure context) or `localhost` (also
     secure), so the failure case is purely defensive — no DOM-based
     `execCommand` fallback is shipped.
3. Be idempotent — wiring the same button twice is a no-op. We achieve
   this by setting a `data-copy-wired="1"` attribute and short-circuiting
   when present.

The button's accessible name is its existing label ("Copy"). After a
successful copy, the button label updates to "Copied!" — this update is
exposed to AT users via `aria-live="polite"` set on the button itself.
(The button is a focusable interactive element; a visible label change
on a focused element is announced by all major screen readers without
requiring a separate live region.)

### Syntax highlighting

We use **Prism via cdnjs** (Cloudflare). Trade-off vs. shipping a
downloaded 8KB bundle: CDN keeps this repo zero-build and inherits
upstream Prism patches automatically. The cost is one extra DNS lookup
per page. This is acceptable for a demo site whose entire purpose is
showcasing third-party CDN-delivered embeds.

Languages loaded (per the demos in this repo): `html` (default in core),
`javascript` (default in core), `swift`, `kotlin`, `bash`, `json`.

`installPrismHighlighting()`:

1. If the page already has a `<script data-prism="core">` element, no-op.
2. Inject `<link rel="stylesheet" href="https://cdnjs.cloudflare.com/.../prism.min.css">`
   into `<head>`.
3. Inject the core `<script src="https://cdnjs.cloudflare.com/.../prism.min.js" data-prism="core" defer>`,
   followed by language addon scripts (swift, kotlin, bash, json — html
   and javascript are bundled in core).
4. Once the core script loads, call `window.Prism.highlightAll()`. (The
   addon scripts auto-extend Prism on parse; `highlightAll` runs once
   they're all loaded.)

The helper is invoked from `bootstrap()` on every demo page — pages
that don't have any `class="language-*"` blocks simply produce a no-op
highlight pass.

### bootstrap() wiring

`bootstrap()` in `chrome-render.js` already mounts the chrome shell and
the health banner. Two additions:

- `installPrismHighlighting()` — injects assets, schedules a single
  `Prism.highlightAll()` once core loads.
- `installCopyHandlers()` — runs after the DOM is settled to wire any
  `[data-copy-target]` buttons present in per-page HTML.

Both calls are gated by `opts.skipCopyHandlers` / `opts.skipPrism`
(default: include) so a future page can opt out if needed. No current
page sets either.

## Acceptance

- All seven demo pages (`iframe`, `script`, `sdk`, `api`, `ios`,
  `android`, `rn`) have at least one copyable snippet.
- Click-to-copy works in current Chrome, Safari, and Firefox (any
  modern browser supporting `navigator.clipboard.writeText` in a secure
  context).
- After clicking copy, the button label flips to "Copied!" for ~2s,
  then reverts.
- Syntax highlighting renders on the static `<pre><code class="language-...">`
  blocks on `ios/index.html`, `android/index.html`, and `rn/index.html`.
- `node --check docs/assets/chrome.js` and
  `node --check docs/assets/chrome-render.js` pass.
- HTML structural review on `ios/index.html`, `android/index.html`,
  `rn/index.html` confirms each snippet block has `id`,
  `class="language-..."`, and an adjacent `<button data-copy-target="#id">Copy</button>`.

## Out of scope

- Replacing the per-page copy buttons on `iframe`, `script`, `sdk`, and
  `api` demo pages (each already builds dynamic snippets and ships its
  own copy button). These pages will continue using their per-page
  buttons — they predate this ticket. The new shared `installCopyHandlers`
  is additive and only fires for buttons that declare
  `data-copy-target`. The dynamic-snippet pages do NOT use that
  attribute, so the helpers don't double-wire.
- Hosting Prism locally / vendoring an 8KB bundle. We picked CDN. If a
  later ticket wants to drop the CDN dependency, swap the loader inside
  `installPrismHighlighting()` only — call sites stay identical.
- Per-page configurable copy success messages or durations. Hard-coded
  "Copied!" + 2000ms.
- Multi-line copy preserving HTML escapes — copy reads `textContent`,
  which already gives the un-escaped source the user sees.
- POL-789+ follow-ups: rich syntax themes, dark-mode Prism theme
  switching, copy analytics. Out of scope.

## Dependencies

- POL-771 (FOUND-1 scaffold) — merged.
- POL-772 (FOUND-2 chrome) — merged.
- POL-781 (FOUND-3 health banner) — merged. Establishes the
  `installX()` mount-helper pattern this ticket follows.
- POL-777 / POL-778 (iOS / Android demo pages) — merged. Provide the
  static `<pre><code class="language-swift|kotlin">` blocks this ticket
  attaches to.

## Assumptions

- `navigator.clipboard.writeText` is available in every browser the
  demo site supports (modern evergreen). Secure-context requirement is
  satisfied by GitHub Pages (HTTPS) and `localhost`.
- Prism cdnjs URLs are stable and CORS-allowed for `<script>` and
  `<link>` cross-origin loads. (Cloudflare's CDN sets permissive CORS
  headers; well-trodden integration path.)
- The Prism bundle plus addons is comfortably under 30KB transferred,
  far under the budget of any demo page.
- Each per-page dynamic copy button on `iframe/script/sdk/api` keeps
  working as-is. We do NOT touch those code paths.
