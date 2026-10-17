package com.example.blocking

import java.util.concurrent.ConcurrentHashMap

class PopupManager(
    private val siteExceptionManager: SiteExceptionManager = SiteExceptionManager.instance,
    private val blockingEventRepository: BlockingEventRepository = BlockingEventRepository.instance
) {

    // Tracks navigation redirect chain timestamps: tabId -> List<Long>
    private val redirectChainTracker = ConcurrentHashMap<String, MutableList<Long>>()

    fun shouldAllowNewWindow(
        tabId: String,
        sourceUrl: String?,
        targetUrl: String,
        hasUserGesture: Boolean
    ): Boolean {
        val sourceDomain = sourceUrl?.let { FilterRule.extractHost(it) } ?: ""

        // If site is whitelisted for popups, permit
        if (sourceDomain.isNotEmpty() && siteExceptionManager.isPopupAllowed(sourceDomain)) {
            return true
        }

        // If user explicitly clicked a link or triggered a gesture, permit (e.g. auth login, checkout)
        if (hasUserGesture) {
            return true
        }

        // Suppress unsolicited background popup
        val targetDomain = FilterRule.extractHost(targetUrl)
        blockingEventRepository.recordEvent(
            tabId = tabId,
            category = BlockCategory.POPUP,
            url = targetUrl,
            domain = if (targetDomain.isNotEmpty()) targetDomain else sourceDomain
        )
        return false
    }

    fun isMaliciousRedirectLoop(tabId: String, targetUrl: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = redirectChainTracker.getOrPut(tabId) { ArrayList() }

        synchronized(timestamps) {
            // Remove timestamps older than REDIRECT_WINDOW_MS
            timestamps.removeAll { now - it > REDIRECT_WINDOW_MS }
            timestamps.add(now)

            if (timestamps.size >= MAX_REDIRECTS_IN_WINDOW) {
                // Loop detected!
                val targetDomain = FilterRule.extractHost(targetUrl)
                blockingEventRepository.recordEvent(
                    tabId = tabId,
                    category = BlockCategory.MALICIOUS_REDIRECT,
                    url = targetUrl,
                    domain = targetDomain
                )
                return true
            }
        }
        return false
    }

    fun clearTabRedirectHistory(tabId: String) {
        redirectChainTracker.remove(tabId)
    }

    companion object {
        private const val REDIRECT_WINDOW_MS = 2500L
        private const val MAX_REDIRECTS_IN_WINDOW = 5

        val instance by lazy { PopupManager() }
    }
}
