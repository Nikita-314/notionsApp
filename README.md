# NotionsApp

NotionsApp is a Kotlin/Android recreation of the original Flutter `notion_app` project.

The current Kotlin version focuses on the local-first journal/planner core:

- day pages with tasks, notes, completion state, and time buckets;
- books for grouping entries;
- checklist items inside entries;
- date-time reminder data;
- calendar overview with per-day entry counts;
- user display preferences;
- encrypted JSON persistence through Android Keystore.

The original Dart app also contains deeper platform integrations such as native notification orchestration, Wi-Fi reminders, geofence reminders, Yandex map picking, and generated Drift database tables. Those concepts are represented in the Kotlin domain model and can now be implemented incrementally on top of this native Android foundation.

## Build

```bash
./gradlew :app:assembleDebug
```

The debug APK is produced under `app/build/outputs/apk/debug/`.
