# iOS demo screenshots

Static images used by `docs/ios/index.html`. Each file ships today
as an 8x8 transparent PNG placeholder so the page layout works out
of the box. Replace each with a real capture from the
`ios/PolstSDKSandbox` app running on the iOS Simulator (or a real
device) when available.

## Files

| File | What it should depict | Suggested capture |
|---|---|---|
| `swiftui-single.png` | A single Polst rendered via the SwiftUI `PolstView` in the sandbox app. | In Xcode, select the `PolstSDKSandbox` scheme, pick an iPhone simulator (e.g. iPhone 15), `Cmd+R`, tap the **SwiftUI** row on the root menu, screenshot the rendered widget (light theme). |
| `uikit-view.png` | The same Polst rendered via the UIKit `PolstViewController` surface. | Tap the **UIKit** row on the root menu, same simulator + theme as `swiftui-single.png` so the two shots line up visually. |
| `offline-replay.png` | The offline replay queue flushing once connectivity returns. Will eventually be a GIF. | Toggle the simulator into airplane mode (Settings → Airplane Mode), cast a vote, re-enable the network, capture the moment the replay job fires. For now a static PNG of the offline-state UI is acceptable. |

## Format guidance

- PNG (or animated GIF for `offline-replay`) at roughly 16:10 aspect
  ratio so the `aspect-ratio: 16/10` rule on `.ios-grid img` matches
  without cropping.
- Target ~720px wide; the grid scales down on narrow viewports.
- Light-theme captures render best against the current page chrome.
  If a dark-theme variant is added later, drop a `<picture>` block
  in `docs/ios/index.html` to swap on `prefers-color-scheme`.
- Capture from the Simulator with `Cmd+S` (saves to `~/Desktop/`),
  or `xcrun simctl io booted screenshot <path>.png` from the CLI.

## Replacing a placeholder

Just drop the new file at the same path with the same name; no
markup change needed. Verify in a browser that the `<img>` no longer
shows the transparent placeholder.
