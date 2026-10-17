# EchoChat

EchoChat is a native Android project built with Kotlin, Jetpack Compose, Material 3, Room, coroutines, and Flow. The interface supports Persian (RTL) and English (LTR).

## Current functionality

- Responsive chat-style interface for phones and wide screens.
- Local Room persistence for channels, messages, profiles, social posts, stories, and comments.
- Message replies, reactions, and message deletion.
- Local Explore feed with category filtering, likes, bookmarks, and saved posts.
- Create and view text stories.
- Add comments to posts.
- Native Android Sharesheet for sharing post text.
- Persistent language and theme preferences using DataStore.
- Profile editing that preserves unrelated saved fields.
- Non-destructive Room migrations for the schema versions defined in this project.
- Firebase AI Logic integration for the Echo assistant, with a visible error message when Firebase AI is not configured or a request fails.
- GitHub Actions workflow for unit tests, lint, and a debug APK build.

## Important scope and deployment note

The current chat and social data repositories still use the local Room database. This means human conversations, channels, posts, comments, and stories are **not yet synchronized between accounts or devices**. Firebase Authentication and Cloud Firestore are not currently wired into those flows. Do not advertise this build as a production-ready multi-user or real-time cloud messenger until authentication, remote repositories, and restrictive production Firestore rules have been implemented and tested.

The seeded channels, sample messages, and sample feed content are local demo data. They are not actual online users or remote conversations. The app intentionally no longer fabricates replies from human participants.

## Requirements

- Android Studio with a compatible Android SDK.
- JDK 21 for the current Gradle/Android Gradle Plugin setup.
- Internet access to download Gradle and Maven dependencies.
- A Firebase project only when using Firebase AI features.

## Build and test

On macOS or Linux:

```bash
bash ./gradlew testDebugUnitTest
bash ./gradlew lintDebug
bash ./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat testDebugUnitTest
gradlew.bat lintDebug
gradlew.bat assembleDebug
```

The debug APK is generated at:

`app/build/outputs/apk/debug/app-debug.apk`

## Configure Firebase AI Logic

1. Create or select a Firebase project.
2. Register an Android app whose package name matches the project's `applicationId` in `app/build.gradle.kts`.
3. Enable Firebase AI Logic and select a supported Gemini Developer API model.
4. Download the project's `google-services.json` and place it under `app/google-services.json`. Do not commit private service-account credentials or debug tokens.
5. Register the debug App Check token shown by the Debug provider in the Firebase console for local development.
6. For release builds, register and configure Play Integrity for the actual application signing setup.
7. Build and run the app, then test the Echo AI conversation while online.

Firebase AI requests are real model requests; the application does not silently replace a failed model request with a canned response. If Firebase is missing or App Check denies the request, the conversation displays an explicit configuration/error message.

## Data and migration notes

Room schema version 3 includes the local posts, stories, and comments tables. Migrations are explicit; destructive fallback migration is not enabled. Keep any future schema changes paired with a migration and an appropriate database migration test.

## Security and release readiness

Before a public release, complete and verify at least the following:

- Firebase Authentication and account/session lifecycle.
- Firestore-backed chat and social repositories with authorization rules.
- Real conversation membership, delivery/read receipts, and presence.
- Production error reporting, data deletion/export policy, privacy policy, and abuse controls.
- Integration tests against Firebase emulators and the configured App Check environment.
- Release signing with an owner-controlled keystore and a verified Play Integrity setup.

Never commit API secrets, service-account keys, signing keystores, or App Check debug tokens.
