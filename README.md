# Aegis Browser - Production Android Web Browser

Aegis Browser is a production-oriented, privacy-first Android web browser powered by **Mozilla GeckoView (Omni)** and built using **Kotlin** and **Jetpack Compose**. It features genuine network request filtering, tracker blocking, popup/redirect protection, an anti-adblock diagnostic compatibility layer, isolated private browsing, and a dark cyber aesthetic.

---

## Key Features

1. **Mozilla GeckoView Engine**:
   - Uses genuine `org.mozilla.geckoview:geckoview-omni:125.0.20240425211020` delivering desktop-grade standards compliance, rendering fidelity, and WebExtension support.
   - Singleton `GeckoRuntime` architecture for minimal memory footprint.

2. **Built-in Ad Blocking & Content Filtering**:
   - Curated **AdGuard Base & Tracking Protection** list bundled locally in `assets/filters/adguard_base.txt`.
   - Native WebExtension (`aegis-blocker`) with `webRequest.onBeforeRequest` blocking and CSS cosmetic element hiding.
   - Fast domain lookup with support for `$third-party`, `$script`, `$image` resource types, and exception rule (`@@`) precedence.
   - Secure HTTPS list updates with size limits (10MB) and fallback retention on network failure.

3. **GeckoView Tracking Protection**:
   - Native `ContentBlocking.ANTI_TRACKING` and `ContentBlocking.COOKIE_BEHAVIOR_REJECT_TRACKERS`.
   - Real-time tracker blocking events reported directly to the Aegis Shield telemetry repository.

4. **Popup & Malicious Redirect Protection**:
   - Suppresses unsolicited `window.open` requests while preserving user-initiated authentication and payment flows.
   - Rapid redirect loop detection halts malicious redirect chains (>5 hops within 2.5 seconds).

5. **Anti-Adblock Compatibility & Diagnostic System**:
   - Distinguishes between resource blocks, DNS errors, TLS/SSL certificate failures, connection resets, and anti-adblock barriers.
   - Non-destructive recovery options: "Retry", "Temporarily Allow Site", or "Continue Blocking".
   - Per-site whitelist stored locally in `SiteExceptionManager`.

6. **Privacy & Local Persistence**:
   - **Room SQLite**: Local persistent storage for Bookmarks and Browsing History.
   - **DataStore**: High-speed preferences for search provider, ad-block toggles, and homepage.
   - **Isolated Private Browsing**: Memory-only sessions with zero disk caching, isolated cookie jars, and no history logging.

7. **Modern Dark UI**:
   - Deep void `#0A0D14` backgrounds with electric cyan `#00D2FF` and cobalt blue accents.
   - Edge-to-edge support with safe area insets handling (`WindowInsets.statusBars` and `WindowInsets.navigationBars`).
   - Accessible touch targets (>= 48dp), animated progress bar, tab switcher with live counts, and privacy dashboard sheet.

---

## Build and Run Instructions

### Prerequisites
- JDK 17 (Java 17 toolchain)
- Android SDK 36 (minSdk 26, targetSdk 36)
- Gradle 8.x+

### Debug Build
```bash
gradle assembleDebug
```
Output APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit and Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

---

## Architecture Overview

```
app/src/main/java/com/example/
├── MainActivity.kt               // Entry activity with edge-to-edge & intent routing
├── browser/
│   ├── GeckoEngineProvider.kt    // Singleton GeckoRuntime & WebExtension setup
│   ├── BrowserTab.kt             // Tab data model
│   ├── TabManager.kt             // Tab lifecycle, normal & private sessions
│   ├── NavigationHandler.kt      // GeckoSession Navigation & Progress delegates
│   └── UrlUtils.kt               // URL normalization & search engine query formatting
├── blocking/
│   ├── FilterRule.kt             // Filter rule models & domain matching
│   ├── FilterParser.kt           // AdGuard / EasyList syntax parser
│   ├── FilterEngine.kt           // Fast lookup, matching & secure HTTPS update
│   ├── BlockingEventRepository.kt// Real-time blocked event tracking & deduplication
│   ├── PopupManager.kt           // Unsolicited popup & redirect loop guard
│   └── SiteExceptionManager.kt   // Domain whitelist & exceptions
├── privacy/
│   ├── TrackingProtectionManager.kt // Native GeckoView ContentBlocking delegate
│   └── PrivateSessionManager.kt  // Isolated private browsing settings
├── compatibility/
│   └── DiagnosticClassifier.kt   // Classification of network, DNS, SSL & anti-adblock
├── data/
│   ├── Entities.kt               // Room Bookmark & History entities
│   ├── Daos.kt                   // Room DAOs
│   ├── AppDatabase.kt            // Room database configuration
│   └── Repositories.kt           // Repositories & DataStore settings
└── ui/
    ├── BrowserViewModel.kt       // Central state coordinator
    ├── BrowserScreen.kt          // Primary Compose UI
    ├── components/
    │   ├── PrivacyShieldSheet.kt // Live privacy shield & per-site controls
    │   ├── TabSwitcherSheet.kt   // Tab management & private tab switcher
    │   ├── SettingsDialog.kt     // Browser settings & search engine selector
    │   ├── BookmarksHistoryDialog.kt // Saved bookmarks & history list
    │   └── DiagnosticBanner.kt   // Smart diagnostic alert & recovery
    └── theme/
        ├── Color.kt              // Dark cyber palette tokens
        ├── Theme.kt              // Material 3 dark color scheme
        └── Type.kt               // Typography scale
```

---

## Dependencies & Licensing

- **Mozilla GeckoView Omni** (`MPL 2.0`)
- **Jetpack Compose & Material 3** (`Apache 2.0`)
- **AndroidX Room & DataStore** (`Apache 2.0`)
- **OkHttp** (`Apache 2.0`)
- **Robolectric & JUnit** (`MIT / EPL`)

---

## Known Limitations & Security Notes
- **Private Browsing Scope**: Private mode isolates cookies, cache, and history from device storage. It does not provide network anonymity from ISPs or web servers (use Tor or VPN for network anonymity).
- **Anti-Adblock Bypass**: Aegis Browser does not guarantee 100% bypass of server-side verified paywalls. When a site requires server-side advertising telemetry, the diagnostic layer transparently prompts the user to allow the site.
- **TLS Validation**: Aegis Browser strictly enforces TLS/SSL certificate validation and never bypasses invalid or expired certificates.
