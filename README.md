# ⚡ LinkUp Messenger

A modern, real-time Android chat application built with **Jetpack Compose**, **Kotlin**, and backed by **InsForge** (PostgreSQL-powered Backend-as-a-Service with Row Level Security).

---

## ✨ Features

- 🔐 **Authentication & Security:** Secure email & password sign-up and sign-in backed by InsForge Auth with session persistence and Row Level Security (RLS).
- 💬 **Real-Time Direct Messaging:** Fast 1-on-1 conversations with message delivery status, timestamps, and automatic message sync.
- 👥 **Friend Management & Discovery:**
  - Real-time user search by username or display name.
  - Send, accept, or decline friend requests.
  - Manage friends list with online indicator badges.
- 🎨 **Modern Jetpack Compose UI:**
  - Material 3 fluid animations and transitions.
  - Full Dark Mode and Light Mode support with seamless system theme switching.
  - Clean bottom navigation and responsive chat bubble layout.
- 🚀 **Release Ready:** ProGuard/R8 optimizations enabled, signed release APK ready for deployment.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0+ |
| **UI Toolkit** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Repository Pattern |
| **Networking** | OkHttp3, Retrofit2, Kotlinx Serialization |
| **Backend / DB** | [InsForge](https://insforge.dev) (PostgreSQL BaaS, PostgREST, Auth & RLS) |
| **State Management** | StateFlow, SharedFlow, Kotlin Coroutines |
| **Image Loading** | Coil Compose |

---

## 🗄️ Backend Schema (InsForge PostgreSQL)

- **`users`**: User profile information (id, username, display_name, avatar_url, bio, status, is_online, last_seen).
- **`friend_requests`**: Connection requests between users (sender_id, receiver_id, status: pending/accepted/rejected).
- **`chats`**: Conversation rooms tracking participants and last message activity.
- **`messages`**: Encrypted/secured message records with sender_id, recipient_id, content, and read receipts.

Database migrations and schemas are tracked in [`migrations/`](migrations/).

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala / Ladybug or newer
- JDK 17 or JDK 21
- Android SDK 34+

### Installation & Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Lalit05Hadkar/linkup-messenger.git
   cd linkup-messenger
   ```

2. **Configure Environment:**
   Create a `.env.local` or `.env` file in the project root:
   ```env
   INSFORGE_PROJECT_URL=https://4u3psaut.us-east.insforge.app
   INSFORGE_ANON_KEY=your_anon_key_here
   ```

3. **Build and Run:**
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Select an emulator or physical device running Android 8.0+ (API 26+).
   - Press **Run** (`Shift + F10`) or execute:
     ```bash
     ./gradlew assembleDebug
     ```

### 📦 Building Release APK

To produce an optimized production release build:
```bash
./gradlew assembleRelease
```
The resulting signed APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License

This project is licensed under the MIT License.
