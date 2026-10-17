# Aegis Browser - Implementation Plan

Aegis Browser is a production-oriented Android web browser built with Mozilla GeckoView and Jetpack Compose. It combines real browser-engine rendering with comprehensive built-in ad blocking, tracker suppression, popup protection, an anti-adblock compatibility diagnostic engine, and strict private browsing under a modern dark aesthetic.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were aligned during the interactive clarification phase:

- **UI Framework (Confirmed)**: Jetpack Compose with Material 3 Dark theme, utilizing `AndroidView` interop to host `GeckoView` content surfaces while maintaining Compose-native address bar, tab switcher, bottom toolbars, and modal sheets.
- **Default Search Engine (Confirmed)**: Google Search (`https://www.google.com/search?q=%s`), with one-tap switching to DuckDuckGo, Brave Search, or Startpage in Settings.
- **Default Filter Lists (Confirmed)**: AdGuard Base and Tracking Protection rule sets bundled locally in assets and backed by an HTTPS update engine with integrity verification and fallback protection.
- **Rendering Engine**: Mozilla GeckoView (`org.mozilla.geckoview:geckoview-omni`) leveraging Gecko runtime configuration, content blocking flags, and a dedicated WebExtension background service for fine-grained network request interception and cosmetic filtering.

---

## 1. Overview & Core Concept

- **What It Does**: Aegis Browser renders modern web content using Mozilla's Gecko engine rather than standard system WebViews. It enforces network-level ad and tracking blocking, blocks unsolicited popups and malicious redirects, provides intelligent diagnostic reporting when sites degrade, and protects user identity with isolated private tabs and zero cloud telemetry.
- **Target Audience / Persona**: Security-conscious mobile users, privacy advocates, and power users who demand genuine desktop-grade content blocking and tab isolation on Android without telemetry or intrusive advertisements.
- **Key Value**: Delivers the performance and web-standard fidelity of GeckoView while cutting data consumption and intrusive tracking, wrapped in an ergonomic, thumb-friendly dark interface.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Browsing & Address Entry**:
   - The user opens the app to a clean home surface or active session.
   - Tapping the top address bar highlights the URL and triggers instant search suggestion/domain autocompletion.
   - Typing search terms routes to Google Search, while valid domains/schemes navigate directly.
   - As the page loads, an animated linear progress indicator runs across the bottom of the address bar.
2. **Privacy Shield & Dashboard**:
   - The address bar features a prominent Privacy Shield icon displaying the count of blocked ads and trackers on the current page.
   - Tapping the shield opens a bottom sheet showing real-time statistics (ads blocked, trackers thwarted, popups intercepted), SSL/TLS certificate status, and a toggle to whitelist the current domain.
3. **Multi-Tab Management**:
   - The bottom bar shows the active tab counter pill.
   - Tapping opens a visual tab switcher grid with live thumbnail cards, title, URL, close buttons, and a "+ New Tab" FAB.
   - Includes a segmented control to toggle between Normal Tabs and Private Tabs (incognito session with distinct purple/magenta indicator and memory-only storage).
4. **Anti-Adblock Diagnostic & Recovery**:
   - When a site experiences an error or anti-adblock barrier, the Diagnostic Classifier identifies whether the issue stems from a blocked network asset, server verification requirement, DNS error, or offline state.
   - An amber diagnostic banner or modal offers non-destructive recovery options: "Retry", "Temporarily Allow Site", or "Continue Blocking".
5. **Settings & Data Management**:
   - Accessible via the three-dot overflow menu. Provides granular controls for Content Blocking, Search Engines, Site Exceptions whitelist, Download Management, and a one-tap "Clear Browsing Data" dialog (cache, cookies, history, tabs).

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech luxury dark, clean cyber-minimalism with rounded surfaces and restrained luminescence.
- **Color Palette**:
  - `Background`: Deep void `#0A0D14`
  - `Surface`: Charcoal dark `#121722`
  - `Surface Variant / Borders`: Subtle steel `#1E2638`
  - `Primary Accent`: Electric Cyan `#00D2FF`
  - `Secondary Accent`: Cobalt Blue `#2F80ED`
  - `Warning / Diagnostic`: Warm Amber `#FFB020`
  - `Private Mode Accent`: Neon Violet `#9D4EDD`
- **Typography & Hierarchy**: Clean sans-serif hierarchy using Android system Material 3 typography with legible tabular figures for counters and URL displays.
- **Layout & Inset Handling**: Full edge-to-edge support with `enableEdgeToEdge()`, respecting `WindowInsets.statusBars` for the top toolbar and `WindowInsets.navigationBars` for the bottom action bar.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: GeckoView Engine Architecture
- **Chosen Approach**: Integrate `org.mozilla.geckoview:geckoview-omni` with a centralized `GeckoRuntime` singleton. Sessions are encapsulated in `BrowserSession` instances managed by a `TabManager`.
- **Why**: `geckoview-omni` bundles 64-bit and 32-bit native Gecko libraries for both ARM and x86 architectures, ensuring seamless execution on real devices and emulators. Singleton runtime management prevents multiple process allocations and saves device memory.
- **Alternatives Considered**: Android `WebView` was rejected because it does not support Gecko WebExtensions or native Gecko tracking protection.

### Decision 2: Hybrid Content Blocking Strategy
- **Chosen Approach**: A two-tier defense:
  1. *GeckoView Native Content Blocking*: Enabled in `GeckoRuntimeSettings` (`ContentBlocking.ANTI_TRACKING`, `ContentBlocking.COOKIE_BEHAVIOR_REJECT_TRACKERS`).
  2. *WebExtension & Filter Engine*: A bundled WebExtension running in GeckoView that uses `webRequest.onBeforeRequest` and CSS cosmetic injection matching AdGuard Base rules.
- **Why**: Combines zero-latency engine-level tracking protection with AdGuard filter list syntax matching, ensuring high ad-block efficacy.
- **Alternatives Considered**: Simple URL regex in Kotlin was rejected as insufficient for real-world blocking and cosmetic hiding.

### Decision 3: Local Persistence with Room and DataStore
- **Chosen Approach**:
  - `DataStore Preferences`: High-speed key-value persistence for browser settings (selected search engine, blocking toggles, popup preference, dark mode).
  - `Room Database`: Structured local SQLite database for History entries, Bookmarks, Site Exceptions (whitelist), and Blocking Log history.
  - *Private Browsing Isolation*: Private tabs use an ephemeral in-memory session; their history and cookies are never written to Room or persistent disk.
- **Why**: Zero cloud dependency guarantees 100% user privacy and compliance with Google Play storage policies.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Aegis Browser UI                              │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │   Top Bar (Address/Search Input, Shield Icon, Tab Count, Menu)   │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │   Web View Area (AndroidView hosting GeckoView Surface)          │  │
│  ├──────────────────────────────────────────────────────────────────┤  │
│  │   Bottom Navigation (Back, Forward, Home, Tabs, Overflow)        │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│           ▲                                          │                 │
│           │ State Flow / Events                      │ User Action     │
│           ▼                                          ▼                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                       BrowserViewModel                           │  │
│  │   - activeTabId            - navigationState (canGoBack/Forward) │  │
│  │   - currentUrl / title     - blockingStats (ads/trackers/popups) │  │
│  │   - progressPercent        - activeExceptions                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└───────────────────────────┬────────────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌───────────────────────┐       ┌────────────────────────────────────────┐
│   GeckoEngine Layer   │       │          Data & Blocking Layer         │
│ ┌───────────────────┐ │       │ ┌────────────────────────────────────┐ │
│ │   GeckoRuntime    │ │       │ │           FilterEngine             │ │
│ │ (Shared Singleton)│ │       │ │ (AdGuard Base + Tracking Rules)    │ │
│ └─────────┬─────────┘ │       │ └──────────────────┬─────────────────┘ │
│           │           │       │                    │                   │
│ ┌─────────▼─────────┐ │       │ ┌──────────────────▼─────────────────┐ │
│ │    TabManager     │◄┼───────┼─┤       SiteExceptionManager         │ │
│ │ (Normal & Private │ │       │ └────────────────────────────────────┘ │
│ │  GeckoSessions)   │ │       │ ┌────────────────────────────────────┐ │
│ └─────────┬─────────┘ │       │ │         DiagnosticClassifier       │ │
│           │           │       │ └────────────────────────────────────┘ │
│ ┌─────────▼─────────┐ │       │ ┌────────────────────────────────────┐ │
│ │ NavigationHandler │ │       │ │       AppDatabase (Room SQLite)    │ │
│ │ & PopupDelegate   │ │       │ │ (Bookmarks, History, Exceptions)   │ │
│ └───────────────────┘ │       │ └────────────────────────────────────┘ │
└───────────────────────┘       └────────────────────────────────────────┘
```

### Core Entities & Persistence Schemas

1. **`HistoryEntity`**: `id`, `url`, `title`, `timestamp`, `visitCount`
2. **`BookmarkEntity`**: `id`, `url`, `title`, `createdAt`, `faviconUri`
3. **`SiteExceptionEntity`**: `domain`, `adBlockingDisabled`, `popupAllowed`, `createdAt`
4. **`BlockingEventEntity`**: `id`, `domain`, `blockedUrl`, `category` (`AD`, `TRACKER`, `POPUP`, `MALICIOUS`), `timestamp`
5. **`BrowserSettings` (DataStore)**:
   - `adBlockingEnabled`: Boolean (default true)
   - `trackingProtectionEnabled`: Boolean (default true)
   - `popupProtectionEnabled`: Boolean (default true)
   - `searchEngine`: String ("Google", "DuckDuckGo", "Brave")
   - `homePageUrl`: String ("https://www.google.com")
   - `privateBrowsingDefault`: Boolean (false)

### Interactive Handlers & State Flow

- **Address Input**: Enter key or query submission parses string. If valid URL (`http://`, `https://`, or matching standard domain pattern), loads URI; otherwise formats Google query `https://www.google.com/search?q={query}`.
- **GeckoSession Delegates**:
  - `ProgressDelegate`: Emits page title, location changes, security indicators, and progress percentages (0-100).
  - `NavigationDelegate`: Handles `onCanGoBack`/`onCanGoForward` updates, external app intent launches, and safe redirect validation.
  - `ContentBlocking.Delegate`: Intercepts tracking elements and increments tracker counters.
  - `PromptDelegate`: Intercepts web alerts, confirms, prompts, and file uploads.
- **Popup & Redirect Protection**:
  - `NavigationDelegate.onNewSession`: Suppresses unsolicited window open calls unless triggered by a genuine user gesture or if the domain is whitelisted in `SiteExceptionManager`.
  - Malicious redirect detector halts redirect chains exceeding 5 hops in under 2 seconds.
- **Diagnostic Engine**:
  - Analyzes navigation errors (`ERROR_HOST_NOT_FOUND`, `ERROR_CONNECTION_REFUSED`, `ERROR_SECURITY_SSL`, or script execution blocks).
  - Emits structured diagnostic state to the UI to render the recovery options.

---

## 5. Verification & Testing Strategy

- **Robolectric Unit Tests**:
  - `UrlParserTest`: URL validation, protocol normalization, and search query routing.
  - `FilterRuleMatcherTest`: Domain matching, sub-resource classification, wildcard rules, and exception precedence.
  - `DiagnosticClassifierTest`: Verifies correct categorization of blocked resources vs. DNS errors vs. SSL errors vs. suspected anti-adblock barriers.
  - `SiteExceptionTest`: Ensures whitelisted domains bypass filtering rules.
  - `RedirectLoopDetectorTest`: Validates detection and suppression of rapid redirect chains.
- **Compilation Check**: Run `compile_applet` to verify clean build of dependencies, GeckoView integration, and Compose UI.
