# POL-780 — Tasks

1. Append a `// --- Copy handlers + Prism highlighting ---` fenced section
   to `docs/assets/chrome.js`. Implement and export:
   - `installCopyHandlers(rootEl?)` — wires every
     `button[data-copy-target]` inside `rootEl` (default `document`) to
     `navigator.clipboard.writeText`. Idempotent via `data-copy-wired`
     sentinel. On success, set the button label to "Copied!" + add
     `is-copied` class for 2000ms. On failure, leave the label alone.
     Add `aria-live="polite"` to the button if absent.
   - `installPrismHighlighting()` — injects the Prism cdnjs CSS link +
     core JS + `swift / kotlin / bash / json` addon scripts into
     `<head>`, guarded by `[data-prism="core"]` to be idempotent. On
     core load, schedule `Prism.highlightAll()` via `setTimeout(_, 0)`
     so the addon scripts have a chance to register grammars first.

   Update the file's top-of-file comment to acknowledge the new
   "Copy handlers + Prism" section.

2. Extend `bootstrap()` in `docs/assets/chrome-render.js` to call
   `installPrismHighlighting()` and `installCopyHandlers()` after the
   chrome / health banner / empty state mount. Add `opts.skipPrism`
   and `opts.skipCopyHandlers` opt-outs (default off, no current page
   sets them). Import the two new helpers from `chrome.js`.

3. Add `.copy-button`, `.copy-button:hover`, `.copy-button:focus-visible`,
   `.copy-button.is-copied`, `.snippet-block`, `.snippet-block__header`,
   `.snippet-block pre` styles to `docs/assets/chrome.css`. Match the
   existing chrome's light/dark token usage.

4. Update `docs/ios/index.html` — wrap the existing SwiftUI snippet in
   a `<div class="snippet-block">` with a header containing a copy
   `<button>`. Add `id="snippet-ios-swiftui"` to the `<code>` (the
   `class="language-swift"` is already present from POL-777).

5. Update `docs/android/index.html` — same pattern. Add
   `id="snippet-android-compose"` to the `<code>` (the
   `class="language-kotlin"` is already present from POL-778).

6. Update `docs/rn/index.html` — add a "Minimal RN integration"
   section with a static `<pre><code id="snippet-rn-import" class="language-javascript">`
   block + adjacent copy button. Mirror the existing minimal-page
   styling — small inline `<style>` additions OK; do NOT add a new
   per-page CSS file.

7. Quality gates:
   - `node --check docs/assets/chrome.js`
   - `node --check docs/assets/chrome-render.js`
   - Read each modified HTML, confirm:
     - `<pre><code>` snippet block has `id` AND `class="language-..."`.
     - Adjacent `<button data-copy-target="#<id>">Copy</button>` is
       present and the selector matches the `<code>` id exactly.
     - No broken markup (matched tags, attribute quoting).
   - Confirm `docs/iframe/`, `docs/script/`, `docs/sdk/`, `docs/api/`
     are unchanged or only see additive markup (no per-page logic
     removed).

8. Write `.local-testing-passed` marker:
   `touch specs/pol-780-polish-copy/.local-testing-passed`.

9. Commit:
   `git add -A && git commit -m "POL-780: snippet copy buttons + Prism syntax highlighting"`.
   Capture SHA via `git rev-parse HEAD`. Emit `committed` progress event.
