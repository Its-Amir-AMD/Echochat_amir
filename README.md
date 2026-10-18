# EchoChat

EchoChat is a native Android chat application built with Kotlin, Jetpack Compose, Material 3, Room, Kotlin coroutines/Flow, Firebase Authentication, and Cloud Firestore. The UI supports Persian (RTL) and English (LTR), with a compact phone layout and a two-pane layout for larger screens.

## Implemented in this branch

- Email/password registration, sign-in, sign-out, and password-reset flow through Firebase Authentication.
- Per-account Room cache files to avoid reusing the same local conversation cache when accounts change.
- Firestore-backed public channels and member-only direct messages.
- Realtime channel and message listeners, local cache reconciliation, basic message reactions, and sender-only message deletion.
- Account ID display and copy action in the profile; direct-message creation requires the recipient's Firebase UID, not a fabricated contact name.
- A restrictive starting Firestore ruleset in `firestore.rules` and Firebase CLI configuration in `firebase.json`.
- Responsive layout threshold and adaptive conversation-list width; the profile sheet scrolls and moves above the keyboard on compact devices.
- Persistent language/theme settings and Firebase AI Logic for the private Echo AI chat.
- GitHub Actions workflows for unit tests, lint, and a debug APK artifact.

## Honest feature scope

**This is a backend integration milestone, not yet a store-ready production release.**

- Chat channels and messages use Firestore when a Firebase project is configured. The Firestore database and Authentication provider do not exist until you create/configure them in your Firebase project.
- Explore posts, comments, stories, likes, and bookmarks still use the per-account Room cache in this branch. They are not a shared multi-user social feed yet. Do not advertise those screens as a cloud social network until their remote repositories and access rules are implemented and tested.
- Message delivery/read receipts, presence, typing indicators between devices, push notifications, attachments, abuse reporting/blocking, account deletion, export, and production observability are not implemented.
- Clearing a conversation currently clears the local cached history. It does not mean every message is erased from Firestore; adjust the product wording and add an explicit remote-deletion policy before shipping.
- The assistant conversation uses Firebase AI Logic. Provider, model availability, App Check, quota, and billing must be verified in your own Firebase project.
- Legacy installs used the old shared `echochat_database` file. This branch uses per-account databases; the old database is not automatically imported.

## Requirements

- Android Studio and Android SDK compatible with this repository's Gradle/Android Gradle Plugin versions.
- JDK 21.
- Internet access to download Gradle and Maven dependencies.
- A Firebase project with Authentication and Cloud Firestore enabled.
- Firebase CLI if deploying the security rules from this repository.

## Firebase setup

1. Create a Firebase project and register an Android app.
2. In `app/build.gradle.kts`, choose a permanent production `applicationId`. The existing `com.aistudio.echochat.vpkzqr` identifier is a generated project identifier; change it before publishing and register the matching package in Firebase.
3. Download the matching `google-services.json` and put it in `app/google-services.json`. This file is intentionally git-ignored; never commit service-account credentials, debug tokens, or signing keys.
4. In Firebase Authentication, enable the Email/Password provider.
5. Create a Cloud Firestore database.
6. Deploy the repository's starting ruleset:
   ```bash
   firebase deploy --only firestore:rules
   ```
7. Open Firebase console → App Check. Register the debug provider only for development. For release builds, configure Play Integrity for the actual package and signing certificate. Never ship a debug App Check token.
8. Enable Firebase AI Logic and verify the model/provider access used by the app. Set appropriate budgets, quotas, and monitoring.
9. Run through registration, sign-in/out, two-account messaging, unauthorized-read/write attempts, offline/reconnect, and small-phone/large-screen UI scenarios.

The provided Firestore rules are a restrictive baseline, not a substitute for a security review. Firestore query indexes may be requested by Firebase during test runs; create them only from the trusted index links or add reviewed index definitions to the repository.

## Build and test

On macOS/Linux:

```bash
bash ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

On Windows:

```bat
gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. A CI artifact is a debug build for testing, not a Play Store release package.

## Before public release

- Finish cloud persistence and rules for posts/comments/stories/bookmarks, or remove/clearly label the local-only social screens.
- Add integration tests against Firebase Auth/Firestore emulators, including users attempting to read or modify another user's private conversations and profiles.
- Verify schema/indexes, migration from supported legacy versions, rate limits, quotas, abuse controls, blocking/reporting, notification behavior, and recovery after offline writes.
- Implement clear user-facing privacy/data retention disclosures, account deletion, and applicable data export/deletion flows.
- Test accessibility, large font/display sizes, keyboard/insets, RTL/LTR, low-end devices, and Android versions targeted for release.
- Configure release signing using an owner-controlled keystore supplied through protected secrets. Do not store keystores or passwords in the repository.
- Replace the generated app identifier, set the final version code/name, confirm icon/assets and third-party licenses, and verify a signed release build on real devices.
- Complete Play Console declarations, privacy policy URL, Data safety form, content rating, and store listing.

**Do not publish this branch as a fully production-ready multi-user social platform until the checklist above is completed and the relevant tests pass.**
