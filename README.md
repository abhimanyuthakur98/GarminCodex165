# Pulse 165 — real Garmin daily dashboard

Native Android / Kotlin / Compose companion for Garmin Forerunner 165. Version **0.3.0** adds interactive insights and personal activity goals on top of the working, on-device Health Connect import. The app name and pulse icon are unchanged.

## Install and connect (Android 14+, including Android 17)

1. Install the new APK over the previous locally built APK. The application ID and local debug signing key are unchanged; Room migrates existing live history without destructive recreation.
2. In Android **Settings → Health Connect → App permissions / Your Health Apps → Garmin Connect**, enable Garmin's **write** access for heart rate, sleep, steps and total calories. Menu labels vary by phone.
3. Open **Garmin Connect** and complete a watch sync. Health Connect receives data after a successful Garmin sync. Merely seeing Garmin listed does not mean every data category is enabled or populated.
4. Open **Pulse 165 → Connect → Connect Health Connect**. Allow read permissions, then tap **Import last 14 days**. Partial permissions are supported: other cards can populate independently.
5. Check the import status (number of HR samples and nights), then open **Today**, **Sleep**, **Strain**, or **Trends**. Use the date arrows to review prior days. Sleep belongs to the date you woke up.
6. Optional: **Enable background refresh** grants additional access and schedules WorkManager about hourly. Android controls the exact timing. Otherwise Pulse imports on app resume and manual Sync.

**You do not need to broadcast heart rate for the daily dashboard.** The data path is:

```text
Forerunner 165 → Garmin Connect sync → Android Health Connect → Pulse 165 → local Room history
```

The app reads only records attributed to Garmin Connect (`com.garmin.android.apps.connectmobile`). It does not scrape Garmin, access its private app database, request your Garmin password, or combine phone steps and duplicate provider streams. Garmin's direct Health Connect sharing requires Android 14+; standard BLE still works on Android 8+.

## What is real, what is estimated

| Card | Data and behavior |
| --- | --- |
| Daily HR | Actual imported samples; min/mean/max and a chart positioned by time of day. Five-minute chart bins reduce rendering cost. Missing intervals remain gaps. |
| Sleep | Main session, actual available stages and bedtime/wake time. Unknown stages stay unknown. Session duration alone is never asserted to be time asleep. Naps are excluded from the main sleep score. |
| Steps/calories | Health Connect aggregation restricted to Garmin, for each local calendar day. Missing values are `—`, not fabricated zeros. Calories are **total**, not active calories. |
| Strain | Independent 0–21 HR-reserve estimate. Linear interval approximation between valid samples up to 5 minutes apart; longer gaps add no load. Coverage is displayed. Resting/max HR are editable. BLE strain stays separate to avoid double counting the same workout. |
| Recovery | Explicit **estimate** from sleep duration (40%), sleep efficiency (20%) and overnight HR versus the prior 7 days (40%). Needs 3 previous baseline nights, current sleep stages, overnight HR and no more than 30 unknown sleep minutes. Missing inputs show calibration rather than a fake score. |
| HRV/resting HR | Optional record reads; shown only if Garmin actually exports them. Garmin's published sharing list does not include these or Body Battery. Pulse does not synthesize them from normal HR. |
| Consistency | Wake-time consistency over up to 7 nights; needs 3 observations and handles midnight wraparound. |

The sleep goal defaults to 8 hours; HR profile defaults to resting 55/max 190. Change them under **Connect → Adjust sleep & heart-rate profile**, using your established profile. This recalculates the refreshed 14-day window. Strain weights for HR reserve `<.3, <.5, <.6, <.7, <.8, <.9, >=.9` are `0,1,2,3,5,8,12`. Weighted minutes map to `21 × (1 − exp(−load/180))`. These are experimental fitness estimates, not WHOOP, Garmin Training Readiness or medical scores.

## If the import is empty

- Verify Garmin has **write** access and Pulse has **read** access; these are separate.
- In Health Connect, inspect **Data and access** for heart rate and sleep and check whether Garmin records exist.
- Sync the watch in Garmin Connect, wait for that sync to finish, return to Pulse and tap Sync.
- If sharing was enabled today, older history may not be backfilled by Garmin. Pulse can only read records that actually exist in Health Connect. New activity and the next completed night may be the first records.
- Read the Connect status; a failed import preserves the previous complete cache. Permission denial, unavailable provider and empty data have distinct messages. No cloud credentials or proprietary SDK approval are needed for this route.
- Recovery will remain in calibration until enough real baseline nights exist. Sleep, steps and HR do not wait for recovery calibration.

## Optional live HR

On the watch enable **Health & Wellness → Wrist Heart Rate → Broadcast Heart Rate** (firmware wording may vary). In Pulse choose **Connect → Connect live HR sensor**, allow Nearby devices, and select your sensor. View live BPM under **Today → Strain**. BLE retries on disconnect with backoff and uses a connected-device foreground service. Stop via the UI/notification. No broadcasting is required for historical imports.

## UI and offline history

Compact sleep/recovery/strain rings, date navigation, metric tabs, recovery context, sleep hypnogram, daily heart-rate chart, zone durations and Trends. Real data is the default. **Connect → Open synthetic design preview** is a clearly labeled, in-memory visual preview; it never writes to imported history. Exit the banner to return to actual data. The original mock sensor implementation remains available to developers through the SensorFeed boundary.

Imports page through all readable HR/sleep records, normalize duplicates, aggregate cumulative data by local calendar day (including DST), and atomically replace the last 14 days. This reflects source corrections/deletions in that window. Older imported summaries remain for offline trends; use Clear to remove them. Room v1→v2→v3 migration preserves earlier BLE/demo daily records in a separate table.

## Privacy

Read-only Health Connect access, optional separate background permission, no remote upload, ads or analytics. Imported summaries and chart samples stay in Room on the phone, excluded from backup. The in-app permission rationale explains each use. **Connect → Clear imported data & disconnect** revokes access, cancels scheduled work and clears imported history; Garmin originals and legacy BLE rows are unchanged. Uninstall removes all local app data. Permission revocation alone stops future reads but does not erase the cache.

## Build and test

JDK 17; Android SDK 36 and build tools 36.0.0; Gradle wrapper 9.1.0; AGP 9.0.1 / built-in Kotlin 2.2.10. Set `ANDROID_HOME` or open in Android Studio.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows: `gradlew.bat`. The KSP compatibility setting `android.disallowKotlinSourceSets=false` is required by the pinned processor. Health Connect SDK 1.1.0, Room, WorkManager, Koin and lifecycle-aware StateFlow provide data, scheduling, storage, DI and MVVM.

Key modules: `HealthConnectSync.kt` (permission-aware import and worker), `DailyAnalysis.kt` (pure analysis), `Database.kt` (storage/migration), `DashboardUi.kt` (Compose), `Feeds.kt` (optional BLE/mock), `HealthRepository.kt` (live-session state).

Tests cover sparse HR/gaps, duplicates, unknown/overlapping stages, midnight sleep, baseline requirements, recovery response, consistency, packet decoding, Room upgrade and Compose navigation/rendering. UI test screenshots are written under `app/build/screenshots`. Physical Garmin export/permissions must still be exercised on the user's phone; synthetic rendering tests cannot verify their personal data flow.

## APK downloads

Open **Actions → Android APK → successful run → Artifacts → Pulse165-debug-<SHA>**. Extract the ZIP and install `app-debug.apk`. Every push builds, tests and lints an installable **debug-signed** APK. Artifacts expire after 30 days. CI debug keys may differ from local builds or prior CI runs; use the local upgrade APK to preserve compatibility with the original locally supplied install. Production release signing/Play publishing is not configured.

## Official references

- [Garmin → Health Connect sharing and data types](https://support.garmin.com/lv-LV/?faq=JToBEy0jfe6pIygark2Ui5)
- [Health Connect setup and permission rationale](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Read records, pagination and background access](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Garmin public SDKs/examples](https://github.com/garmin)
- [Garmin Health SDK (alternative approved integration)](https://developer.garmin.com/health-sdk/)

## 0.3.0: interactive insights and refreshed design

Install `Pulse165-0.3.0.apk` over the existing local build. Your imported days are preserved. After upgrading, go to **Connect → Review read permissions**, allow **Active calories burned** and optionally **Basal metabolic rate**, then sync again. No new Bluetooth pairing is needed. If Garmin does not supply a category, it remains unavailable.

- **Interactive charts:** tap recovery, sleep, strain or daily HR to open a full-screen breakdown. The HR detail supports point selection by tap or an accessible slider. Sleep details list actual stage intervals. Android Back or the visible Back button closes the screen.
- **Steps:** lime ring, custom daily goal, hourly bars, peak hour and selectable hour readouts. Tap the card for details or its **Edit** control to change the goal. Goal validation: 1,000–100,000 steps.
- **Calories:** orange ring tracks **active calories** against your target (100–5,000 kcal). Detail displays active, total, reported basal energy and derived resting/other separately. Resting/other is `total − active` only when both are present and nonnegative; it is **not measured BMR**. No fabricated active/BMR values or metabolic equations are used. Basal aggregation covers the imported day-to-date interval, not an assumed full-day rate.
- **Hourly data:** Health Connect duration aggregation restricted to Garmin. Missing hours retain empty slots; zero and missing are distinct. DST days can contain 23 or 25 hours; selected-hour labels include UTC offset. Hourly allocation can be apportioned from longer source intervals by Health Connect; it does not imply per-minute sensor precision. If the watch only exports coarse data, the app cannot recover more exact timing.
- **Fitness Age:** new tab and overview entry. Enter chronological age (18–100) locally. This is an explicitly **unvalidated wellness illustration**, not biological age, life expectancy, Garmin Fitness Age or WHOOP Age. It requires 7 complete days in the preceding 14, excluding today; each needs steps, sleep, recovery and ≥120 minutes HR coverage. Four equally weighted factors use fixed references: 8,000 steps/day, 480 sleep minutes/night, Pulse recovery percentage, and 30 minutes/day above 50% HR reserve. The mean index maps to entered age with `(70 − index) / 6`, capped at ±5 years and minimum age 18. Personal goal changes do not alter these fixed references. No pace-of-aging claim is made.
- **Visual refresh:** near-black backgrounds, subtle gradient cards, luminous lime/orange/mint accents, compact week strip, goal rings and a deterministic particle orb built directly in Compose Canvas. References inspire the style; app name/icon remain unchanged.

Goals persist in private on-device preferences. They apply globally to the viewed day; there is no dated goal history. Data imports migrate additively to Room v3 with nullable new calorie fields and empty hourly history until the next sync. Clearing imported health data leaves your personal goal preferences; uninstalling clears both.

Tests additionally cover goal validation/persistence, interactive goal editing and chart navigation, null/zero hourly encoding, missing-hour and DST preservation, calorie consistency, age-calibration bounds and exclusion of duplicate/current-day records, and preservation of v2 imported days during migration.

## 0.3.1: motion and compact-screen polish

- Goal rings use square geometry and auto-sized values so five- and six-digit step counts stay inside the ring. Paired cards keep equal heights.
- Rings, sleep timelines, HR lines, hourly bars, strain zones, trends and Fitness Age factors animate on entering their view. Animations use Compose duration scaling (including system-disabled animations).
- Fitness Age has gently morphing edges and drifting particles; the animation is decorative and does not encode additional health measurements.
- The calorie card now displays imported total calories separately and offers **Check access** when active energy is missing. Details show a clearly labelled total-energy chart when only total energy is available. Total energy never fills the active goal ring. Active/BMR data cannot be recovered if Garmin has not shared those record types; allow the category in both apps and sync again.
- Verification includes a 320dp ring layout with 10,911 steps, missing active energy with available total energy, permission-button routing, and deterministic entrance-animation timing.