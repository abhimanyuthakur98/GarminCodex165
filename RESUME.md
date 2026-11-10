# Resume checkpoint — 10 November 2026

## User decision

Workout recording and Strava implementation is postponed until the user resumes it. Only planning/checkpoint documentation has been added. Do not start implementation merely because this file exists.

Read [the full blueprint](docs/workout-recording-strava-blueprint.md) before continuing. Start with the local in-app workout recorder, then add Strava integration. No backend, credentials, hosting or subscriptions have been provisioned.

## Known working baseline

- Branch: main; application baseline commit: a9075243ebf650179e14b05915c343f641447928 (already pushed).
- Pulse 165 v0.12.0, versionCode 13; Room schema v6; compile/target SDK 36, min SDK 26.
- Local signed debug APK: dist/Pulse165-0.12.0.apk. Signature verified.
- Last verified command: `./gradlew.bat assembleDebug testDebugUnitTest lintDebug` — successful; 66 tests passed, lint 0 errors / 42 warnings.
- Latest refinements: retained tab views/scroll positions, Back to Today, active-tab scroll-to-top, Today date reset, Journal date correction, dismissible sync feedback, goal keyboard spacing and workout filter reset.
- Existing Garmin Health Connect sync, BLE HR, FIT imports, Today/Trends/Fitness Age, journal and workouts must remain functional.
- `UI inspiration/` is user-owned untracked material. Preserve it; do not stage it incidentally.

## Safe restart sequence

1. Read this checkpoint and blueprint, then inspect current Git status/history. If changes occurred after this checkpoint, reconcile them; do not reset to the baseline or discard user work.
2. Confirm baseline build/tests before editing. Use a `codex/` feature branch for implementation and small, reviewable commits.
3. Inspect Workouts.kt, Database.kt, HealthRepository.kt, Feeds.kt, WorkoutUi.kt and AndroidManifest.xml. The blueprint describes proposed components, not existing functionality.
4. Implement recorder storage/state recovery first, followed by the recording UI, service and optional HR; add outdoor GPS and field tests before releasing the local recorder.
5. Add Strava only after local recording is stable and the backend/account prerequisites are resolved. Keep client secrets out of APK, source and logs.

## Data and regression constraints

- Add explicit tested Room migrations from v6; never use destructive migration or require reinstalling to update.
- Current `clearAllImports()` deletes every Workout. Separate source-specific import deletion from local recordings before saving phone workouts.
- Share the BLE owner; do not create competing watch connections.
- Preserve journal entries/check-ins, user goals, existing sync permissions and source provenance.
- Keep session HR/load separate from daily strain until overlap reconciliation prevents double-counting Garmin/BLE/Strava data. Never add workout calories blindly to all-day calories.
- Persist session checkpoints and pause intervals. No fabricated GPS/HR during gaps or silent recording restart after reboot.
- Strava import is read-only initially; uploading activities is a separate later opt-in feature.
- Verify migrations, interrupted sessions, duplicate imports, navigation and current regression tests. Perform real-device screen-off/GPS/BLE checks before claiming recorder reliability.
- Build and verify the APK with the existing signing identity, increment versionCode for an actual app release, and report any untested device behavior honestly.

## Suggested resume request

“Resume from RESUME.md and the workout-recording-Strava blueprint. Begin with the local workout recorder and preserve the existing app and data.”
