# POL-778 — Android native SDK demo page (text + screenshots + README link)

**Feature Branch**: `pol-778-demo-android`
**Created**: 2026-05-09
**Status**: Draft
**Input**: User description: "POL-778 DEMO-AND — Android native page (text + screenshots + README link)"

## Goal

Replace the `docs/android/index.html` placeholder with a static
informational page describing the Android native SDK integration story.
Mirror of the iOS page (POL-777) shape, swapped for Android idioms
(Compose + XML View, native rendering, vendored mirror, sandbox app).
No live render, no env switcher logic — purely text, screenshots, and
links.

## Scope

- `docs/android/index.html` — the Android demo page, replacing the
  current placeholder. Pure static markup with the shared chrome shell.
- `docs/assets/android/` — placeholder screenshot directory with a
  `README.md` describing what each shot should depict and 2-3 1x1
  transparent PNG placeholders so the page layout works while real
  screenshots are pending.
- (Optional) page-specific `android.css` if minor layout additions are
  needed beyond the shared `chrome.css`. Default: inline a tiny `<style>`
  block in `index.html` to keep parity with the iOS page (`docs/ios/index.html`).

No build step. Vanilla HTML/CSS. No JS beyond the chrome bootstrap
import that every demo page already does.

## Behaviour

### Page sections

The page is a single-column reading experience inside `.demo-wrap`,
mirroring `docs/ios/index.html`. Sections appear in this order:

1. **Header strip** — chrome (mounted by `bootstrap()` into `#chrome`),
   then an inline icon (Android robot emoji), `<h1>Android SDK</h1>`,
   and a one-paragraph lede explaining "this page describes how to
   embed Polst inside an Android app — no live render here; clone the
   sandbox to try it locally".

2. **What you get** — a `<ul>` with bullet points covering the
   headline value props of the native SDK:
   - First-class **Jetpack Compose** support (`PolstView` composable)
     plus an **XML `View`** for legacy view-based screens.
   - Native rendering — no `WebView`, no JS bridge.
   - **Offline cache** — last-fetched polst payloads are persisted so
     the widget renders even when the device is offline.
   - **Vote replay queue** — votes cast offline are queued in a local
     Room database and replayed by a `WorkManager` job once
     connectivity returns.
   - Encrypted device-id and token storage via
     `EncryptedSharedPreferences`.
   - Theming via `PolstThemeProvider` (light/dark/custom palettes).

3. **Screenshots / GIFs** — a grid of three figures pulled from
   `docs/assets/android/`:
   - `compose-single.png` — single Polst rendered via the Compose API
     in the sandbox app.
   - `xml-view.png` — same Polst rendered via the legacy XML `View`
     surface.
   - `offline-replay.gif` (or PNG placeholder) — the offline replay
     queue flushing once connectivity returns.

   Each figure has a `<figcaption>` explaining the shot. Real captures
   come later; for now the directory ships transparent 8x8 PNG
   placeholders so the layout doesn't collapse.

4. **Try it** — numbered ordered list:
   1. Clone the embedding repo
      (`git clone git@github.com:ajcpwnz/polst-embedding.git`).
   2. Open the `android/` directory in **Android Studio**, or build from
      the CLI (`./gradlew :example:assembleDebug`).
   3. Follow `android/README.md` for sandbox-specific run notes.
   4. For SDK source / issues / releases, see the upstream
      [`polst-android`](https://github.com/ajcpwnz/polst-android) repo.

5. **Vendored-mirror note** — a callout box explaining that
   `android/sdk/` in this embedding repo is a **vendored mirror** of
   the upstream [`polst-android`](https://github.com/ajcpwnz/polst-android)
   SDK, kept here so the sandbox builds out of the box. Bug fixes and
   feature work should go upstream first; the mirror is refreshed
   periodically to track upstream releases.

6. **Minimal Compose integration snippet** — the canonical
   "render a polst" call inside a `<pre><code class="language-kotlin">`
   block. Copy-paste-able. The snippet shows:

   ```kotlin
   import androidx.activity.ComponentActivity
   import androidx.activity.compose.setContent
   import androidx.compose.foundation.layout.padding
   import androidx.compose.material3.Surface
   import androidx.compose.ui.Modifier
   import androidx.compose.ui.unit.dp
   import com.polst.sdk.PolstClient
   import com.polst.sdk.core.theme.PolstTheme
   import com.polst.sdk.core.theme.PolstThemeProvider
   import com.polst.sdk.core.theme.light
   import com.polst.sdk.ui.PolstView

   class MainActivity : ComponentActivity() {
     override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

       // One-time at app startup (typically Application.onCreate).
       PolstClient.installDefault(PolstClient.forContext(this))

       setContent {
         PolstThemeProvider(theme = PolstTheme.light) {
           Surface(modifier = Modifier.padding(24.dp)) {
             PolstView(shortId = "abc123XYZ_")
           }
         }
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
existing iOS page behaviour.

### No live data, no env switcher

- The page does NOT call `getApiOrigin()` / `getPolstTarget()` /
  `renderEmptyState()`.
- The page does NOT execute any HTTP request.
- The chrome bar does still show the env switcher (mounted by
  `bootstrap()`), since it's a global header — but switching env on
  this page just reloads with `?env=`, which has no functional effect
  on a static info page.

## Acceptance

- Page loads under `docs/android/index.html` and renders the chrome
  shell, header, "what you get" bullets, screenshot grid (with
  placeholder images), "try it" steps, vendored-mirror note, and the
  Compose integration snippet.
- The link to `polst-embedding/android/README.md` resolves
  (relative path: `../../android/README.md` from
  `docs/android/index.html`).
- The link to upstream
  [`polst-android`](https://github.com/ajcpwnz/polst-android)
  resolves.
- The Compose snippet is wrapped in a `<pre><code>` block and is
  copy-paste-able as Kotlin source.
- HTML parses cleanly (structural review).
- `node --check` passes for any `.js` touched (none expected for this
  page, so this gate is N/A).

## Out of scope

- Live SDK execution in the browser (impossible — the SDK is a native
  Android library).
- Real device screenshots — placeholder images ship now; real
  captures are a follow-up once the sandbox has a stable demo flow.
- Snippet copy buttons / syntax highlighting — those land in POL-780
  (POLISH-COPY) project-wide.
- Health banner on the chrome — POL-781.
- Cross-mode demos (iframe, script, sdk, ios, rn, api) — separate
  tickets.
- Backend changes — N/A for an info page.

## Dependencies

- POL-771 (FOUND-1 scaffold) — merged.
- POL-772 (FOUND-2 chrome with `bootstrap()`) — merged.
- POL-777 (DEMO-IOS) — sibling page; symmetrical structure but
  independently shipped.

## Assumptions

- The `polst-embedding` repo continues to vendor the
  [`polst-android`](https://github.com/ajcpwnz/polst-android) SDK at
  `android/sdk/` and ship a runnable sandbox at `android/example/`.
  Both already exist (see `android/README.md`,
  `android/example/build.gradle.kts`, and
  `android/sdk/src/main/kotlin/com/polst/sdk/PolstClient.kt`).
- The canonical Compose render API is `PolstView(shortId = ...)` —
  matches the existing sandbox code at
  `android/example/src/main/kotlin/com/polst/example/SinglePolstComposeActivity.kt`.
- The `PolstClient.installDefault(...)` / `PolstClient.forContext(...)`
  pattern is the recommended single-call boot path for a Compose host
  (matches the public API in
  `android/sdk/src/main/kotlin/com/polst/sdk/PolstClient.kt`).
- Modern evergreen browser baseline for the demo site, same as every
  other page.
