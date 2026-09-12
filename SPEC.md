# CalmBlock

An app and website blocker for the Mudita Kompakt. One person, one phone.
Fully offline, local storage only, no accounts, no telemetry, no cloud.

Working name. Rename the folder and package if you want something else.

## What it does

- Block apps and websites, in groups called blocks.
- Unlimited blocks, unlimited apps and websites per block.
- Opening a blocked app shows a plain screen: the app name, "Blocked", and one
  short quote about time or attention from a poet or writer.
- Websites are blocked in DuckDuckGo, the browser you sideload.
- Blocks survive reboot, restart, and crashes. If anything goes wrong the phone
  ends up more blocked, never less.
- The app cannot be uninstalled, force-stopped, or disabled from the phone.
  Removing it takes a computer, a USB cable, and two adb commands.
- Screen time per app, a weekly report, and a plain-text export.

## Blocks

A block has a name, a list of apps, a list of website domains, and one mode.

| Mode | How it starts | When it ends | Can you edit it while active |
|---|---|---|---|
| Permanent | Tap Block permanently, confirm once | Never | Nothing can be taken out, ever. More can always be added. |
| Timed | Pick a duration, confirm | When the duration runs out | Nothing taken out until it ends. More can be added. |
| Scheduled | Set days and times, turn it on | End time each day | Nothing taken out while active or frozen. More can be added. |

**Permanent** is the main mode. Once confirmed, nothing in the block can be
taken out. No edit, no delete, no pause, no emergency unlock, no waiting period
that eventually opens it.

**Adding is always allowed.** Apps and websites can be added to any block at
any moment, active, frozen or not, because adding only ever takes access away.
Removing is what gets guarded. This is the same asymmetry the whole app runs
on: tightening is instant, loosening is not.

**But an addition that cannot be taken back has to be confirmed.** Adding to
any block that will not let the item out again takes over the whole screen
first, naming the app or website and saying plainly what is about to happen.
Cancel is the prominent button. If the block is permanent, the word BLOCK has
to be typed before the confirm button will work at all, so no mis-tap and no
half-awake thumb can reach it. Adding to a block that is merely running gets
the same screen without the typing, since that one ends by itself.

**Timed** starts a countdown for a duration you choose when you start it. While
it runs the block is frozen exactly like a permanent block. When it ends the
block is inactive and editable again. A running timer cannot be shortened or
cancelled. It can be extended.

**Scheduled** repeats on chosen days of the week between a start and an end
time. Outside the active window the block is editable, except during the freeze
window. The freeze window is a number of hours before start, set per block, and
can be zero. Once inside the freeze window the schedule, the app list, and the
website list are frozen until the block ends. Schedules can cross midnight.

**The permanent list** is a block that exists from first launch, is always
active, and cannot be deleted. Anything added to it is blocked from then on,
around the clock, forever.

## Blocked screen

Full screen, black text on white, no images.

```
Instagram

Blocked

"How we spend our days is, of course,
how we spend our lives."

Annie Dillard
```

One button: Back. It goes to the home screen. No unlock path from this screen.

Quotes come from a built-in list of short attributed lines, around forty,
rotated so the same one does not repeat twice in a row.

Sound is stopped too. A blocked music or podcast app plays from the
background, where the accessibility service never sees it, so the app takes
hold of its media session and pauses it, and clears its notifications so no
play button is left in the shade. This needs notification access, granted once
from the app's Settings tab.

How it gets there: the app's accessibility service sees every app that comes
to the front. When it is one in an active block, the blocked screen is put over
it straight away and Back goes home. Reopen the app and the same happens
again.

## Websites

Type a domain, tap Block. The domain and every subdomain is blocked in the
browser from that moment. Domains live in blocks like apps do and follow the
block's mode.

There is no VPN, proxy, or DNS change. Wi-Fi and mobile data are untouched.

The browser is DuckDuckGo. It has no managed blocklist of its own, so the
app's accessibility service reads the address bar and, when it shows a blocked
domain, backs out of the page and puts the blocked screen up.

The blocklist only covers that browser. Any other app on the phone that can
open web pages is treated as an app and blocked through the app's block, so a
second browser cannot be used as a way around it.

## Usage

Screen time per app, recorded from the system's own usage records.

- Today: list of apps by time spent, most first.
- This week: same, plus total, plus each day's total.
- Weekly report: generated every Monday morning for the previous week. Total
  time, top ten apps, change against the week before. Kept locally, viewable
  from the app.
- Export: one tap writes a CSV of daily per-app minutes to the phone's
  Documents folder. Columns: date, app, minutes.

## Screens

Words, not icons. Black on white. No animations, no greys below 30 percent
contrast, no gradients, no shadows. Large type.

1. **Blocks.** List of blocks. Each row: name, mode, status line such as
   "Active", "Starts 21:00", "Ends in 2h 10m", or "Permanent". Tap to open.
   Button: New block.
2. **Block.** Name, mode, apps, websites, and the mode's controls. Frozen
   blocks show everything read-only with one line explaining why, such as
   "Permanent. Cannot be changed." or "Frozen until 07:00."
3. **Usage.** Today and This week tabs, Weekly reports, Export.
4. **Settings.** Nothing to bypass. Shows setup state, the Android version, and
   which browser the blocklist is applied to.

## Reliability

- Enforcement is the accessibility service, which Android restarts on boot
  and keeps running while the app is installed.
- Uninstall protection is the Device Admin role. While it is active the
  Uninstall button in Settings is unavailable. The accessibility service keeps
  the user off the pages that could undo it: this app's info page, the device
  admin deactivation page, its own accessibility toggle, and the package
  installer's uninstall dialog. Land on any of them and the phone goes home.
- On boot, on every alarm, every fifteen minutes in the background, and after
  every edit, the app re-derives which blocks are active from stored
  timestamps and hands the accessibility service the current list. All paths
  call the same routine. If a block starts while its app is already open, the
  blocked screen appears at that moment.
- State is stored as timestamps and lists. Whether a block is active is always
  computed, never stored, so a bad write cannot unblock anything.
- If the system clock moves backwards, blocks that were active stay active.
- A permanent block is stored with a flag that the app refuses to clear. There
  is no code path that deletes a permanent block.

**Settings is closed once setup is done.** While any permission is still
missing, Settings stays reachable so it can be granted. The moment all four are
granted, any Settings page that so much as names this app sends the phone home,
the accessibility pages soonest of all, before their text has even drawn.

**The one gap.** Android guarantees that a user can always switch off an
accessibility service; a service is not allowed to prevent its own disabling.
Safe mode turns them all off outright, and the volume-key accessibility
shortcut can toggle one without going near Settings. Guarding the pages makes
the switch hard to reach, not impossible. Closing that would need the
Device Owner role, which Android will not grant on a phone that already has
accounts on it. Nothing else on the phone is affected by any of this: no
accounts, no data, no reset, no VPN.

## Setup, on the phone

Install the APK over adb, then open the app. The Settings tab has three
buttons, each opening the system page where the switch lives:

1. Turn on uninstall protection. Activates the Device Admin role.
2. Turn on blocking. Turns the accessibility service on.
3. Turn on usage access.

Then choose the browser. Nothing else on the phone is touched.

Removing it later, from the computer: deactivate the admin role over adb,
then uninstall. Both commands are in the runbook.

## Not included

Anything not listed above. In particular no delays, no unlock requests, no
cooldowns, no browser, no trip mode, no maintenance windows, no PIN, no
emergency access, no debug bypass, no VPN, no Device Owner, no reset.

## Build

Kotlin, Jetpack Compose, minSdk 31, targetSdk 35, same layout as CalmMoney.
Room for blocks and usage. No third-party libraries beyond AndroidX.
