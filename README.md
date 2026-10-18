# EchoChat — Android

EchoChat is a native Android app built with Kotlin, Jetpack Compose, Material 3, Room, coroutines and Firebase. The interface supports Persian RTL and English LTR and provides compact phone and wide-screen layouts.

## Implemented in this source candidate

- Email/password registration, sign-in, password reset and sign-out through Firebase Authentication.
- A Firebase setup screen instead of a crash or a fake local-only session when Firebase configuration is missing.
- Firestore-backed public channels, custom public-channel creation, membership-aware direct messages and real-time message synchronization.
- Message replies, reactions and sender-only message deletion, with visible error feedback for failed cloud operations.
- Per-account Room database caches and a local history-clear threshold so one account does not reuse another account's private message cache and cleared history is not immediately reinserted from an old snapshot.
- Firebase profile synchronization and local language/theme preferences.
- Echo AI through Firebase AI Logic, with an explicit unavailable/error response instead of a canned answer presented as real AI output.
- Firestore-backed Explore posts, likes, bookmarks, stories and comments at source level, plus user-scoped like/bookmark state.
- Firestore security rules for profiles, channel access, messages/reactions and social content operations.
- Improved responsive Compose layout, login flow, keyboard inset handling, text-length validation and error surfaces.
- GitHub Actions workflows for test/lint/debug build checks.

## Release status and scope limits

**This is a substantially improved release candidate, but it is not yet verified or approved for public-store release.** The Android build, Room migrations, security rules, Firebase-backed social operations and two-account messaging flows still require successful CI, emulator and physical-device testing.

The source includes cloud synchronization for social posts/stories/comments/likes/bookmarks, but those paths have **not** been integration-tested against a real Firebase project or the Firebase Emulator. Treat them as implemented in source, not as validated production behavior. The post feed currently listens to the whole collection; paging and server-side moderation will be needed as the service grows. Each chat syncs at most the latest 500 messages. Older cloud messages remain on the server but are not loaded into the local conversation by this query.

Other features not currently implemented include end-to-end encryption, push notifications, read receipts, online presence, image/file uploads, account deletion/export, user blocking/reporting and server-side abuse moderation. Local chat-history clearing hides history on this account/device; it does not delete server-side messages. Direct messages start by entering the recipient's EchoChat account ID shown on their profile.

## Requirements

- Android Studio with Android SDK Platform 36 (including the configured minor API level if prompted).
- JDK 21.
- Network access to resolve Gradle and Maven dependencies.
- A Firebase project configured for Authentication, Cloud Firestore, App Check and Firebase AI Logic.

## Firebase setup

1. Create/select a Firebase project and register an Android app with application ID `com.aistudio.echochat.vpkzqr` (set in `app/build.gradle.kts`). Keep this ID stable after first publication.
2. Enable **Email/Password** under Firebase Authentication.
3. Create Cloud Firestore.
4. Download `google-services.json` and place it at `app/google-services.json`. This private/environment-specific file is ignored by Git.
5. Test `firestore.rules` against a Firebase Emulator first. When reviewed, publish with Firebase CLI:

   ```bash
   firebase deploy --project YOUR_FIREBASE_PROJECT_ID --only firestore:rules
   ```

   Replace the placeholder with the exact project ID and authenticate Firebase CLI against that project. Review before deploying to a database with existing data.
6. Enable Firebase AI Logic and confirm that the model configured in the source is available for your selected provider/region. Do not embed a raw Gemini API key in the app.
7. For debug testing, register the App Check debug token in Firebase Console. For production, register Play Integrity for the real application signing certificate and only enable enforcement after testing.
8. Use two accounts on at least two devices/emulators to test public channels, private DMs, sign-out/account switching, social posts, likes, bookmarks, stories and comments. Confirm a non-member cannot read a private DM.

## Build, test and lint

Run from the project root in an environment with Android SDK and working network access:

```bash
bash ./gradlew --no-daemon testDebugUnitTest
bash ./gradlew --no-daemon lintDebug
bash ./gradlew --no-daemon assembleDebug
```

On Windows, use `gradlew.bat` with the same tasks. The debug APK path is `app/build/outputs/apk/debug/app-debug.apk`.

**Build status in this handoff:** no APK/AAB was produced here. The environment could not resolve/download the Gradle distribution and does not have an Android SDK installed. Source-level checks are not a substitute for a successful Android build. Run the GitHub Actions workflows in a network-enabled environment and fix any failures before release.

## Release signing

A clean checkout is configured not to require an owner's private keystore just to configure a release build; without signing credentials, any resulting release artifact will be unsigned and cannot be published as a store-ready package. For signing, set these environment variables in a private build environment:

- `KEYSTORE_PATH`: absolute path to the `.jks` / `.keystore` file.
- `STORE_PASSWORD`: keystore password.
- `KEY_ALIAS`: key alias (defaults to `upload`).
- `KEY_PASSWORD`: key password.

Then run `bash ./gradlew --no-daemon bundleRelease`. Verify the output signature, package ID, version, Play Integrity configuration and release track before upload. Never commit keystores or passwords.

## Required before public release

- Get passing CI for Android build, unit tests and lint; verify Room migrations with actual upgrade tests.
- Test Firestore rules using the Firebase Emulator and tests for authorized/unauthorized access, counters, deleted posts, private DMs and two-account synchronization.
- Run manual tests on small phones, tablets, RTL/LTR, keyboard/IME, offline/reconnect, account switching, denied/invalid Firebase configuration and signed release builds.
- Add post-feed pagination and an intentional retention/cleanup strategy for server content.
- Implement an owner-reviewed account deletion/export flow, blocking/reporting, moderation and abuse controls before inviting the public.
- Publish a privacy policy at a stable public URL, provide a monitored privacy contact, and ensure the store's Data safety declaration matches Firebase/AI data flows.
- Finalize the app branding, legal/publisher information, signing key, versioning and store assets.
