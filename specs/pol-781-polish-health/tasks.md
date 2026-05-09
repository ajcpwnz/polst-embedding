# POL-781 — Tasks

1. Append a `// --- Health banner -----` fenced section to
   `docs/assets/chrome.js`. Implement and export:
   - `pingHealth(apiOrigin, opts?)` — async, 30s in-memory cache
     keyed by `apiOrigin`, 5s `AbortController` timeout, treats any
     non-2xx / network error / abort as `healthy: false`.
   - `installHealthBanner(containerEl, opts?)` — builds the
     env/host/status DOM, runs an immediate `pingHealth`, schedules a
     30s `setInterval`. Returns a teardown function. Idempotent on
     repeat-mount: clears any prior interval stored on the element.

   Update the file's top-of-file comment to acknowledge that the
   Health banner section deliberately does DOM mounting (paving the
   way for POL-780's `installCopyHandlers()`).

2. Extend `bootstrap()` in `docs/assets/chrome-render.js` to inject
   (or reuse) a `<div id="chrome-health">` element above
   `<div id="chrome">` and call `installHealthBanner()` on it. Add an
   `opts.skipHealthBanner` opt-out (default off). No per-page HTML
   changes.

3. Add `.chrome-health-banner`, `.chrome-health-banner__env`,
   `.chrome-health-banner__sep`, `.chrome-health-banner__host`,
   `.chrome-health-banner__status`, `.chrome-health-banner__label`,
   `.chrome-health-dot`, `.chrome-health-dot.is-healthy`,
   `.chrome-health-dot.is-down` styles to `docs/assets/chrome.css`.
   Match the existing chrome's light/dark token usage.

4. Quality gates:
   - `node --check docs/assets/chrome.js`
   - `node --check docs/assets/chrome-render.js`
   - Structural review of `docs/iframe/index.html` and
     `docs/sdk/index.html` — confirm `<div id="chrome">` mount points
     are still in place and the inline `bootstrap()` invocation hasn't
     been disturbed.

5. Write `.local-testing-passed` marker:
   `touch specs/pol-781-polish-health/.local-testing-passed`.

6. Commit:
   `git add -A && git commit -m "POL-781: env health banner in shared chrome"`.
   Capture SHA via `git rev-parse HEAD`. Emit `committed` progress
   event.
