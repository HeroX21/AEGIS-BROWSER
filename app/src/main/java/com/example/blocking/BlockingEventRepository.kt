package com.example.blocking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class BlockCategory {
    AD,
    TRACKER,
    POPUP,
    MALICIOUS_REDIRECT
}

data class BlockingEvent(
    val id: String,
    val domain: String,
    val blockedUrl: String,
    val category: BlockCategory,
    val timestamp: Long = System.currentTimeMillis()
)

data class BlockingStats(
    val totalAdsBlocked: Int = 0,
    val totalTrackersBlocked: Int = 0,
    val totalPopupsSuppressed: Int = 0,
    val totalMaliciousRedirectsBlocked: Int = 0,
    val currentTabAdsBlocked: Int = 0,
    val currentTabTrackersBlocked: Int = 0,
    val currentTabPopupsSuppressed: Int = 0
)

class BlockingEventRepository {

    private val _stats = MutableStateFlow(BlockingStats())
    val stats: StateFlow<BlockingStats> = _stats.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<BlockingEvent>>(emptyList())
    val recentEvents: StateFlow<List<BlockingEvent>> = _recentEvents.asStateFlow()

    // Per-tab event deduplication sets: tabId -> Set of blocked URLs
    private val tabDeduplicationCache = ConcurrentHashMap<String, MutableSet<String>>()

    private val totalAds = AtomicInteger(0)
    private val totalTrackers = AtomicInteger(0)
    private val totalPopups = AtomicInteger(0)
    private val totalRedirects = AtomicInteger(0)

    fun recordEvent(
        tabId: String,
        category: BlockCategory,
        url: String,
        domain: String
    ): Boolean {
        val tabSet = tabDeduplicationCache.getOrPut(tabId) { ConcurrentHashMap.newKeySet() }
        val dedupeKey = "$category:$url"

        // Avoid counting duplicate events for the same URL in the same tab session
        if (!tabSet.add(dedupeKey)) {
            return false
        }

        when (category) {
            BlockCategory.AD -> totalAds.incrementAndGet()
            BlockCategory.TRACKER -> totalTrackers.incrementAndGet()
            BlockCategory.POPUP -> totalPopups.incrementAndGet()
            BlockCategory.MALICIOUS_REDIRECT -> totalRedirects.incrementAndGet()
        }

        val event = BlockingEvent(
            id = "${System.currentTimeMillis()}-${tabSet.size}",
            domain = domain,
            blockedUrl = url,
            category = category
        )

        val updatedEvents = (_recentEvents.value.take(49) + event)
        _recentEvents.value = updatedEvents

        updateStats(tabId)
        return true
    }

    fun onTabNavigated(tabId: String) {
        tabDeduplicationCache[tabId]?.clear()
        updateStats(tabId)
    }

    fun onTabClosed(tabId: String) {
        tabDeduplicationCache.remove(tabId)
    }

    private fun updateStats(currentTabId: String) {
        val tabEvents = tabDeduplicationCache[currentTabId] ?: emptySet()
        var tabAds = 0
        var tabTrackers = 0
        var tabPopups = 0

        for (item in tabEvents) {
            when {
                item.startsWith("${BlockCategory.AD}:") -> tabAds++
                item.startsWith("${BlockCategory.TRACKER}:") -> tabTrackers++
                item.startsWith("${BlockCategory.POPUP}:") -> tabPopups++
            }
        }

        _stats.value = BlockingStats(
            totalAdsBlocked = totalAds.get(),
            totalTrackersBlocked = totalTrackers.get(),
            totalPopupsSuppressed = totalPopups.get(),
            totalMaliciousRedirectsBlocked = totalRedirects.get(),
            currentTabAdsBlocked = tabAds,
            currentTabTrackersBlocked = tabTrackers,
            currentTabPopupsSuppressed = tabPopups
        )
    }

    fun clearStats() {
        totalAds.set(0)
        totalTrackers.set(0)
        totalPopups.set(0)
        totalRedirects.set(0)
        tabDeduplicationCache.clear()
        _recentEvents.value = emptyList()
        _stats.value = BlockingStats()
    }

    companion object {
        val instance by lazy { BlockingEventRepository() }
    }
}
