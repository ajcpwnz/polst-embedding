# POL-777 — iOS native SDK demo page (text + screenshots + README link)

**Feature Branch**: `pol-777-demo-ios`
**Created**: 2026-05-09
**Status**: Draft
**Input**: User description: "POL-777 DEMO-IOS — iOS native page (text + screenshots + README link)"

## Goal

Replace the `docs/ios/index.html` placeholder with a static
informational page describing the iOS native SDK integration story.
Symmetric to the Android page (POL-778) shape, swapped for iOS idioms
(SwiftUI + UIKit, native rendering not WebView, offline cache, vote
replay queue, sandbox app at `ios/PolstSDKSandbox`). No live render,
no env switcher logic — purely text, screenshots, and links. Native
code can't run in a browser; this page tells integrators what they
get and how to try it locally.

## Scope

- `docs/ios/index.html` — the iOS demo page, replacing the current
  placeholder. Pure static markup with the shared chrome shell.
- `docs/assets/ios/` — placeholder screenshot directory with a
  `README.md` describing what each shot should depict and three 8x8
  transparent PNG placeholders so the page layout works while real
  simulator captures are pending.
- All page-local CSS lives inside the `<style>` block in
  `docs/ios/index.html` (mirroring the Android page approach). No new
  shared chrome additions, no new external CSS file.

No build step. Vanilla HTML/CSS. No JS beyond the chrome bootstrap
import that every demo page already does.

## Behaviour

### Page sections

The page is a single-column reading experience inside `.demo-wrap`,
mirroring `docs/android/index.html`. Sections appear in this order:

1. **Header strip** — chrome (mounted by `bootstrap()` into `#chrome`),
   then an inline icon (Apple emoji), `<h1>iOS SDK</h1>`, and a
   one-paragraph lede explaining "this page describes how to embed
   Polst inside an iOS app — no live render here; clone the sandbox
   to try it on a simulator or device".

2. **What you get** — a `<ul>` with bullet points covering the
   headline value props of the native SDK:
   - First-class **SwiftUI** support via the `PolstView` view, plus a
     **UIKit** `PolstViewController` for legacy view-controller-based
     screens.
   - Native rendering — no `WKWebView`, no JS bridge. The widget
     draws with the host app's SwiftUI / UIKit stack.
   - **Offline cache** — last-fetched polst payloads are persisted so
     the widget renders even when the device is offline.
   - **Vote replay queue** — votes cast offline are queued locally
     and replayed once connectivity returns.
   - Encrypted device-id and token storage via the iOS Keychain.
   - Theming via SwiftUI environment / `PolstClient(environment:)`
     configuration (production / staging / dev / custom).

3. **Screenshots / GIFs** — a grid of three figures pulled from
   `docs/assets/ios/`:
   - `swiftui-single.png` — single Polst rendered via the SwiftUI
     `PolstView` in the sandbox app.
   - `uikit-view.png` — same Polst rendered via the UIKit
     `PolstViewController` surface.
   - `offline-replay.png` (or GIF later) — the offline replay queue
     flushing once connectivity returns.

   Each figure has a `<figcaption>` explaining the shot. Real captures
   come later from the iOS Simulator; for now the directory ships
   transparent 8x8 PNG placeholders so the layout doesn't collapse.

4. **Try it** — numbered ordered list:
   1. Clone the embedding repo
      (`git clone git@github.com:ajcpwnz/polst-embedding.git`).
   2. Open `ios/PolstSDKSandbox.xcodeproj` in **Xcode**, pick an
      iOS Simulator, press `Cmd+R`.
   3. Follow `ios/README.md` for sandbox-specific run notes
      (Swift Package resolution, command-line build).
   4. For SDK source / issues / releases, see the upstream
      [`polst-ios`](https://github.com/ajcpwnz/polst-ios) repo.

5. **Upstream-SDK note** — a callout box explaining that the iOS
   sandbox depends on the upstream
   [`polst-ios`](https://github.com/ajcpwnz/polst-ios) Swift package
   (`git@github.com:ajcpwnz/polst-ios.git`) resolved through Xcode's
   Swift Package Manager. Bug fixes and feature work go upstream;
   the sandbox tracks tagged releases.

6. **Minimal SwiftUI integration snippet** — the canonical
   "render a polst" call inside a `<pre><code class="language-swift">`
   block. Copy-paste-able. The snippet shows:

   ```swift
   import PolstSDK
   import SwiftUI

   // One-time at app startup — pick the env you need.
   private let polstClient = PolstClient(environment: .production)

   @main
   struct MyApp: App {
     var body: some Scene {
       WindowGroup {
         PolstView(shortId: "abc123XYZ_", client: polstClient)
       }
     }
   }
   ```

   The `shortId` placeholder is a stand-in for any real 10-char Polst
   short id; the snippet is for shape, not for paste-and-run.

### Empty state

There is no `?polst=` empty state on this page — the page is
informational, not a live render. The shared chrome still mounts
(env switcher, footer) but `#demo` stays empty. That matches the
Android page behaviour.

### No live data, no env switcher

- The page does NOT call `getApiOrigin()` / `getPolstTarget()` /
  `renderEmptyState()`.
- The page does NOT execute any HTTP request.
- The chrome bar does still show the env switcher (mounted by
  `bootstrap()`), since it's a global header — but switching env on
  this page just reloads with `?env=`, which has no functional effect
  on a static info page. The iOS env is configured at SDK init in
  app code (`PolstClient(environment: ...)`).

## Acceptance

- Page loads under `docs/ios/index.html` and renders the chrome
  shell, header, "what you get" bullets, screenshot grid (with
  placeholder images), "try it" steps, upstream-SDK callout, and the
  SwiftUI integration snippet.
- The link to `polst-embedding/ios/README.md` resolves
  (relative path: `../../ios/README.md` from `docs/ios/index.html`).
- The link to upstream
  [`polst-ios`](https://github.com/ajcpwnz/polst-ios)
  resolves.
- The SwiftUI snippet is wrapped in a `<pre><code>` block and is
  copy-paste-able as Swift source.
- HTML parses cleanly (structural review).
- `node --check` passes for any `.js` touched (none expected for this
  page, so this gate is N/A).

## Out of scope

- Live SDK execution in the browser (impossible — the SDK is a native
  iOS / Swift Package).
- Real simulator screenshots — placeholder images ship now; real
  captures are a follow-up once the sandbox has a stable demo flow.
- Snippet copy buttons / syntax highlighting — those land in POL-780
  (POLISH-COPY) project-wide.
- Health banner on the chrome — POL-781.
- Cross-mode demos (iframe, script, sdk, android, rn, api) — separate
  tickets.
- Backend changes — N/A for an info page.

## Dependencies

- POL-771 (FOUND-1 scaffold) — merged.
- POL-772 (FOUND-2 chrome with `bootstrap()`) — merged.
- POL-778 (DEMO-AND) — sibling page; merged. POL-777 mirrors its
  structural shape so the two native-platform pages read as a pair.

## Assumptions

- The `polst-embedding` repo continues to ship a runnable iOS sandbox
  at `ios/PolstSDKSandbox.xcodeproj` (already exists; see
  `ios/README.md`, `ios/PolstSDKSandbox/PolstSDKSandboxApp.swift`,
  `ios/PolstSDKSandbox/ContentView.swift`).
- The upstream SDK lives at
  [`polst-ios`](https://github.com/ajcpwnz/polst-ios) and is consumed
  by the sandbox via Swift Package Manager
  (`git@github.com:ajcpwnz/polst-ios.git`).
- The canonical SwiftUI render API is
  `PolstView(shortId:, client:)` and the canonical UIKit bridge is
  `PolstViewController(shortId:, client:)` — matches the existing
  sandbox code at `ios/PolstSDKSandbox/ContentView.swift`.
- The recommended client init pattern is
  `PolstClient(environment: .production)` (or `.staging` / `.dev` /
  `.custom`), constructed once and passed into each render surface.
- Modern evergreen browser baseline for the demo site, same as every
  other page.
