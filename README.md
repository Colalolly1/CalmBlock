# CalmBlock

App and website blocker for the Mudita Kompakt. `SPEC.md` says what it does,
`RUNBOOK.md` how to put it on the phone.

```bash
cd /Users/simon/code/CalmBlock && JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleRelease testDebugUnitTest
```

## Layout

| | |
|---|---|
| `core/` | Block model, the rules that derive status from time, domain matching, quotes. Pure Kotlin, unit tested. |
| `data/` | Room tables, the few scalar prefs, `Repo` (every write the UI can make, each guarded by the rules), CSV export. |
| `admin/` | The Device Admin receiver and `PolicyEngine`, which knows which packages are protected, web-capable, or launchable. |
| `engine/` | `Reconciler` (the one routine every trigger calls), alarms, boot and time receivers, usage rollup, the never-backwards clock. |
| `access/` | `WatchService`, the enforcement layer: covers blocked apps, reads the browser's address bar, guards the Settings pages that could remove the app. |
| `ui/` | Compose screens. Words, not icons. |
