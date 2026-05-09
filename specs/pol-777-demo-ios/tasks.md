# POL-777 — Tasks

1. Replace `docs/ios/index.html` placeholder with the full iOS demo
   page. Mirror `docs/android/index.html` chrome shell and inline
   `<style>` block (swap `.and-*` class names for `.ios-*`), raise
   `.demo-wrap` max-width to 880px, and add the six content sections
   (header + lede, "what you get" bullets, screenshot grid, "try it"
   steps, upstream-SDK callout, SwiftUI integration snippet).
2. Add page-local CSS rules for `.ios-grid`, `.ios-grid figure`,
   `.ios-grid img`, `.ios-callout`, and `pre`/`code` inside the
   inline `<style>` block.
3. Create `docs/assets/ios/` directory containing:
   - `README.md` describing what each placeholder PNG should be
     replaced with and where the captures will come from
     (`ios/PolstSDKSandbox` running on the iOS Simulator).
   - `swiftui-single.png`, `uikit-view.png`, `offline-replay.png` —
     all three 8x8 transparent PNG placeholders, generated via a
     single `node -e` writeFileSync invocation.
4. Quality gates:
   - No JS files touched, so `node --check` is N/A.
   - Re-read `docs/ios/index.html` and confirm head/body
     structure, six sections present, alt text, link hrefs, snippet
     wrapping, and acceptance criteria from spec.
5. Write the marker file
   `specs/pol-777-demo-ios/.local-testing-passed`.
6. `git add -A && git commit -m "POL-777: iOS native demo page"`.
   Capture COMMIT_SHA via `git rev-parse HEAD`. Emit final committed
   progress event.
