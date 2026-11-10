# Pulse 165 — real Garmin daily dashboard

Native Android / Kotlin / Compose companion for Garmin Forerunner 165. Version **0.11.0** brings a compact Fitness Age overview, interactive factor/history charts and habit suggestions. The app name and pulse icon are unchanged.

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
| Steps/calories | Health Connect aggregation restricted to Garmin, for each local calendar day. Missing values are `—`, not fabricated zeros. Active, total and basal energy are shown separately when supplied; only active energy fills the active goal ring. |
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
## 0.4.0: daily coaching

- **Today → Overview:** a morning/day briefing summarises that wake-up date's sleep, recovery and yesterday's activity. Text is generated locally from recorded values, not a remote AI service; it does not infer causes of recovery changes.
- **Today → Sleep:** a planner saves a 24-hour wake time and subtracts the configured sleep goal to suggest bedtime. The seven-night shortfall sums recorded nights below your goal, excluding missing nights and without offsetting short nights with longer ones. It is not a clinical debt calculation. No alarm or notification is scheduled.
- **Trends:** weekly averages compare the last seven completed calendar days against the previous seven. Coverage is shown separately for each metric, and changes are only displayed with seven recorded values in both weeks. Historical sync availability can limit the previous week's coverage.
- **Connect → Data status:** expand categories to distinguish missing Pulse read permissions, imported records and categories with no Garmin record on the selected date. The timestamp is the last successful import, not a guarantee that the watch has synced recently.
- The Fitness Age orb is larger with centred text. Particles emerge behind the values, move outward, fade at the rim and restart, while the outline gently morphs. No additional score meaning is attached to the animation.

## 0.5.0: explore and personalise

- **Overview → Activity timeline:** three aligned hourly lanes show the selected day's main sleep-session overlap, imported steps and mean HR chart values. Tap an hour or use the accessible slider. The view spans local midnight to midnight, including 23/25-hour DST days; the selected timestamp includes its UTC offset. Missing samples stay missing. The sleep lane includes awake intervals inside the main session; open the full sleep view for stages and the previous-night portion.
- **Trends → Personal baselines:** sleep duration and overnight HR display seven daily values against a rolling band calculated separately for each date. Bands are the interpolated 25th–75th percentiles of that date's previous 14 days, require seven recorded values and exclude the current date. These describe personal history, not clinical healthy ranges. Tap or slide to inspect a date.
- **Connect → Customise dashboard:** show/hide Overview cards, reorder them with up/down controls, select the start tab, or reset defaults. Settings save locally immediately and survive restarts. Start tab applies to a fresh launch; Android state restoration preserves the current tab. Hidden cards do not delete data or stop imports. The overview rings, date navigation and connection controls remain available.
- No additional Health Connect permissions or database migration are needed for this update.

## 0.6.0: workout library and check-ins

- Open **Workouts → Allow exercise**, grant the new Exercise read category, then sync. Pulse reads Garmin-origin exercise session records and associates HR samples and available active calories within each session window. Exercise permission is optional for all existing daily features. The app reports the imported workout count; availability must be checked on your phone and cannot be inferred from daily HR data.
- Filter by activity type and the last 7/30 days. Open a session for elapsed duration, HR chart, HR-reserve zones, available energy and comparison with the most recent earlier session of the same type and source. Pauses are included in elapsed time and zone analysis; long missing-HR gaps do not count. Health Connect energy can be apportioned from longer source intervals and is labelled accordingly.
- **FIT fallback:** export an activity's original file from Garmin Connect, unzip if necessary, transfer the `.fit` file to your phone and choose **Workouts → Import FIT**. Parsing uses [Garmin's official FIT Java SDK](https://github.com/garmin/fit-java-sdk), pinned to 21.176.0. Files are validated, decoded on-device and limited to 10 MB / 200,000 HR samples / 100 sessions. No file upload or broad storage permission is used. Only complete sessions with timestamps are imported; planned workouts, courses, ZIPs and corrupt files are rejected.
- Exact file reimports update existing sessions using a file hash and session index. Different exports and Health Connect copies remain separate, visibly labelled entries. Comparisons remain within one source; workout imports never add load to the daily scores. FIT does not backfill daily sleep/recovery/steps. FIT entries can be removed from session details.
- Effort (1–10) and soreness (0–10) check-ins are saved locally, editable and retained through session refreshes. Clearing imported data removes workouts and check-ins too; Garmin originals are untouched.
- Room schema 4 adds workout and check-in tables with an automatic 3→4 migration. Existing daily history and settings are preserved.
- Garmin FIT SDK license: [FIT Protocol License](https://github.com/garmin/fit-java-sdk/blob/main/LICENSE.txt). Exercise integration follows [Android's workout guidance](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts). GPS routes, distance and pace are not imported in this version.

## 0.7.0: journal and visual refresh

- The floating bottom dock uses five consistent vector-style line icons, compact single-line labels and animated selection colours. **Journal** is a persistent header action, not a crowded sixth tab; it can also be selected as the initial screen in dashboard customisation. The overview rings share a framed gradient panel, cards use coordinated lime/mint/lavender/orange accents, and the morning briefing expands on demand.
- **Journal → Check-in:** choose any past/current day; record energy, stress and soreness (1–5), last-caffeine timing, and whether a meal was within two hours of bedtime. Answers start blank. Tap a selected answer to clear it. Save before changing dates/tabs. Add up to 12 active custom habits, record explicit yes/no, archive or restore them. Editing replaces that date's entry; unrecorded habits are never assumed to be “no”.
- **Patterns:** uses the last 90 logged days paired with the following wake-up date's sleep duration and Pulse recovery. Each outcome needs at least five yes and five no paired records. Shows counts and an unadjusted difference in means, not a causal claim or personalised medical recommendation. Missing source data reduces sample counts. Archived habits retain historical patterns.
- **Calendar:** month navigation, recovery-tier colours, journal dots and workout diamonds. Tap a non-future day to edit it. Grey means no recovery score, not poor recovery.
- **Reminders:** off by default, optional notification permission on Android 13+, local HH:mm selection. WorkManager schedules the next local occurrence and then the following one; Android battery restrictions may delay delivery. No exact-alarm or health-background permission is required. Turn reminders off from Journal. Notifications contain no health values. Re-save the time after changing time zones to update an already scheduled reminder.
- Journal data is stored locally in Room schema 5 with an automatic 4→5 migration. **Clear journal & custom habits** removes it separately; clearing watch imports preserves journal entries. Uninstall removes all local records. No analytics, uploads or remote AI are involved.

## 0.8.0: visual insights, running details and home widget

- **Journal** now has a notebook icon, rounded tinted surface, visible border and a 48dp touch target in the header.
- **More visual summaries:** animated current/previous-week comparison bars; a tappable seven-night sleep chart with a goal line; 14-day import coverage tiles; elapsed/timer/moving-time bars; recorded lap-speed bars. Exact values remain visible, missing data is labelled, and bars within each comparison share a zero baseline. Lap detail text expands on demand.
- **Workouts → Kilometres / Miles** saves your distance preference. Reimport older FIT files to populate distance, speed, recorded laps, timer time and moving time. The identical file retains its identity and check-ins. Absent fields remain unavailable. These additions currently come from FIT, not the Health Connect exercise records.
- Tap or slide through the animated pace chart to see pace and the nearest HR within 60 seconds. Speed samples are averaged in time bins for display; gaps longer than one minute remain disconnected and zero speed has no pace. Average pace uses distance / timer time. Elapsed time includes pauses; moving time is shown only when explicitly supplied. Recorded laps can be partial or manual: selecting miles converts units, not lap boundaries.
- Previous-session comparisons require the same source and activity type and, when distance exists, a distance within 10%. No comparable session means an explicit empty state. Comparisons describe records, not proof of fitness improvement.
- **Home-screen widget:** long-press your Android home screen → Widgets → **Pulse Daily**, then add it. It displays today's imported steps/recovery, last import time, and a Journal shortcut. Tap the body to open Today. Updates follow successful imports/clearing and Android's approximately 30-minute widget refresh schedule; battery/launcher restrictions may delay updates. It reads the local cache and does not initiate a watch sync. Health values are visible on your home screen while the widget is installed. Remove it using your launcher to hide them.
- **Connect → Data status** shows 14 calendar days of data presence. Expanded diagnostics record the most recent successful import's sample/day counts and newest HR timestamp, separately from permissions. Filled coverage tiles mean some data exists, not full-day coverage. Missing categories require checking Garmin sharing and Pulse permissions; a recent import timestamp alone does not mean fresh watch data.
- Room schema 6 adds nullable FIT metrics and empty speed/lap histories via automatic 5→6 migration, preserving existing workouts, check-ins and journal entries. No extra health permissions are requested.
- Build: `./gradlew assembleDebug testDebugUnitTest lintDebug`. Install `app/build/outputs/apk/debug/app-debug.apk`. CI APK downloads remain under GitHub → Actions → Android build → Artifacts.

## 0.9.0: a calmer Today

- Today opens with a compact snapshot: one recovery summary, sleep and recorded strain, two activity-goal bars, and a recent HR chart average with its timestamp. No repeated briefing, timeline, setup instructions or Fitness Age promotion in the default feed. Missing metrics remain missing; an empty day gets one connection prompt.
- Tap any summary to open its existing chart/details. Steps and active calories still offer editable goals in their detail screens. **All details** opens the previous configurable feed, sleep planner and live-HR controls; **Back to snapshot** returns to the compact view. Dashboard visibility/order settings now apply to this detailed view, not the fixed snapshot.
- The existing Pulse icon replaces the text wordmark. The refresh icon has an accessible Sync health data label, disables during import and displays a progress indicator. The header retains a dated last-import indicator: imported data is not a live watch stream.
- **Journal** sits beside the date and opens the day currently being reviewed. Tap the date to reveal previous/next/today controls. Future dates remain disabled. Journal reminders and the home widget still open today's entry on a fresh launch.
- No new permissions, database changes or score changes. Existing imports, preferences, goals and journal entries remain intact.

## 0.10.0: focused Trends and calorie access

- **Trends:** choose one metric (sleep, recovery, strain, steps, active calories or overnight HR), then **History**, **Usual range**, or **Weekly**. History/range offer 7/14/30 calendar days with a tappable chart and accessible date slider. Open a selected day directly in Today. Only local records appear; requesting 30 days does not create or backfill unavailable data. Missing days stay gaps and today is labelled incomplete.
- Usual-range shading retains the preceding-14-day middle-50% calculation with seven prior values required. Weekly compares the last seven complete calendar days against the preceding seven, showing record counts rather than claiming incomplete weeks are equivalent. The old stacked reports no longer make Trends a long feed.
- **Calories → Fix:** separates missing Pulse read permission from a missing Garmin record. Buttons open Pulse permissions, Health Connect settings, Garmin Connect and a new Pulse import. Enable Garmin's **write** permission for **Active calories burned**, then sync the watch in Garmin Connect before importing in Pulse. If the category is absent, update Garmin Connect. The app cannot force Garmin to backfill historical records.
- [Garmin's Health Connect guide](https://support.garmin.com/en-US/?faq=JToBEy0jfe6pIygark2Ui5) lists active and total calories as supported. When only total energy arrives, Today labels it **Total calories**, hides the unavailable active-goal progress, and offers repair. Total calories never masquerade as active energy. This is better diagnosis and UI, not a guarantee that Garmin supplied your phone's missing records.
- Main tabs, metric views and detail screens use short 240ms fade/slide entrances; charts reveal over 650ms. Compose honours system animation duration scaling. No additional permissions or database migration.

## 0.11.0: visual Fitness Age and habit suggestions

- **Overview / Progress / Actions** replaces the long Fitness Age feed. Overview contains a fluid mint/ice-blue orb, a centred estimate, four tappable factor bars (0–100), and a suggested focus. Tap a factor to inspect its recorded average and one practical habit suggestion.
- The orb uses a softly morphing outline, luminous rim, outward-moving particles and a dark centre for readable text. **Pause motion** freezes the decorative drawing; animation never represents a hidden measurement. Tab/chart entrances follow the app's short motion style and system animation scaling.
- **Progress** shows 14 recomputed daily estimates with tap/slider selection. Each point uses only qualifying records before that date; missing history leaves gaps. Historical values use the currently entered chronological age, so editing it changes the chart. These are reconstructed estimates, not separately stored measurements.
- **Actions** chooses the weakest model component, except low current/average recovery takes priority. Suggestions focus on sustainable movement, regular sleep, comfortable activity and gradual progression. Low recovery never triggers advice to chase higher strain. No personalised suggestions appear until the age and required data are available.
- General habit guidance links to [CDC sleep guidance](https://www.cdc.gov/sleep/about/) and [NIDDK getting started with activity](https://www.niddk.nih.gov/health-information/weight-management/tips-get-active/tips-starting-physical-activity). These sources support general habits, not Pulse's illustrative age formula. Suggestions do not promise a reduction in age or reversal of aging.
- Existing formula, calibration gates and local records are unchanged. Fitness Age remains an unvalidated wellness illustration, not biological age, a lifespan prediction or Garmin/WHOOP's proprietary score. Model details remain expandable in Progress and Actions.
