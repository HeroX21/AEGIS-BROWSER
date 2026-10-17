package com.example.browser

import org.mozilla.geckoview.GeckoSession

data class BrowserTab(
    val id: String,
    val session: GeckoSession,
    val isPrivate: Boolean,
    var url: String = "about:blank",
    var title: String = "New Tab",
    var isLoading: Boolean = false,
    var progress: Int = 0,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
    var isSecure: Boolean = false,
    var blockedAdsCount: Int = 0,
    var blockedTrackersCount: Int = 0
)
