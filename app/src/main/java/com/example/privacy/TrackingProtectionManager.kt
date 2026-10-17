package com.example.privacy

import com.example.blocking.BlockCategory
import com.example.blocking.BlockingEventRepository
import com.example.blocking.FilterRule
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoSession

class TrackingProtectionManager(
    private val blockingEventRepository: BlockingEventRepository = BlockingEventRepository.instance
) {

    fun createContentBlockingDelegate(tabId: String): ContentBlocking.Delegate {
        return object : ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
                val blockedUri = event.uri
                val domain = FilterRule.extractHost(blockedUri)
                blockingEventRepository.recordEvent(
                    tabId = tabId,
                    category = BlockCategory.TRACKER,
                    url = blockedUri,
                    domain = domain
                )
            }
        }
    }

    companion object {
        val instance by lazy { TrackingProtectionManager() }
    }
}
