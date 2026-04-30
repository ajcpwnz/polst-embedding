# Polst Android Sandbox

This is a standalone Android sandbox for embedding the Polst SDK.

Open the `android/` folder in Android Studio, or build the debug APK from this directory:

```sh
./gradlew :example:assembleDebug
```

The `example` app exercises both Compose and XML rendering surfaces. The `sdk` module is included locally so the sandbox can build directly from source.
