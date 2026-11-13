# ReelFlow 500 — Facebook Multi-Page Automation (brand-new app)

A brand-new, phone-first, local-first automation platform for up to **500 Facebook Pages**:
AI video generation (Google Veo 3.0 / 3.1, Standard + Fast), a separate thumbnail per
video, one video → one assigned page, approval first, then auto-publish at a scheduled
time, plus title / caption / hashtags per video and on-device analytics.

This project was built **from zero** in `~/workspace/reelflow-new/`. It does NOT touch
the old app in `~/workspace/autoreels/` — that project stays exactly as-is. Proven
working pieces (Graph API 3-phase Reels upload, AES256 token store, Veo service,
scheduler, thumbnail extractor) were **copied and adapted** from the old app, not
re-implemented from scratch.

## Stack choice (and why)

| Choice | Decision | Why |
|---|---|---|
| Android | Native **Kotlin + Jetpack Compose** (Material 3) | Flutter is the plan's choice, but Flutter is **not installed** on this VM, local Flutter builds would OOM (~500 MB free RAM), and the proven build path here is Gradle via GitHub Actions. Native Kotlin ports the old app's proven services directly. |
| Web dashboard | **Vite + React 19 + TypeScript** | Node 24 / npm available on the VM; mirrors the old app's web frontend; static build, no server. |
| Storage | **Local-first on the phone** (SharedPreferences + Gson, encrypted token store) | The user has no dollars and no laptop — no AWS / backend / monthly server cost yet. Backend comes later. |
| Publishing | **Meta Graph API v21.0, 3-phase Reels upload**, direct from the device | No server in the middle. One video → one assigned page. |
| Video generation | **Google Veo 3.0 / 3.1** (Standard + Fast), direct Gemini API | The user's own AI Studio key, stored encrypted on-device. Prompts are sent **100% verbatim**. |

## Project structure

```
reelflow-new/
├── README.md
├── .github/workflows/build-apk.yml   # GitHub Actions: builds debug APK (+ release when secrets exist)
├── android/                          # Native Android app (package com.reelflow.platform)
│   ├── build.gradle.kts / settings.gradle.kts / gradle.properties
│   ├── gradlew / gradle/wrapper/
│   └── app/
│       ├── build.gradle.kts          # versionCode 1, versionName 1.0.0
│       ├── src/main/AndroidManifest.xml
│       ├── src/main/res/values/strings.xml   # app name "ReelFlow 500", Meta App ID
│       └── src/main/java/com/reelflow/platform/
│           ├── MainActivity.kt / PlatformApp.kt
│           ├── data/model/Models.kt          # JobStatus, FacebookPage, VideoReelItem, VeoModel, AnalyticsSummary
│           ├── data/interfaces/ServiceInterfaces.kt
│           ├── data/service/                 # SecureTokenStore (AES256), AccountStore (50 max),
│           │                                 # PagePromptStore (verbatim master prompts), JobStore,
│           │                                 # ThumbnailService (frame extraction), Veo3GenerationService,
│           │                                 # RealMetaPublishingService (3-phase Reels upload),
│           │                                 # FacebookLoginHelper (SDK login + token login),
│           │                                 # PublishScheduler + ScheduleWorker (WorkManager),
│           │                                 # MockPublishingService (demo mode, 500 sample pages)
│           ├── data/repository/PlatformRepository.kt   # pages + jobs + approval + scheduling + publish gates
│           ├── viewmodel/PlatformViewModel.kt
│           ├── ui/navigation/NavGraph.kt
│           ├── ui/theme/Theme.kt
│           └── ui/screens/                   # Dashboard, VideoLibrary, CreateVideo, Approval,
│                                              # ThumbnailEditor, Schedule, Pages, Analytics,
│                                              # Settings, VideoDetail, UiKit
└── web/                              # Vite + React + TS dashboard (same screens, localStorage)
    ├── package.json / vite.config.ts / tsconfig.json / index.html
    └── src/ (App.tsx, types.ts, index.css, main.tsx, components/*.tsx)
```

## Screens (Android)

- **Home Dashboard** — video / page / scheduled / published counts + quick actions + recent videos.
- **Video Library** — all videos, status filter, gallery import, bulk assign to a page, delete.
- **Create Video** — pick target page, Veo model (Standard / Fast, 3.0 / 3.1), page master
  prompt (verbatim) or custom prompt, aspect ratio, Generate.
- **Thumbnail Editor** — candidate frames (25% / 50% / 75%) extracted on-device; tap to set cover.
- **Approval** — video + thumbnail + title + caption + hashtags + target page together;
  Approve / Reject. **A rejected video is NEVER published.**
- **Scheduling Calendar** — date + time picker for APPROVED videos; WorkManager publishes
  at that time even if the app is closed; cancel returns the video to Approved.
- **Pages** — up to 500 pages: search, name, Graph Page ID, token health, enable toggle,
  token verify, per-page master prompt editor.
- **Analytics** — totals, published by page, recent jobs (from on-device records).
- **Settings** — connected accounts (Facebook Login button + **Token se Connect**),
  Veo API key (encrypted), publishing mode (Demo / Real), system logs.

There is deliberately **no email+password login field anywhere** — Facebook forbids
password login in third-party apps and it would get the app banned.

## Facebook connection

1. **Facebook Login button** — opens the official Facebook login page (Meta App
   "Rahim Automation Agent", App ID `1138116601998610`). After login, `/me/accounts`
   returns every Page access token, saved encrypted.
2. **Token se Connect** — paste a User Token from Graph API Explorer (same Meta app,
   permissions `pages_show_list`, `pages_manage_posts`, `pages_read_engagement`).
   Tokens are never a password and are never shown in chat.

## Build

### Web dashboard
```bash
cd web
npm install
npm run build   # -> web/dist/
npm run dev     # local preview
```

### Android (GitHub Actions — the reliable route)
Push this repo to GitHub. The workflow builds a **debug APK** on every push
(artifact `ReelFlow-500-Debug-APK`, installable on the phone). The release APK
builds only when the repo has these secrets:
`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`,
`RELEASE_KEY_PASSWORD`.

### Android (local — does NOT work on this dev VM)
Local Gradle builds are **blocked on this VM by the sandbox**: Java TCP
connections are intercepted (the runtime injects a policy notice into Java
socket streams and closes them), so Gradle's daemon IPC over localhost
fails ("Could not receive a message from the daemon") and dependency
downloads fail ("Connection reset"). curl works, Java does not. Use the
GitHub Actions route below instead.

## Still needed (not in this repo)

- **A new GitHub repo + PAT** to push this project and run the Actions build
  (the builder has no PAT and will not invent one). Suggested name:
  `rahimmatta106-droid/reelflow-500`.
- **A Veo API key** (the user's own Google AI Studio key) — entered in the app's
  Settings, stored encrypted on-device. Never put it in chat or in the repo.
- **Meta App Review → Live mode** (later, optional): removes the developer-registration
  + tester-accept gate entirely. Needs a privacy-policy URL + demo + permission
  approval, submitted by the Joshi (admin) account.
