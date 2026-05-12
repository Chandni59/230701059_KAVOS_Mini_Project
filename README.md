# KAVOS

A personal safety Android app that lets users discreetly send SOS alerts with their location to trusted contacts.

## Features

- **Disguised launcher** — opens as a working calculator; entering a secret code reveals the safety app
- **Multiple SOS triggers**
  - Manual long-press on the SOS button
  - Shake detection (3 shakes above 1.8g within 2 seconds)
  - Voice trigger ("help me" detected via speech recognition)
  - Dead Man Switch (5-minute inactivity timer)
  - Low battery auto-trigger
  - Check-in timer expiry
- **Emergency contacts** — add guardians with custom SOS messages
- **Location sharing** — every SOS sends a Google Maps link via SMS
- **SOS history log** — local record of every alert triggered
- **AI safety companion ("Saheli")** — in-app advisor for safety guidance
- **5-second cancel window** on most triggers to prevent false alarms

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0) | **Target SDK:** 36
- **Local DB:** Room 2.6.1 (with KSP)
- **Cloud:** Firebase Auth + Firestore
- **UI:** XML layouts with View binding
- **Build:** Gradle Kotlin DSL

## Project Structure

```
app/src/main/java/com/chandni/kavos/
├── SplashActivity        # Entry point — routes to Auth or Main
├── AuthActivity          # Email/password sign-in
├── MainActivity          # SOS button, shake & dead-man-switch listeners
├── DisguiseActivity      # Calculator disguise
├── VoiceSOSActivity      # Voice-trigger listener
├── CheckInTimerActivity  # Periodic check-in timer
├── SOSHelper             # Central SOS dispatch (SMS + location + log)
├── BatteryReceiver       # Low-battery broadcast receiver
└── db/                   # Room entities, DAOs, AppDatabase
```

## Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/Chandni59/230701059_KAVOS_Mini_Project.git
   ```

2. **Firebase config** — the included `app/google-services.json` is bound to the original Firebase project. To use your own backend, replace it with your own from the [Firebase Console](https://console.firebase.google.com/).

3. **Open in Android Studio** and let Gradle sync.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

On Windows use `gradlew.bat` in place of `./gradlew`.

## Required Permissions

- `SEND_SMS` — to send SOS messages
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — to attach location
- `RECORD_AUDIO` — for voice trigger
- `VIBRATE` — for feedback on trigger

Permissions are requested at runtime in `MainActivity` and `VoiceSOSActivity`.

## Author

Built by Chandni as a mini project.