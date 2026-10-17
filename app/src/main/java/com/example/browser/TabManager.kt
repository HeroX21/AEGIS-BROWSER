package com.example.browser

import android.content.Context
import com.example.blocking.BlockingEventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TabManager(
    private val context: Context,
    val sessionManager: BrowserSessionManager = BrowserSessionManager(context),
    private val blockingEventRepository: BlockingEventRepository = BlockingEventRepository.instance
) {

    private val _tabs = MutableStateFlow<List<BrowserTab>>(emptyList())
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val activeTab: BrowserTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    fun createTab(initialUrl: String = "https://www.google.com", isPrivate: Boolean = false): BrowserTab {
        val tabId = UUID.randomUUID().toString()
        val session = sessionManager.createSession(tabId = tabId, isPrivate = isPrivate)

        val tab = BrowserTab(
            id = tabId,
            session = session,
            isPrivate = isPrivate,
            url = initialUrl,
            title = if (isPrivate) "Private Tab" else "New Tab"
        )

        val currentList = _tabs.value.toMutableList()
        currentList.add(tab)
        _tabs.value = currentList
        _activeTabId.value = tabId

        if (initialUrl.isNotBlank() && initialUrl != "about:blank") {
            session.loadUri(initialUrl)
        }

        return tab
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == tabId }
        if (index == -1) return

        currentList.removeAt(index)
        sessionManager.closeSession(tabId)
        blockingEventRepository.onTabClosed(tabId)

        _tabs.value = currentList

        // Switch to adjacent tab if active was closed
        if (_activeTabId.value == tabId) {
            val nextTab = currentList.getOrNull(index.coerceAtMost(currentList.size - 1))
            _activeTabId.value = nextTab?.id
        }

        // If all tabs closed, open a default tab
        if (currentList.isEmpty()) {
            createTab("https://www.google.com", isPrivate = false)
        }
    }

    fun closeAllTabs(isPrivateOnly: Boolean = false) {
        val currentList = _tabs.value.toMutableList()
        val iterator = currentList.iterator()

        while (iterator.hasNext()) {
            val tab = iterator.next()
            if (!isPrivateOnly || tab.isPrivate) {
                sessionManager.closeSession(tab.id)
                blockingEventRepository.onTabClosed(tab.id)
                iterator.remove()
            }
        }

        _tabs.value = currentList
        if (currentList.isEmpty()) {
            createTab("https://www.google.com", isPrivate = false)
        } else {
            _activeTabId.value = currentList.first().id
        }
    }

    fun updateTabState(
        tabId: String,
        url: String? = null,
        title: String? = null,
        isLoading: Boolean? = null,
        progress: Int? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null,
        isSecure: Boolean? = null
    ) {
        val currentList = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = url ?: tab.url,
                    title = title ?: tab.title,
                    isLoading = isLoading ?: tab.isLoading,
                    progress = progress ?: tab.progress,
                    canGoBack = canGoBack ?: tab.canGoBack,
                    canGoForward = canGoForward ?: tab.canGoForward,
                    isSecure = isSecure ?: tab.isSecure
                )
            } else {
                tab
            }
        }
        _tabs.value = currentList
    }
}
