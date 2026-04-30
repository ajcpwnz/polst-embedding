# Testing the SDK in Android Studio

The `:example` app is a self-contained sandbox: it boots an in-process mock backend on `http://127.0.0.1:8765` so every demo screen exercises the full SDK pipeline (REST → Polst rendering → vote submission → offline cache → vote replay queue) without needing a real backend or network access.

---

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later.
- **JDK 17** — verify with `java -version`. macOS Zulu 17 works (already installed in this dev env).
- **Android SDK Platform 34** + Build-Tools 34.x — installable from `Tools → SDK Manager` inside Android Studio.
- An emulator (Pixel 5 / API 34 recommended) or a USB-attached device with developer mode enabled.

---

## One-time bootstrap (Gradle wrapper)

The repo doesn't yet ship a Gradle wrapper. Run these commands once before opening Android Studio:

```bash
cd /Users/vasiliypivovarov/Desktop/Projects/PolstSDK-android-native-sdk

# Install gradle CLI if you don't already have it (macOS):
brew install gradle

# Generate the wrapper at the version this project targets:
gradle wrapper --gradle-version 8.9 --distribution-type all
```

This creates `gradlew`, `gradlew.bat`, and `gradle/wrapper/{gradle-wrapper.jar, gradle-wrapper.properties}`. Commit them.

After this, every subsequent build uses `./gradlew` — no global Gradle install needed.

---

## Open in Android Studio

1. **Android Studio → File → Open** → navigate to `/Users/vasiliypivovarov/Desktop/Projects/PolstSDK-android-native-sdk` → **Open**.
2. Wait for Gradle sync. First sync downloads:
   - Android Gradle Plugin 8.5.0
   - Kotlin 2.0.0 + Compose Compiler plugin
   - All dependencies declared in `gradle/libs.versions.toml`
   - Compose BOM 2024.08.00
3. If sync fails, the Gradle output panel will tell you exactly what's missing (usually an SDK platform or build-tools version). Install via SDK Manager and re-sync.

---

## Run the example app

1. In the run-config dropdown (top toolbar), select **example**.
2. Pick a target device — emulator or physical.
3. Click ▶ **Run**.

The app launches with **PolstSDK Demos** as the title and two buttons:

- **Single Polst (Compose) — fruit** → `SinglePolstComposeActivity` rendering `PolstView(shortId = "demo-fruit")` (the Compose composable).
- **Single Polst (XML) — coffee** → `SinglePolstXmlActivity` rendering `<PolstWidgetView app:polst_shortId="demo-coffee" />` (the View-system FrameLayout subclass).

---

## What happens behind the scenes

`PolstExampleApp.onCreate` (the `Application` class) does three things:

```kotlin
1. MockBackend.start()
   // Boots a com.sun.net.httpserver.HttpServer on 127.0.0.1:8765
   // Routes:
   //   GET  /api/rest/v1/polsts/demo-fruit  → canned PolstDto
   //   GET  /api/rest/v1/polsts/demo-coffee → canned PolstDto
   //   POST /api/rest/v1/polsts/{id}/votes  → updates an in-memory tally + returns VoteResponseDto

2. val client = PolstClient.forContext(
       context = this,
       environment = Environment.Custom(baseUrl = "http://127.0.0.1:8765/api/rest/v1"),
   )
   // forContext also wires up:
   //   - EncryptedSharedPreferencesTokenStore   (filesDir/polst/tokens.xml)
   //   - EncryptedDeviceIdProvider              (UUID v4 in same store)
   //   - OfflineCache                           (filesDir/polst/offline/*.json)
   //   - ReplayDatabase + ReplayDao             (filesDir/polst/replay.db, Room)
   //   - ReplayScheduler + DefaultConnectivityObserver  (auto-starts on init)

3. PolstClient.installDefault(client)
   // Globally installed; PolstWidgetView reads via PolstClient.default,
   // PolstView (Compose) reads via LocalPolstClient.
```

Cleartext traffic to `127.0.0.1` / `localhost` / `10.0.2.2` is allowed via `res/xml/network_security_config.xml`. The SDK's manifest sets `android:usesCleartextTraffic="false"`; the example overrides it with `tools:replace` (debug/demo only — production apps should keep the SDK's default and use HTTPS).

---

## Acceptance test walkthrough

### US1 (P1) — Single Polst embed renders + vote round-trip

1. Open **Single Polst (Compose) — fruit**.
2. **Expect**: the question "What's your favorite fruit?" renders with three options (Apple / Banana / Cherry) and seeded tallies (42 / 17 / 9).
3. Tap **Apple**.
4. **Expect**:
   - Brief "voting" state (button row disabled).
   - Apple's tally bumps to **43** (the mock server increments on POST).
   - Logcat tag `polst-example` shows `voted opt-apple` from `setOnVoteListener` (XML demo only logs; Compose uses an inline lambda).
   - Logcat tag `Polst-Replay` is silent (no replay needed because immediate delivery succeeded).

5. **Repeat for the XML demo** — open **Single Polst (XML) — coffee**, tap **Coffee** or **Tea**, see the tally bump and the logcat message.

### US2 (P1) — Vote cast offline survives + replays exactly-once

1. Open **Single Polst (Compose) — fruit** with the network reachable; let it load.
2. Press the **back** button to return to the launcher.
3. On the device/emulator: **enable Airplane Mode** (or `adb shell svc wifi disable && adb shell svc data disable`).
   - Note: the in-process mock backend on `127.0.0.1` is **always reachable** even with Airplane Mode (loopback isn't subject to airplane mode). To truly test offline, kill the mock by stopping the example process **OR** point the example at the emulator host alias `10.0.2.2:8765` and disable WiFi on the host machine. Easiest path for a quick test:
     - In `PolstExampleApp.kt`, temporarily change `MockBackend.BASE_URL` reference so the SDK points at `http://10.0.2.2:8765/api/rest/v1`. Then disable the host machine's WiFi to simulate offline.
   - **Or** for a pure offline-replay smoke test without changing config: stop `MockBackend` mid-session by adding a temporary "Stop mock" button to `MainActivity`.
4. Open **Single Polst (Compose) — fruit** again — the previously-cached Polst renders, with a "Showing cached results" badge below the options (this proves the offline cache fallback path is working — `OfflineCache.readPolst` succeeded after the network attempt failed).
5. Tap an option.
6. **Expect** an optimistic `Vote(state = Pending)` is returned. The `ReplayEntity` row is inserted into `polst-replay.db`. Verify with:
   ```bash
   adb shell run-as com.polst.example sqlite3 /data/data/com.polst.example/files/polst/replay.db \
     "SELECT idempotencyKey, endpoint, attemptCount FROM replay_entries"
   ```
7. **Restore connectivity** (disable Airplane Mode / restart the mock backend / re-enable host WiFi). Within ~30 s, `ReplayScheduler` enqueues `ReplayWorker` via `WorkManager`. Re-run the SQL above — the row should be **gone** (delivered + deleted).
8. Open the demo again — the tally now reflects the offline-cast vote, and `Polst-Replay` logcat is empty (queue is drained).

### US2 process-death drill (the hardest acceptance scenario)

1. Cast a vote while offline (steps 1–6 above).
2. **Force-stop the app**: `adb shell am force-stop com.polst.example` (or via Settings → Apps → Polst Example → Force Stop).
3. Re-open the app **with connectivity restored**.
4. **Expect**: within ~30 s, `WorkManager` re-discovers the queued worker on app boot, drains the entry, removes it from `polst-replay.db`. No user re-action needed. This is FR-054 + SC-005 working end-to-end.

---

## Inspecting on-disk state

```bash
# Encrypted token store + device ID:
adb shell run-as com.polst.example ls -la /data/data/com.polst.example/files/polst/

# Cached Polst payloads (flat JSON):
adb shell run-as com.polst.example cat /data/data/com.polst.example/files/polst/offline/_index.json
adb shell run-as com.polst.example cat /data/data/com.polst.example/files/polst/offline/polst_demo-fruit.json

# Replay queue (Room SQLite):
adb shell run-as com.polst.example sqlite3 /data/data/com.polst.example/files/polst/replay.db \
  ".schema replay_entries"
adb shell run-as com.polst.example sqlite3 /data/data/com.polst.example/files/polst/replay.db \
  "SELECT * FROM replay_entries"

# Image cache (when a Polst with media renders):
adb shell run-as com.polst.example ls -la /data/data/com.polst.example/cache/polst-images/
```

---

## Running the test suites

From the project root:

```bash
# Unit tests (JVM only — fast):
./gradlew :sdk:testDebugUnitTest

# Compose snapshot tests (Paparazzi, JVM only):
./gradlew :sdk:recordPaparazzi      # capture/refresh baselines
./gradlew :sdk:verifyPaparazzi      # verify against committed baselines (CI gate)

# Lint + static analysis:
./gradlew :sdk:ktlintCheck :sdk:detekt

# Public-API stability gate (binary-compatibility-validator):
./gradlew :sdk:apiCheck
./gradlew :sdk:apiDump              # refresh sdk/api/sdk.api after intentional API changes

# Instrumented tests (need an emulator / device):
./gradlew :sdk:connectedDebugAndroidTest

# AAR size budget gate (600 KB):
./gradlew :sdk:bundleReleaseAar
bash scripts/check-aar-size.sh
```

The CI workflow at `.github/workflows/ci.yml` runs all of the above on every PR, plus the instrumented matrix on API levels 26 / 30 / 34.

---

## Pointing at a real backend

When POL-556 ships a live REST API, replace the example app's `Environment.Custom(...)` with `Environment.Production` (or `Staging`):

```kotlin
// In PolstExampleApp.onCreate:
val client = PolstClient.forContext(
    context = this,
    environment = Environment.Production,
)
```

Remove `MockBackend.start()` (or guard it behind `BuildConfig.DEBUG`). Restore the SDK's default cleartext rejection by removing the `usesCleartextTraffic`/`networkSecurityConfig` overrides from the example's manifest. Replace `"demo-fruit"` / `"demo-coffee"` with real published Polst short IDs.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `./gradlew: No such file or directory` | Wrapper not bootstrapped | Run `gradle wrapper --gradle-version 8.9 --distribution-type all` once |
| Sync fails with "Could not find compose-compiler-gradle-plugin" | Missing Compose plugin coordinate | Verify `org.jetbrains.kotlin.plugin.compose` version `2.0.0` in `sdk/build.gradle.kts` |
| App crashes on launch with "PolstClient.installDefault was not called" | `PolstExampleApp` not registered | Check `AndroidManifest.xml` `<application android:name=".PolstExampleApp">` |
| `CLEARTEXT communication to 127.0.0.1 not permitted` | Network security config missing | Verify `res/xml/network_security_config.xml` exists + manifest references it |
| Polst renders "Set polst_shortId attribute" | XML attr not parsed | Check `app:polst_shortId="demo-coffee"` is set on `<PolstWidgetView>` and the `xmlns:app` namespace is declared on the root `<LinearLayout>` |
| Vote tap does nothing visually | Mock backend not running | Logcat: search for `PolstMockBackend` — should show `MockBackend listening on http://127.0.0.1:8765/api/rest/v1` at app start |
| Tally doesn't bump after vote | Mock backend started but `OfflineCache` returned stale | Clear app data (`adb shell pm clear com.polst.example`) and retry |
| Compose preview / Paparazzi tests fail with "Compose Compiler not applied" | Kotlin 2.0 needs the new plugin | Verify `id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"` in `sdk/build.gradle.kts` and `example/build.gradle.kts` |
