package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blocking.BlockingEventRepository
import com.example.blocking.BlockingStats
import com.example.blocking.FilterEngine
import com.example.blocking.FilterRule
import com.example.blocking.FilterUpdateStatus
import com.example.blocking.SiteExceptionManager
import com.example.browser.BrowserTab
import com.example.browser.NavigationHandler
import com.example.browser.TabManager
import com.example.browser.UrlUtils
import com.example.compatibility.DiagnosticReport
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import com.example.data.BookmarkRepository
import com.example.data.BrowserPreferences
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val bookmarkRepository = BookmarkRepository(db.bookmarkDao())
    val historyRepository = HistoryRepository(db.historyDao())
    val settingsRepository = SettingsRepository(application)
    val filterEngine = FilterEngine.getInstance(application)
    val blockingRepo = BlockingEventRepository.instance
    val siteExceptionManager = SiteExceptionManager.instance

    val tabManager = TabManager(application)

    val tabs: StateFlow<List<BrowserTab>> = tabManager.tabs
    val activeTabId: StateFlow<String?> = tabManager.activeTabId

    val blockingStats: StateFlow<BlockingStats> = blockingRepo.stats
    val filterStatus: StateFlow<FilterUpdateStatus> = filterEngine.status

    val preferences: StateFlow<BrowserPreferences> = settingsRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BrowserPreferences()
    )

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history: StateFlow<List<HistoryEntity>> = historyRepository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _isCurrentBookmarked = MutableStateFlow(false)
    val isCurrentBookmarked: StateFlow<Boolean> = _isCurrentBookmarked.asStateFlow()

    init {
        // Initialize filter engine
        viewModelScope.launch {
            filterEngine.initialize()
        }

        // Create default starting tab
        val defaultTab = tabManager.createTab("https://www.google.com", isPrivate = false)
        attachDelegatesToTab(defaultTab)
    }

    private fun attachDelegatesToTab(tab: BrowserTab) {
        val navHandler = NavigationHandler(
            tabId = tab.id,
            isPrivate = tab.isPrivate,
            tabManager = tabManager,
            historyRepository = historyRepository,
            onDiagnosticError = { report ->
                _diagnosticReport.value = report
            },
            scope = viewModelScope
        )
        navHandler.attachDelegates(tab.session)
    }

    fun loadUrl(input: String) {
        _diagnosticReport.value = null
        val active = tabManager.activeTab ?: return
        val searchEngineUrl = preferences.value.getSearchUrl("%s")
        val resolved = UrlUtils.resolveInput(input, searchEngineUrl)

        active.session.loadUri(resolved)
        checkIfCurrentIsBookmarked(resolved)
    }

    fun goBack() {
        tabManager.activeTab?.session?.goBack()
    }

    fun goForward() {
        tabManager.activeTab?.session?.goForward()
    }

    fun reload() {
        _diagnosticReport.value = null
        tabManager.activeTab?.session?.reload()
    }

    fun stop() {
        tabManager.activeTab?.session?.stop()
    }

    fun goHome() {
        loadUrl(preferences.value.homePageUrl)
    }

    fun openNewTab(url: String = "https://www.google.com", isPrivate: Boolean = false) {
        val tab = tabManager.createTab(url, isPrivate)
        attachDelegatesToTab(tab)
    }

    fun selectTab(tabId: String) {
        _diagnosticReport.value = null
        tabManager.selectTab(tabId)
        tabManager.activeTab?.let { checkIfCurrentIsBookmarked(it.url) }
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
    }

    fun closeAllTabs(isPrivateOnly: Boolean = false) {
        tabManager.closeAllTabs(isPrivateOnly)
    }

    fun toggleBookmark() {
        val active = tabManager.activeTab ?: return
        val url = active.url
        val title = active.title.ifBlank { url }

        viewModelScope.launch(Dispatchers.IO) {
            if (_isCurrentBookmarked.value) {
                bookmarkRepository.removeBookmarkByUrl(url)
                _isCurrentBookmarked.value = false
            } else {
                bookmarkRepository.addBookmark(title, url)
                _isCurrentBookmarked.value = true
            }
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkRepository.removeBookmark(id)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteHistory(id)
        }
    }

    fun checkIfCurrentIsBookmarked(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isCurrentBookmarked.value = bookmarkRepository.isBookmarked(url)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.clearHistory()
        }
    }

    fun toggleSiteException(domain: String) {
        val currentDisabled = siteExceptionManager.isAdBlockingDisabled(domain)
        siteExceptionManager.setAdBlockingDisabled(domain, !currentDisabled)
        reload()
    }

    fun isSiteAdBlockingDisabled(domain: String): Boolean {
        return siteExceptionManager.isAdBlockingDisabled(domain)
    }

    fun retryNavigation() {
        _diagnosticReport.value = null
        reload()
    }

    fun allowCurrentSiteAndRetry() {
        val report = _diagnosticReport.value
        if (report != null) {
            siteExceptionManager.setAdBlockingDisabled(report.domain, true)
        }
        _diagnosticReport.value = null
        reload()
    }

    fun dismissDiagnostic() {
        _diagnosticReport.value = null
    }

    fun triggerFilterUpdate() {
        viewModelScope.launch {
            filterEngine.updateFilterList()
        }
    }

    fun clearAllBrowsingData() {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.clearHistory()
            blockingRepo.clearStats()
            // Close private tabs
            tabManager.closeAllTabs(isPrivateOnly = true)
        }
    }
}
