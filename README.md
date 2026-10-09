<p align="center">
  <img src="branding/logo.svg" width="160" alt="Nagmo logo" />
</p>

<h1 align="center">Nagmo</h1>
<p align="center"><b>Personalised nagging memo.</b><br/><i>We nag so you don't have to.</i></p>

<p align="center">
  <img src="branding/mascot_happy.svg" width="72" alt="Nagmo happy" />
  <img src="branding/mascot_nagging.svg" width="72" alt="Nagmo nagging" />
  <img src="branding/mascot_sleepy.svg" width="72" alt="Nagmo sleepy" />
  <img src="branding/mascot_party.svg" width="72" alt="Nagmo celebrating" />
</p>

Nagmo is a calm, minimalist Android reminder app with a little sticky-note
mascot. It rings like an alarm and keeps nagging until your work is done.

## Design

* Clean neutral surfaces with hairline borders and one accent colour.
* **Light, dark or system** theme, and a choice of **accent**: Honey, Coral,
  Rose, Lavender, Ocean, Sage, Graphite, or your wallpaper colours (Android 12+).
* The mascot is drawn in your accent colour and changes mood with your progress.
* Swipe a nag right to complete it, left to delete it (with undo).

## Features

**The basics**

| | |
|---|---|
| 📝 **Work** | What needs doing |
| ⏰ **Remind me at** | Rings like an alarm (alarm sound, full screen over the lock screen, wakes the screen) |
| 🏁 **Must be finished by** | A deadline, with a warning before it's due and an *overdue* badge after |
| 🗒️ **Specifics** | Free-form notes plus a checklist of steps |

**Widgets & lock screen**

* **Nag list widget** (resizable): a clean list of what's next. Tick nags off
  from the widget, tap one to open it, or use the **+** button to add work.
* **Quick nag widget** (2×2): the mascot, your next nag and a **+** button.
* Widgets follow the system light/dark mode and use your accent colour.
* Both widgets are declared for **home screen and lock screen** (`keyguard`).
  Most phones since Android 5 don't allow lock-screen widgets, so Nagmo also has:
  * a **lock-screen list**: a quiet, persistent notification showing today's
    nags in full on the lock screen, with an **Add nag** button (can be turned off);
  * a **Quick Settings tile** (*Add nag*) that you can reach from the lock screen.
* Settings has buttons that place each widget on your home screen for you.

**More features**

* 📢 **Persistent nagging**: re-nag every 5/10/15/30/60 min until it's marked done.
* 🗣️ **Nagging personalities**: Gentle, Sassy (the default) or Drill sergeant.
  Re-nags get more insistent ("Nag #3. I can do this all day.").
* 😴 **Snooze, Done and Stop nagging** buttons on the alarm screen and notification.
* 🔁 **Repeat**: daily, weekdays, weekly or monthly. Ticking one off moves it to
  the next time it's due.
* ⚠️ **Deadline warnings**: 15 min / 1 h / 3 h / 1 day before (your choice).
* ☀️ **Morning digest**: a daily rundown at a time you pick.
* 🔥 **Priority**, 🎨 **colour labels**, 🏷️ **categories** and 📌 **pinning**.
* 🔎 Search, plus filters for *To do / Today / Upcoming / Overdue / Done*.
* 🏆 **Stats**: streaks, done today/this week, on-time rate and a 7-day chart.
* ↩️ Undo after ticking something off.
* 📤 **Share to Nagmo**: share text from any app to turn it into a nag.
* 🚀 Launcher shortcuts (long-press the icon): *New nag* and *Today*.
* Themed (monochrome) icon, and alarms are restored after a reboot or time-zone
  change.

## Building

Requirements: JDK 17+ and the Android SDK (API 35). Open the project in
Android Studio, or:

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Download

Every push to `main` builds the app and publishes the APK on the
[**Releases**](../../releases/latest) page. Download `Nagmo-1.0.N.apk` and
open it on your phone. New versions install over old ones and keep your nags.

Release APKs are signed with `keystore/nagmo-release.jks`. That key is committed
to the repo so updates always install cleanly, which also means it is **not
secret**. Before publishing to a store, use your own key by setting
`NAGMO_KEYSTORE`, `NAGMO_KEYSTORE_PASSWORD`, `NAGMO_KEY_ALIAS` and
`NAGMO_KEY_PASSWORD`.

### Permissions

| Permission | Why |
|---|---|
| Notifications | To ring and show your list |
| Alarms & reminders (`USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM`) | To ring at the exact minute |
| Full-screen intent | To show the alarm over the lock screen |
| Run at startup | To restore alarms after a reboot |

Settings shows a reminder for any permission that's missing.

## Project layout

```
app/src/main/java/com/nagmo/app/
├── data/      Nag model, JSON-file repository, settings
├── alarm/     AlarmManager scheduling, receivers, notifications, nag messages
├── ui/        Compose screens (home, editor, stats, settings), alarm screen
├── widget/    Nag list + Quick nag widgets
└── tile/      Quick Settings tile
branding/      Mascot & logo SVGs and the script that generates them
```

### Mascot art

The mascot and logo are drawn in code. `branding/generate_art.py` writes the
SVGs in `branding/`, the Android vector drawables (`mascot_*.xml` and the
launcher icon layers) and `MascotArt.kt`, which the app uses to draw the mascot
in the chosen accent colour. If you change the art, run:

```bash
python3 branding/generate_art.py
```
