# POL-778 — Tasks

1. Replace `docs/android/index.html` placeholder with the full Android
   demo page. Mirror `docs/ios/index.html` chrome shell and inline
   `<style>` block, raise `.demo-wrap` max-width to 880px, and add the
   six content sections (header + lede, "what you get" bullets,
   screenshots grid, "try it" steps, vendored-mirror callout, Compose
   integration snippet).
2. Add page-local CSS rules for `.and-grid`, `.and-grid figure`,
   `.and-grid img`, `.and-callout`, and `pre`/`code` inside the
   inline `<style>` block.
3. Create `docs/assets/android/` directory containing:
   - `README.md` describing what each placeholder PNG should be
     replaced with and where the captures will come from.
   - `compose-single.png`, `xml-view.png`, `offline-replay.png` —
     all three 8x8 transparent PNG placeholders, generated via a
     single `node -e` writeFileSync invocation.
4. Quality gates:
   - No JS files touched, so `node --check` is N/A.
   - Re-read `docs/android/index.html` and confirm head/body
     structure, six sections present, alt text, link hrefs, snippet
     wrapping, and acceptance criteria from spec.
5. Write the marker file
   `specs/pol-778-demo-android/.local-testing-passed`.
6. `git add -A && git commit -m "POL-778: Android native demo page"`.
   Capture COMMIT_SHA via `git rev-parse HEAD`. Emit final committed
   progress event.
