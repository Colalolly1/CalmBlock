# CalmBlock runbook

Phone setup, once. Nothing on the phone is reset, removed, or signed out.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleRelease testDebugUnitTest
```

Output: `app/build/outputs/apk/release/app-release.apk`. Without a
`keystore.properties` it is signed with the debug key, which is fine for one
phone. Keep the same key: an update signed with a different key will not
install over the old one.

## Install

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

## Turn things on, from the app's Settings tab

1. **Turn on uninstall protection.** Android shows its activation page for
   CalmBlock. Tap Activate.
2. **Turn on blocking.** Opens Accessibility settings. Find CalmBlock and turn
   it on. From this moment the app guards its own Settings pages.
3. **Turn on usage access.** Opens the usage access list. Find CalmBlock and
   allow it. Or from the Mac:

```bash
adb shell appops set com.calmblock.app android:get_usage_stats allow
```

4. **Choose the browser.** DuckDuckGo, once it is installed.

Order matters slightly: do admin before accessibility, because once the
service is on it sends you home from the admin page.

## Verify

- Add an app to a timed block, start it, open the app: blocked screen with a
  quote, Back goes home.
- Start a timed block while its app is already open: blocked screen appears.
- Add a domain, open it in DuckDuckGo: blocked screen, then home.
- Reboot mid-timer: still blocked afterwards.
- Settings, Apps, CalmBlock: sent home before the page settles.
- Settings, Security, Device admin apps, CalmBlock: sent home.
- Long-press CalmBlock on the launcher, Uninstall: sent home.
- Set the clock back an hour: timer does not end early.

## Updating the app later

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Same key, same package. Blocks, usage history, and the admin role survive.

## Removing it, from the Mac only

```bash
adb shell dpm remove-active-admin com.calmblock.app/com.calmblock.app.admin.BlockAdminReceiver
```

```bash
adb uninstall com.calmblock.app
```
