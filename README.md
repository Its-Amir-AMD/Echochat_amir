# EchoChat

Native Android messaging and social-feed prototype built with Kotlin, Jetpack Compose, Material 3, Room and StateFlow. Persian (RTL) and English (LTR) are supported.

## Current local functionality

- Responsive channel and conversation UI.
- Room persistence for messages, channels, profiles, posts, stories and comments.
- Message replies, deletion and reactions.
- Social feed, category filters, local likes, bookmarks and saved posts.
- Creating and viewing text stories.
- Post comments with persisted local comment counts.
- Persistent language and theme preferences.
- Explicit Room schema migrations and indexes for common queries.
- Native Android Sharesheet for post text.

## Production limitations

**This repository is not yet a production multi-user messenger.** Firebase Authentication, remote Firestore synchronization, server-enforced conversation membership, genuine remote presence, and authenticated AI responses are not connected end-to-end. The initial messages and social content are locally seeded demo data, not real remote users or public content.

Do not deploy this build as a production messaging service until remote identity, cloud synchronization and backend authorization have been implemented and verified.

## Requirements

Use Android Studio, the Android SDK matching the configured compile SDK, and the JDK version required by the Gradle/Android Gradle Plugin versions in the repository.

## Build

Windows PowerShell:
```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

macOS/Linux:
```bash
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

The debug APK is normally generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Local database

Room stores local content in `echochat_database`. Explicit migrations are used instead of silently deleting user data during schema upgrades. New local databases are seeded with sample content for UI preview. Before production, isolate demo fixtures from real user data and replace the single local `me` profile convention with account-scoped records.

## Production checklist

1. Integrate Firebase Authentication and session-aware navigation.
2. Implement remote repositories for conversations, posts, comments and stories.
3. Deploy Firestore rules enforcing authentication, membership, ownership and field validation.
4. Configure App Check where appropriate and keep provider secrets on a trusted backend.
5. Move demo seed data to a development-only path.
6. Test account isolation, migrations, offline synchronization and authorization with Firebase Emulator Suite.
7. Review release signing, privacy disclosures, account deletion and backup behavior.

Never commit service-account keys, private provider API keys, passwords or GitHub tokens. Configure external credentials through an approved secret-management workflow, not source code.
