# Android demo screenshots

Static images used by `docs/android/index.html`. Each file ships
today as an 8x8 transparent PNG placeholder so the page layout works
out of the box. Replace each with a real capture from the
`android/example/` sandbox app when available.

## Files

| File | What it should depict | Suggested capture |
|---|---|---|
| `compose-single.png` | A single Polst rendered via the Compose `PolstView` composable in the sandbox app. | Run `:example:assembleDebug`, launch `SinglePolstComposeActivity`, screenshot the rendered widget on a phone-sized emulator (e.g. Pixel 7, light theme). |
| `xml-view.png` | The same Polst rendered via the legacy XML `View` surface. | Launch `SinglePolstXmlActivity`, same device + theme as `compose-single.png` so the two shots line up visually. |
| `offline-replay.png` | The offline replay queue flushing once connectivity returns. Will eventually be a GIF. | Cast a vote with the device in airplane mode, re-enable network, capture the moment the replay job fires (toast or event log). For now a static PNG of the offline-state UI is acceptable. |

## Format guidance

- PNG (or animated GIF for `offline-replay`) at roughly 16:10 aspect
  ratio so the `aspect-ratio: 16/10` rule on `.and-grid img` matches
  without cropping.
- Target ~720px wide; the grid scales down on narrow viewports.
- Light-theme captures render best against the current page chrome.
  If a dark-theme variant is added later, drop a `<picture>` block
  in `docs/android/index.html` to swap on `prefers-color-scheme`.

## Replacing a placeholder

Just drop the new file at the same path with the same name; no
markup change needed. Verify in a browser that the `<img>` no longer
shows the transparent placeholder.
