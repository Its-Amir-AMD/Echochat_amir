# EchoChat Release Audit

## Source-level fixes included in this handoff

- Routed startup through a Firebase configuration gate and a real Firebase Authentication screen; the activity now observes authentication-state changes.
- Fixed the database factory call by making the local Room database account-scoped using the signed-in Firebase UID.
- Connected the Firestore backend to the repository for public channels, DMs, messages, reactions and sender-authorized message deletion.
- Added default shared channels without seeding fake users or sample messages.
- Replaced fake suggested DM contacts with a real recipient account ID and added sign-out/account-ID controls to the profile.
- Added Firestore rules for user/public profile ownership, public-channel membership, private message reads, messages/reactions, and social posts/likes/bookmarks/comments/stories.
- Added cloud listeners and writes at source level for Explore posts, likes, bookmarks, stories and comments; likes/counts and comments/counts use transactions.
- Added error reporting for backend initialization, listener failures and user-triggered cloud operations.
- Added Room migrations for persisted local chat-history clearing and story author IDs.
- Prevented automatic Android backup of the local app cache and stopped requiring a developer's private signing key just to configure the release variant.
- Added setup documentation, a release checklist and a privacy-policy draft.

## Not verified / release blockers

- **No APK or AAB was built.** Gradle distribution download failed due unavailable DNS/network, and Android SDK is not installed in this execution environment.
- Kotlin sources were only scanned for parser/syntax errors with an incomplete dependency classpath. This is not a successful Kotlin/Android compilation.
- No unit tests, Android lint, Room migration tests, Firebase Emulator tests or physical-device tests were run.
- The Firestore rules and cloud social paths are source implementations but have not been validated against the Firebase Emulator/project. Do not deploy the rules without that validation.
- No `app/google-services.json`, live Firebase project setup, owner-controlled release keystore or Play Integrity registration was provided; no signed AAB/APK can be produced from the supplied files alone.
- Privacy policy is a draft without a live public URL or monitored contact.
- Push notifications, read receipts, presence, end-to-end encryption, image/file uploads, account deletion/export, blocking/reporting and server-side moderation are not implemented.
- The post listener currently reads the full feed and chat messages sync only the latest 500 records per channel; pagination and scale testing remain outstanding.

**Release decision: NOT APPROVED FOR PUBLIC RELEASE YET.** The source includes significant fixes and a connected cloud-data path, but it needs a successful build, rule/emulator integration tests, two-account/device verification, production Firebase/App Check setup, privacy publication and release signing before publication.
