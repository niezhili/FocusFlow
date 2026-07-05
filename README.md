# FocusFlow

A minimalist Android focus timer app inspired by the Pomodoro Technique. Built with Jetpack Compose + Material3.

## Features

- **Task Management** — Create, track, and organize focus sessions per task
- **Focus Timer** — Start/stop/pause timer with real-time notification in the status bar
- **Sound & Vibration** — Customizable alert sounds (ding/bell/chime) and vibration feedback
- **Statistics** — Daily, weekly, and monthly charts showing focus time and session distribution
- **Persistent Counting** — Timer continues running in foreground service even when app is in the background

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + Repository) |
| Database | Room + RxJava3 |
| Background Service | Foreground Service + WakeLock |
| Preferences | Jetpack DataStore (Rx) |
| Charts | Custom Canvas drawing |

## Requirements

- Android 8.0+ (API 26)
- Gradle 8.x

## Getting Started

1. Open the project in **Android Studio**
2. Sync Gradle (auto-detected)
3. Run on a device or emulator

```bash
./gradlew assembleDebug
```

## License

MIT
