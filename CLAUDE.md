# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug APK on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run a single unit test class
./gradlew test --tests "com.chandni.kavos.ExampleUnitTest"

# Clean build
./gradlew clean
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture Overview

**KAVOS** is an Android personal safety app (minSdk 24, targetSdk 36) written in Kotlin. It has no ViewModel/MVVM layer — activities communicate directly with Room and Firebase.

### App Entry Flow

`SplashActivity` (launcher) → checks `FirebaseAuth.currentUser`:
- Authenticated → `MainActivity`
- Not authenticated → `AuthActivity` → `MainActivity`

`DisguiseActivity` is a calculator disguise; typing the secret code (`111=` by default, user-configurable) navigates to `MainActivity`. This is how the app is designed to be opened discreetly.

### Data Layer

**Two persistence layers run in parallel:**

1. **Room DB (`kavos_database`)** — local, for guardians and SOS logs
   - `Contact` entity (`emergency_contacts` table): name, phoneNumber, customMessage
   - `SOSLog` entity (`sos_logs` table): timestamp, triggerMethod, location
   - `AppDatabase` singleton with `fallbackToDestructiveMigration()` (current version: 2)
   - All Room queries must run off the main thread (activities use raw `Thread {}` blocks)

2. **Firebase (Firestore + Auth)** — cloud, for user profile only
   - Auth: email/password sign-in
   - Firestore `users/{uid}`: stores name, phone, secret_code, email
   - After login, user data is synced to `SharedPreferences` (`KAVOS_PREFS`) with keys: `user_name`, `user_phone`, `secret_code`, `is_logged_in`

### SOS Trigger Pipeline

All SOS triggers funnel through `SOSHelper.sendSOS(context, triggerMethod)`:
1. Reads all contacts from Room DB (background thread)
2. Gets last known GPS/Network location via `LocationManager`
3. Sends SMS to each contact with their custom message + Google Maps link
4. Logs the event to `sos_logs` table

**SOS trigger methods:**
- Manual long-press on SOS button (`MainActivity`)
- Shake detection — 3 shakes above 1.8g within 2 seconds (`MainActivity` / accelerometer)
- Dead Man Switch — 5-minute inactivity timer (`MainActivity`)
- Voice trigger — phrase "help me" detected via `SpeechRecognizer` (`VoiceSOSActivity`)
- Battery low — `BatteryReceiver` listens for `ACTION_BATTERY_LOW`
- Check-in timer expiry (`CheckInTimerActivity`)

All triggers except Battery/Voice go through a 5-second countdown with tap-to-cancel.

### Key Dependencies

- **Room 2.6.1** with KSP (not kapt) for annotation processing
- **Firebase BOM 33.1.2** — firebase-auth-ktx, firebase-firestore-ktx
- **Google Services plugin 4.4.2** — requires `google-services.json` in `app/`
- No Jetpack Compose — pure XML layouts with View binding via `findViewById`

### Required Permissions

`SEND_SMS`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `RECORD_AUDIO`, `VIBRATE`

Runtime permission requests happen in `MainActivity.requestAllPermissions()` (SMS + location) and `VoiceSOSActivity` (audio).

### Default Location Fallback

When GPS is unavailable, `SOSHelper` defaults to hardcoded coordinates `12.9152, 80.0000` (college coordinates).
