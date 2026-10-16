# EchoChat — Native Android Real-Time Messenger

A native, modern Android messaging application built with Jetpack Compose, powered by Firebase Firestore for real-time synchronization and Jetpack Credential Manager for Google Sign-In, featuring full bilingual support (Persian/English) and RTL layout adaptation.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during requirements clarification:
> - **Architecture & Platform**: Full native Android implementation using modern Kotlin and Jetpack Compose (Material 3).
> - **Data & Backend**: Cloud persistence and live messaging powered by Firebase Firestore (`requestedProducts: ["firestore"]`).
> - **Localization**: Full bilingual experience supporting Persian (Farsi) and English with automatic and manual RTL/LTR switching and Persian-friendly typography.
>
> **Backend Provisioning Requirement**:
> Upon approving this plan, we will execute the two-step Firebase provisioning handshake (`show_aistudio_ui` for user consent followed by `ProvisionFirebase` via RPC) to configure Firestore and Google Sign-In with your project.

---

### 1. Overview & Core Concept

- **What It Does**: EchoChat provides a responsive, fluid chat experience on Android devices. Users authenticate securely with Google Sign-In, browse active chat channels or direct conversations, send and receive messages in real time with instant optimistic UI updates, view online presence and read receipts, and seamlessly toggle between Persian (RTL) and English (LTR).
- **Target Audience / Persona**: Users who need a fast, private, and aesthetically pleasing messaging tool on Android with native Persian language support, fluid animations, and real-time cloud synchronization across devices.
- **Key Value**: Delivers the desktop/web EchoChat experience as a high-performance native Android application with zero lag, gesture navigation, edge-to-edge design, and offline resilience backed by Firestore cache.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Authentication Flow**:
   - Clean, branded welcome screen with animated EchoChat emblem.
   - One-tap "Sign in with Google" button utilizing Jetpack `CredentialManager`.
   - Automatic credential discovery for returning users with instant session restore.
2. **Conversation List & Discovery Flow**:
   - Main dashboard featuring pinned channels (e.g., `#general`, `#tech`, `#random`) and direct message threads.
   - Dynamic search bar to filter channels or find messages.
   - Floating Action Button (FAB) to create new channels or initiate a direct chat.
   - Profile header displaying current user avatar, status indicator, and quick settings toggle (Language / Dark Theme).
3. **Chat Room Flow**:
   - Modern conversation canvas with auto-scrolling message list.
   - Distinct message bubbles: user messages styled with primary accent gradient, incoming messages with tonal container colors.
   - Timestamp and delivery status indicators (sent, delivered, read).
   - Rich input bar featuring text input, emoji selector trigger, quick action bar, and haptic-enabled send button.
   - Long-press contextual sheet for copy, quote/reply, and message deletion (if author).
4. **Settings & Customization Flow**:
   - Language selector modal (Persian / فارسی vs. English).
   - Theme toggle (System default / Dark mode / Light mode).
   - Sign-out action with clean Credential Manager cache invalidation.

#### Visual Identity & Theme
- **Aesthetic Direction**: *Neo-Editorial Sleek Dark / Clean Light*. High contrast, soft rounded geometry (16dp–24dp pill shapes), atmospheric background gradients, and airy whitespace.
- **Color Palette**:
  - Primary Accent: Deep Indigo / Electric Violet (`#6366F1` / `#818CF8`) providing vibrant contrast.
  - Surface Dark: Obsidian Onyx (`#0F172A`) and Elevated Card Slate (`#1E293B`).
  - Surface Light: Clean Alabaster (`#F8FAFC`) with Crisp White Cards (`#FFFFFF`).
  - Incoming Bubble: Subtle Slate (`#334155` dark / `#E2E8F0` light).
  - Outgoing Bubble: Primary Gradient / Indigo Fill (`#6366F1`).
  - Text & Emphasized Elements: High-legibility typography with M3 `onSurface` and `onPrimary` color tokens.
- **Typography & Hierarchy**:
  - Persian: High-legibility Naskh/Sans typography with comfortable line-height (1.4x) to prevent diacritic clipping.
  - English: Clean geometric sans-serif (Inter / Roboto) following Material 3 scale (`titleLarge`, `bodyLarge`, `labelMedium`).
- **RTL & Layout Adaptations**:
  - Automatic `CompositionLocalProvider(LocalLayoutDirection provides ...)` wrapper supporting both RTL (Persian) and LTR (English).
  - Mirrored chat bubble tails, input field icon placements, and navigation back arrows.

#### Interactive Feedback & Motion
- Micro-spring animations on message bubble appearances.
- Staggered entrance for channel lists on first launch.
- Tactile haptic feedback when sending messages or long-pressing bubbles.
- Typing indicators with pulsing animated dots.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Cloud Backend with Firebase Firestore**
  - *Chosen Approach*: Official Firebase Firestore Android SDK with real-time `snapshots()` Kotlin Coroutine Flows.
  - *Why*: Delivers sub-second message synchronization across devices, automated offline caching, and robust security rules without requiring maintenance of an external Node.js hosting instance.
  - *Alternatives Considered*: Direct WebSocket connection to a self-hosted Node server was rejected because it requires external infrastructure hosting, socket reconnect loops, and lacks automatic offline persistence.

- **Decision 2: Jetpack Credential Manager for Google Sign-In**
  - *Chosen Approach*: Official Android 14+ Jetpack `CredentialManager` API using `GetSignInWithGoogleOption` with the client ID generated by Google Services.
  - *Why*: Modern Android standard; offers smooth bottom-sheet authentication and zero-password friction. Complies with zero-trust security rules on Firestore.
  - *Alternatives Considered*: Anonymous authentication is unprovisioned on this backend and violates data durability expectations.

- **Decision 3: In-App Language Toggle & Bi-directional Support**
  - *Chosen Approach*: Reactive language state stored in DataStore/Preferences and applied through `LocalLayoutDirection` and dynamic string resources.
  - *Why*: Gives users full control to switch between Persian and English regardless of system-wide locale settings.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          EchoChat Android UI                           │
│  ┌───────────────────────┐  ┌─────────────────┐  ┌──────────────────┐  │
│  │     SignInScreen      │  │ ChannelListPane │  │  ChatRoomScreen  │  │
│  └───────────┬───────────┘  └────────┬────────┘  └────────┬─────────┘  │
└──────────────┼───────────────────────┼────────────────────┼────────────┘
               │                       │                    │
┌──────────────▼───────────────────────▼────────────────────▼────────────┐
│                    ViewModels (StateFlow / MVVM)                       │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │     AuthViewModel     │  │             ChatViewModel             │  │
│  └───────────┬───────────┘  └───────────────────┬───────────────────┘  │
└──────────────┼──────────────────────────────────┼──────────────────────┘
               │                                  │
┌──────────────▼──────────────────────────────────▼──────────────────────┐
│                           Repository Layer                             │
│  ┌──────────────────────────────────┐  ┌────────────────────────────┐  │
│  │          AuthRepository          │  │       ChatRepository       │  │
│  └──────────────────┬───────────────┘  └──────────────┬─────────────┘  │
└─────────────────────┼─────────────────────────────────┼────────────────┘
                      │                                 │
┌─────────────────────▼─────────────────────────────────▼────────────────┐
│                           Firebase Services                            │
│  ┌──────────────────────────────────┐  ┌────────────────────────────┐  │
│  │   Jetpack Credential Manager     │  │     Firebase Firestore     │  │
│  │      (Google Sign-In)            │  │ (Custom Database Instance) │  │
│  └──────────────────────────────────┘  └────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Data Model & Schema
1. **Channels Collection (`/channels/{channelId}`)**:
   - `id`: String (Document ID)
   - `name`: String (e.g., "General", "توسعه‌دهندگان")
   - `description`: String
   - `isDirectMessage`: Boolean
   - `memberIds`: List<String>
   - `lastMessage`: String
   - `lastMessageAt`: Timestamp (ServerTimestamp)
   - `createdAt`: Timestamp
2. **Messages Subcollection (`/channels/{channelId}/messages/{messageId}`)**:
   - `id`: String (Document ID)
   - `channelId`: String
   - `senderId`: String (Authenticated Google UID)
   - `senderName`: String
   - `senderAvatar`: String
   - `content`: String
   - `createdAt`: Timestamp (ServerTimestamp)
   - `readBy`: List<String>
3. **Users Collection (`/users/{userId}`)**:
   - `userId`: String
   - `displayName`: String
   - `email`: String
   - `photoUrl`: String
   - `isOnline`: Boolean
   - `lastSeen`: Timestamp

#### Interactive Component & State Mapping
- **Message Sending**: User types message -> triggers `onSendMessage()` -> Optimistic local state item added to view -> `ChatRepository.sendMessage()` adds document with `FieldValue.serverTimestamp()` -> Firestore listener synchronizes server state.
- **Channel Switching**: User clicks channel card -> triggers `onSelectChannel(channelId)` -> `ChatViewModel` re-binds real-time snapshot listener -> message list scrolls smoothly to bottom.
- **Language Switcher**: User selects English/Persian from top app bar -> triggers `onToggleLanguage()` -> Localized strings refresh and layout direction updates between `Rtl` and `Ltr`.
