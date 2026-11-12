# Fieldnote

A study command center for Android (phones and tablets). Kotlin, Jetpack Compose, Room. Offline-first.

## Build the APK with GitHub Actions
1. Create a GitHub repo and push this folder to `main`.
2. Open the **Actions** tab, then the **Build APK** run.
3. Download the `Fieldnote-debug-apk` artifact, unzip it, and install `app-debug.apk`.

## Build locally
Open the folder in Android Studio (it generates the Gradle wrapper), or run `gradle assembleDebug` with Gradle 8.7 and JDK 17.

## What works (all local)
- Stopwatch and Pomodoro timer with subjects, pause/resume/break tracking, subject switching, session notes
- Foreground service keeps the timer running; state is timestamp-based and survives app restarts
- Planner: day/week/month, tasks with priority, planned minutes, daily repeat, move unfinished to tomorrow, D-Day countdowns
- Analytics computed from recorded sessions: day, week, month heatmap, trend, start/end regularity, streaks, CSV export
- Rankings: your subjects and personal records
- Library: notes with a stylus/finger sketch pad
- Focus protection: detects non-allowed apps during a session (Usage Access), alerts you, counts distractions, hold-to-exit
- Adaptive layout: sidebar on tablets, bottom navigation on phones

## Not included in this build
- Groups, shared/global rankings, accounts, sync: need a server (see docs/BACKEND.md). The Groups screen only checks that a server URL is reachable.
- Forced app blocking (needs accessibility or device-admin permissions), home-screen widgets, Wear OS, cloud backup.
- Room runs schema version 1 with no migrations yet; add migrations before changing entities.
