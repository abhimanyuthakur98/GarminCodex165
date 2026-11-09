# Pulse 165 — GarminCodex165

A native Kotlin / Jetpack Compose MVP for a Forerunner 165 companion dashboard. Dark surfaces, neon score rings, live heart-rate chart, sleep breakdown, and offline daily history. Independent experimental scoring, not WHOOP's or Garmin's proprietary algorithms. No affiliation with either company.

## Build and install

Requires JDK 17, Android SDK platform 36 and build-tools 36.0.0. Android 8 (API 26) or later on the phone. Open this repository in Android Studio, or set `ANDROID_HOME` to your SDK directory.

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows: use `gradlew.bat` in place of `./gradlew`. Gradle 9.1.0 wrapper is included; AGP 9.0.1 uses built-in Kotlin 2.2.10. The documented `android.disallowKotlinSourceSets=false` compatibility switch supports the pinned KSP Room processor.

## Try the simulation

Launch **Pulse 165**, then tap **Simulation**. A deterministic, one-second sensor generator produces HR, RR intervals, steps, and acceleration. The recovery and sleep cards use an explicitly synthetic overnight fixture (including HRV baseline, RHR, sleep stages and Body Battery). Strain starts at zero on a new day and grows with recorded time. Tap **Stop** to pause; restart resumes the stored day load. Simulation runs while the app process is alive; it is not a background service.

**DEMO** is always labeled, and Room uses `(date, source)` keys to separate simulated, live, and imported history. Simulation does not populate live recovery/sleep cards.

## Forerunner 165 pairing / live testing

1. Keep normal Garmin Connect pairing if already configured. Enable Bluetooth on the phone.
2. On the watch, hold **UP**, open **Health & Wellness → Wrist Heart Rate → Broadcast Heart Rate**, and start broadcasting. Menu wording can vary with firmware; see Garmin's linked manual below. Merely pairing Garmin Connect does not enable standard HR broadcast.
3. In Pulse 165, tap **Connect watch**, grant Nearby devices permissions (location on Android 8–11), and select the watch from the discovered heart-rate sensors. On Android 8–11, location services may also need to be enabled for BLE scans.
4. Wait for **Connected • live heart rate** and the BPM/trace to update. Device addresses are shown so you can select your sensor explicitly. The app does not silently attach to the first nearby device.
5. Turn broadcasting off or leave range to test reconnect. The app retries with 2–30 second backoff while the foreground service is active. Samples older than ten seconds are not shown as current BPM; gaps over ten seconds do not accumulate strain.
6. Use **Stop** in the dashboard or service notification to disconnect. To return to synthetic data, tap **Simulation**.

A persistent connected-device foreground service keeps live capture and local persistence active with the screen off. It deliberately does not restart after force-stop/reboot. Android may still stop the process; restart from the UI. Android 13+ can hide the notification from the drawer when notifications are disallowed; tracking remains visible in Android's active-apps control.

If no watch appears: verify Broadcast Heart Rate is running, move closer, disconnect other HR receivers, check permissions/Bluetooth, then tap **Scan again**. Scans last 15 seconds. No watch hardware was available for this implementation's verification.

## What live integration does — and does not do

- Implemented: standard Bluetooth SIG HR service `0x180D`, measurement `0x2A37`, notification subscription, uint8/uint16 heart rate, optional RR intervals, contact checks, device selection, reconnect and cleanup.
- RR is only available if the broadcaster includes it. It is not assumed to be overnight HRV. Standard HR BLE does **not** expose sleep, Body Battery, accelerometer or all-day steps.
- Garmin Health Companion SDK provides richer streams subject to Garmin access/licensing and device support. Connect IQ's mobile SDK communicates with a companion Connect IQ app; it is not a generic Garmin Connect health database API. Neither proprietary SDK is bundled or impersonated here.
- `GarminSyncGateway` and `SyncImporter` provide a validated integration hook for normalized daily records from an approved Garmin Health API backend or FIT decoder. Implement `fetchDays()` in your adapter, then call `SyncImporter.sync(adapter)` from your authenticated sync job. Imported days show in history as `GARMIN_SYNC`. There is **no configured cloud backend, credential flow, scheduled remote sync, or binary FIT parser** in this MVP. Live recovery/sleep remain unavailable until that next integration is wired to the dashboard.
- Keep Garmin OAuth credentials and webhook verification on your backend. Do not scrape Garmin Connect or embed user passwords in the APK. A production FIT adapter should use Garmin's official SDK and preserve source/timezone/provenance.

## Architecture and models

Single Android module with independently replaceable components:

| Component | Responsibility |
| --- | --- |
| `Models.kt` | Pure scoring functions, sensor/night models, defensive HR packet decoder |
| `Feeds.kt` | SensorFeed port, deterministic mock and Bluetooth implementation |
| `Database.kt` | Room daily records, DAO, sync adapter/import boundary |
| `HealthRepository.kt` | Coroutine collection, daily rollover, StateFlow state, Koin graph, foreground service |
| `MainActivity.kt` | Lifecycle-aware MVVM collection and modular Compose cards/chart |

Room persists accumulated load, recovery, sleep duration, provenance and update time every ten samples and on clean stop. A sudden process kill can lose up to the last nine samples. History is limited to 28 recent records in the query and 14 in the UI; all days remain in the database. Backup is disabled; no analytics or network upload is included. Uninstall/clear app storage removes local health data. Raw streams and detailed sleep stages are not retained in this MVP.

**Strain:** HR reserve = `(HR − resting) / (maximum − resting)`, clamped 0–1. Zone weights for reserve `<.3, <.5, <.6, <.7, <.8, <.9, >=.9` are `0,1,2,3,5,8,12`. Sum weighted minutes, then `21 × (1 − exp(−load/180))`. Only recorded valid intervals count. The MVP uses an explicit reference profile of resting 55 / maximum 190 bpm; profile calibration/settings are future work. No Garmin training-load value is fabricated.

**Recovery:** 45% HRV component (`70 + 100 ln(overnight/baseline)`), 25% RHR component (`80 − 5 × drift`), 20% sleep efficiency, 10% Body Battery. Components clamp to 0–100. Red 0–33, yellow 34–66, green 67–100. Baselines must be provided; the demo fixture is not a learned personal baseline. Sleep performance = asleep / need, capped at 100%; consistency is a provided fixture/adapter input, not inferred from one night. These heuristic models are for exploration, not medical decisions.

## Download an APK from GitHub Actions

1. Open this repository's **Actions → Android APK** workflow.
2. Select a successful run for your commit.
3. Download **Pulse165-debug-<commit SHA>** under **Artifacts** (GitHub sign-in and repository access required).
4. Extract the ZIP, transfer `app-debug.apk` to your Android phone, and allow installation from that source.

Every push, pull request and manual dispatch builds an automatically **debug-signed**, installable APK, runs unit tests/lint and retains artifacts for 30 days. No release signing secrets are required. This is a testing APK, not a Play Store production release. CI-generated debug keys can differ between runners; uninstall an older CI build if Android rejects the update signature (this deletes its local data). Release publishing and stable production signing are intentionally not configured.

## Verification

Unit tests cover strain bounds/monotonicity, gap clamping, recovery response and tier boundaries, HR packet widths, RR units, truncation and contact loss. CI executes build, unit tests and Android lint. Physical watch pairing, firmware differences, background OEM restrictions and accessibility on a device require hardware acceptance testing.

## Official references

- [Garmin public repositories](https://github.com/garmin), including [Connect IQ examples](https://github.com/garmin/connectiq-apps) and [FIT SDK](https://github.com/garmin/fit-python-sdk)
- [Garmin Health SDK overview](https://developer.garmin.com/health-sdk/)
- [Garmin Health API and approval](https://developer.garmin.com/gc-developer-program/enterprise/health-api/)
- [Connect IQ mobile SDK for Android](https://developer.garmin.com/connect-iq/core-topics/mobile-sdk-for-android/)
- [Forerunner 165 HR broadcast manual](https://www8.garmin.com/manuals/webhelp/GUID-607F08F6-33FC-40BF-9727-84E54043D82D/EN-US/GUID-57A88A77-3813-4E79-9DB1-FC95B06F01BA.html)
- [AGP 9.0 compatibility](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
