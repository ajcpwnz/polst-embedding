# polst-embedding

Sandbox apps for embedding the Polst SDKs.

- `ios/` contains the iOS sandbox app.
- `android/` contains the Android sandbox app.

## Run the iOS app

Requirements:

- Xcode with iOS Simulator support.
- SSH access to GitHub, because the app depends on `git@github.com:ajcpwnz/polst-ios.git`.

Clone and open the project:

```sh
git clone git@github.com:ajcpwnz/polst-embedding.git
cd polst-embedding
open ios/PolstSDKSandbox.xcodeproj
```

In Xcode:

1. Select the `PolstSDKSandbox` scheme.
2. Choose an iOS Simulator.
3. Press `Cmd+R` to build and run.

If Swift Package dependencies do not resolve automatically, run:

```sh
xcodebuild -resolvePackageDependencies -project ios/PolstSDKSandbox.xcodeproj
```

To verify the app from the command line:

```sh
xcodebuild \
  -project ios/PolstSDKSandbox.xcodeproj \
  -scheme PolstSDKSandbox \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build
```
