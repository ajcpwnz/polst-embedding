# polst-embedding

Live, copy-paste-ready demos for every way to embed a Polst — iframe,
script tag, JS SDK, REST, iOS, Android, React Native.

## Live demo

**[https://ajcpwnz.github.io/polst-embedding/](https://ajcpwnz.github.io/polst-embedding/)**

Each mode below has its own live page on that site. Open one, "view
source," and you have your integration snippet. Add `?polst=<share-url>`
to point any demo at your own polst.

## Pick your integration mode

Every snippet below is a working starter. Replace `<shortId>` with a
real 10-character polst id (or paste a polst share URL into the
`?polst=` query param on the live demo to see the same snippet
materialised against your data).

### iframe

Drop one tag on any HTML page. Zero JS required.

- Live: <https://ajcpwnz.github.io/polst-embedding/iframe/>
- Source: [`docs/iframe/index.html`](docs/iframe/index.html)

```html
<iframe src="https://staging.polst.app/embed/polst/<shortId>"
        width="100%" height="600"
        style="border:0; max-width:560px;"></iframe>
```

### script tag

One script line plus a marker `<div>`. The widget hydrates inside its
own Shadow DOM — no CSS leakage either way.

- Live: <https://ajcpwnz.github.io/polst-embedding/script/>
- Source: [`docs/script/index.html`](docs/script/index.html)

```html
<script src="https://unpkg.com/@polst-web/widget@latest/dist/widget.esm.js"
        type="module" async></script>
<div data-polst="<shortId>"></div>
```

Swap `data-polst` for `data-polst-campaign` or `data-polst-brand` to
mount a campaign or brand feed.

### JS SDK

Programmatic mount via `@polst-web/sdk`. Use this when you want to
control fetch lifecycle, render only when in viewport, or react to
vote events in your own code.

- Live: <https://ajcpwnz.github.io/polst-embedding/sdk/>
- Source: [`docs/sdk/index.html`](docs/sdk/index.html)

```js
import { PolstClient } from "https://esm.sh/@polst-web/sdk";
import { renderPolst } from "https://esm.sh/@polst-web/sdk/render";

const client = new PolstClient({ baseUrl: "https://staging-api.polst.app" });
const polst = await client.getPolst("<shortId>");
renderPolst(document.querySelector("#mount"), { polstId: polst.id });
```

### REST API

No SDK at all. Raw `fetch()` against the public REST surface — useful
for server-side rendering, mobile native, or when you can't ship JS.

- Live: <https://ajcpwnz.github.io/polst-embedding/api/>
- Source: [`docs/api/index.html`](docs/api/index.html)

```js
const res = await fetch(
  "https://staging-api.polst.app/api/rest/v1/polsts/<shortId>",
  { headers: { Accept: "application/json" } }
);
const polst = await res.json();
```

Full reference: <https://staging-api.polst.app/api/rest/v1/docs>.

### iOS

Native SDK via Swift Package Manager. The sandbox app under `ios/`
exercises the SDK end-to-end against any env.

- Live: <https://ajcpwnz.github.io/polst-embedding/ios/>
- Source: [`docs/ios/index.html`](docs/ios/index.html)

```swift
// Package.swift
.package(url: "git@github.com:ajcpwnz/polst-ios.git", from: "0.1.0")
```

Run instructions for the sandbox: [`ios/README.md`](ios/README.md).

### Android

Native SDK as a Gradle module. The sandbox under `android/` includes
a Compose example app and the SDK module side-by-side.

- Live: <https://ajcpwnz.github.io/polst-embedding/android/>
- Source: [`docs/android/index.html`](docs/android/index.html)

```sh
cd android && ./gradlew :example:assembleDebug
```

Full instructions: [`android/README.md`](android/README.md).

### React Native

The RN demo page is currently a stub — the recommended RN integration
today is to bridge the native iOS / Android SDKs through your existing
RN bridge layer, or to render the JS SDK in a `WebView`.

- Live: <https://ajcpwnz.github.io/polst-embedding/rn/>
- Source: [`docs/rn/index.html`](docs/rn/index.html)

## Native SDKs

The `ios/` and `android/` directories at the repo root are fully
runnable sandbox apps, kept up to date with the upstream SDKs. They
are the source of truth for native run / build instructions:

- iOS: [`ios/README.md`](ios/README.md) — SwiftPM, Xcode scheme,
  simulator run, command-line build.
- Android: [`android/README.md`](android/README.md) — Gradle, Android
  Studio, debug APK.

The top-level README intentionally does not duplicate those
instructions; treat the per-platform READMEs as canonical.

## How this site works

Every demo page accepts two query params: `?polst=<url-or-shortId>`
chooses the polst to render (any polst share URL or a raw 10-character
nanoid id), and `?env=<canary|staging|prod>` chooses the backend env
(defaults to the env inferred from the polst URL host, falling back to
staging). Combine them to point any demo at any polst against any env
without editing code — the chrome bar at the top of each page also
exposes both controls live.

## License

There is no formal `LICENSE` file at the repo root yet — MIT or
Apache-2.0 is forthcoming. In the meantime, integrators may freely
copy any HTML, CSS, or JS in `docs/` into their own integrations;
that's exactly what those pages exist for.

The native sandbox apps follow per-platform license notes:
[`android/LICENSE`](android/LICENSE) covers the Android sandbox, and
the iOS sandbox follows the upstream `polst-ios` SDK's license (see
[`ios/README.md`](ios/README.md)).

## Source

- Live site: <https://ajcpwnz.github.io/polst-embedding/>
- Repo: <https://github.com/ajcpwnz/polst-embedding>
- Issues: <https://github.com/ajcpwnz/polst-embedding/issues>
