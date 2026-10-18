# EchoChat Privacy Policy — Draft for Owner Review

**This is a draft, not a published policy.** Before publishing EchoChat, replace the contact placeholder, confirm the actual Firebase project configuration and data retention settings, and host the approved policy at a stable public URL.

## Information processed

EchoChat uses Firebase Authentication to process account credentials and account identifiers. The app stores account display name and avatar color in a public profile so registered users can identify a chat recipient. The private account document may contain the account email address.

Messages in public channels and direct messages are stored in Cloud Firestore and synchronized to signed-in members. Local chat cache and account preferences are stored on the device. If Echo AI is used, the prompt and conversation message sent to the assistant are processed by Firebase AI Logic to generate a response.

Explore posts, stories, comments and aggregate like counts are stored in Cloud Firestore and synchronized to signed-in users. Like and bookmark selections are stored under the signed-in account in Firestore; some display/view state, such as whether a story has been viewed on this device, remains local.

## Purpose and service providers

The app processes the information above to provide account sign-in, public-channel conversations, direct messaging, social feed features, local preferences and AI responses. Firebase services are provided by Google; their processing is also subject to the terms and privacy documentation that apply to the configured Firebase services.

## Security and retention

The repository contains Firestore Security Rules intended to restrict private conversations to their members and account-private data to its owner. These rules must be validated in the Firebase Emulator and the production project before release; the source has not been security-audited by an independent reviewer. This app version does not provide end-to-end encryption; do not treat messages as inaccessible to the project operator or cloud service administrators. The project owner must document data-retention and deletion procedures before public release.

## Children's privacy

The publisher must review applicable age requirements and privacy laws for the intended release regions before publishing. Do not collect information beyond what the service genuinely needs.

## Account and privacy requests

Account deletion, full cloud-data export and a self-service deletion workflow are not implemented in this version. Before release, the publisher must provide a verified process for privacy requests and state the contact address below.

**Publisher privacy contact:** `[ADD A MONITORED CONTACT ADDRESS BEFORE PUBLICATION]`

**Effective date:** `[SET BEFORE PUBLICATION]`
