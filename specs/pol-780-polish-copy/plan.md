# POL-780 — Plan

## Summary

Add a shared `installCopyHandlers()` mount helper plus
`installPrismHighlighting()` to `docs/assets/chrome.js`, wire both into
`bootstrap()` (`chrome-render.js`), and tag each static snippet on the
ios / android / rn pages with `id` + `class="language-<lang>"` + an
adjacent copy button. Pre-existing dynamic copy buttons on iframe /
script / sdk / api stay as-is — they predate this ticket and continue
to satisfy the "every demo page has a copyable snippet" acceptance
criterion. Prism is loaded via cdnjs CDN to keep the repo zero-build.

## Files

- `docs/assets/chrome.js` — append a fenced "Copy handlers + Prism"
  section near the bottom (after the "Health banner" section
  POL-781 added) exporting:
  - `installCopyHandlers(rootEl?)` — DOM mount that wires every
    `button[data-copy-target]` inside `rootEl` (default `document`) to
    `navigator.clipboard.writeText`. Idempotent via a `data-copy-wired`
    sentinel attribute. On success, sets the button to "Copied!" for
    2000ms before reverting. Adds `aria-live="polite"` to the button so
    AT users hear the success label change.
  - `installPrismHighlighting()` — injects the Prism cdnjs CSS link +
    JS scripts into `<head>` once per document (guarded by
    `[data-prism="core"]`), and runs `Prism.highlightAll()` once core
    has loaded. Languages: html / javascript (core) plus swift, kotlin,
    bash, json (autoloaded addons).

- `docs/assets/chrome-render.js` — extend `bootstrap()` to call
  `installPrismHighlighting()` (always) and `installCopyHandlers()`
  (always) after the chrome / health banner / empty state are mounted.
  Add `opts.skipCopyHandlers` and `opts.skipPrism` opt-outs (default
  off, no current page sets them).

- `docs/assets/chrome.css` — add `.copy-button`, `.copy-button:hover`,
  `.copy-button.is-copied` styles. Light/dark-aware via the existing
  chrome tokens. Also add a small wrapper `.snippet-block` that lets
  the code page lay out the `<pre>` and the adjacent `<button>` in a
  consistent way (heading + button row, `<pre>` below).

- `docs/ios/index.html` — wrap the existing `<pre><code class="language-swift">`
  block with a `<div class="snippet-block">` containing a header row
  ("Copy" button + optional H3) and the `<pre>`. Add `id="snippet-ios-swiftui"`
  to the `<code>` element. Button: `<button class="copy-button" data-copy-target="#snippet-ios-swiftui" type="button" aria-live="polite">Copy</button>`.

- `docs/android/index.html` — same treatment. `id="snippet-android-compose"`,
  `class="language-kotlin"` already present.

- `docs/rn/index.html` — add a static "minimal RN integration" section
  with a single `<pre><code id="snippet-rn-import" class="language-javascript">`
  block + adjacent copy button. Mirror the page's existing minimal-page
  styling (no extra CSS file — inline the small bits required). The
  snippet shows the `react-native-web`-friendly bridge pattern that
  `apps/frontend` consumes (importing the SDK and rendering
  `<PolstView shortId="...">`-equivalent JSX).

## API shapes

```js
// chrome.js — Copy handlers + Prism section

const COPY_FEEDBACK_MS = 2000;

export function installCopyHandlers(rootEl) {
  const root = rootEl || (typeof document !== 'undefined' ? document : null);
  if (!root) return;
  const buttons = root.querySelectorAll('button[data-copy-target]');
  for (const btn of buttons) {
    if (btn.getAttribute('data-copy-wired') === '1') continue;
    btn.setAttribute('data-copy-wired', '1');
    if (!btn.hasAttribute('aria-live')) btn.setAttribute('aria-live', 'polite');
    const originalLabel = btn.textContent || 'Copy';
    btn.addEventListener('click', async () => {
      const selector = btn.getAttribute('data-copy-target');
      if (!selector) return;
      const target = document.querySelector(selector);
      if (!target) return;
      const text = target.textContent || '';
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
          btn.textContent = 'Copied!';
          btn.classList.add('is-copied');
          window.setTimeout(() => {
            btn.textContent = originalLabel;
            btn.classList.remove('is-copied');
          }, COPY_FEEDBACK_MS);
        }
      } catch {
        /* leave label unchanged */
      }
    });
  }
}

export function installPrismHighlighting() {
  if (typeof document === 'undefined') return;
  if (document.head.querySelector('script[data-prism="core"]')) return;

  const css = document.createElement('link');
  css.rel = 'stylesheet';
  css.href = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css';
  css.setAttribute('data-prism', 'css');
  document.head.appendChild(css);

  const core = document.createElement('script');
  core.src = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js';
  core.defer = true;
  core.setAttribute('data-prism', 'core');
  core.addEventListener('load', () => {
    // addons may still be loading at this point — schedule highlight on
    // the next tick so they have a chance to register their grammars.
    window.setTimeout(() => {
      const Prism = /** @type {any} */ (window).Prism;
      if (Prism && typeof Prism.highlightAll === 'function') Prism.highlightAll();
    }, 0);
  });
  document.head.appendChild(core);

  const addons = ['swift', 'kotlin', 'bash', 'json'];
  for (const lang of addons) {
    const s = document.createElement('script');
    s.src = `https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-${lang}.min.js`;
    s.defer = true;
    s.setAttribute('data-prism', `lang-${lang}`);
    document.head.appendChild(s);
  }
}
```

## Snippet block markup pattern

```html
<div class="snippet-block">
  <div class="snippet-block__header">
    <h3>Minimal SwiftUI integration</h3>
    <button
      class="copy-button"
      data-copy-target="#snippet-ios-swiftui"
      type="button"
      aria-live="polite">Copy</button>
  </div>
  <pre><code id="snippet-ios-swiftui" class="language-swift">…</code></pre>
</div>
```

`installCopyHandlers()` queries `[data-copy-target]` globally so the
new buttons are wired automatically. The dynamic-snippet demos
(iframe, script, sdk, api) do NOT use `data-copy-target`, so the
shared helper does not interfere with their per-page copy logic.

## bootstrap() wiring

```js
export function bootstrap(opts) {
  const run = () => {
    const chromeEl = document.getElementById('chrome');
    const demoEl = document.getElementById('demo');
    if (!opts || !opts.skipHealthBanner) {
      const healthEl = ensureHealthMount(chromeEl);
      if (healthEl) installHealthBanner(healthEl);
    }
    if (chromeEl) renderChrome(chromeEl, opts);
    if (demoEl && !getPolstTarget()) renderEmptyState(demoEl);
    if (!opts || !opts.skipPrism) installPrismHighlighting();
    if (!opts || !opts.skipCopyHandlers) installCopyHandlers();
  };
  // …same readyState gating as before
}
```

## QA

- `node --check docs/assets/chrome.js`
- `node --check docs/assets/chrome-render.js`
- HTML structural review on `docs/ios/index.html`, `docs/android/index.html`,
  `docs/rn/index.html`:
  - confirm each snippet block has `id`, `class="language-<lang>"`,
    and an adjacent `<button data-copy-target="#id">Copy</button>`.
  - confirm no broken markup (matched tags, valid attribute quoting).
- HTML structural review on `docs/iframe/index.html` to confirm the
  inline `bootstrap()` invocation and per-page dynamic copy logic are
  untouched.
- Marker file written after the gates pass.

## Out of scope

- Touching the dynamic-snippet copy buttons on iframe / script / sdk /
  api. They keep their per-page behavior.
- Vendoring Prism. We use cdnjs.
- Configurable copy success durations.
- Dark-mode Prism theme switching (would require swapping
  `prism.min.css` for `prism-tomorrow.min.css` based on
  `prefers-color-scheme`). Defer to a follow-up if requested.
