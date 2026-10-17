package com.example.browser

import com.example.blocking.BlockingEventRepository
import com.example.blocking.PopupManager
import com.example.blocking.SiteExceptionManager
import com.example.compatibility.DiagnosticClassifier
import com.example.compatibility.DiagnosticReport
import com.example.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

class NavigationHandler(
    private val tabId: String,
    private val isPrivate: Boolean,
    private val tabManager: TabManager,
    private val historyRepository: HistoryRepository,
    private val popupManager: PopupManager = PopupManager.instance,
    private val siteExceptionManager: SiteExceptionManager = SiteExceptionManager.instance,
    private val blockingEventRepository: BlockingEventRepository = BlockingEventRepository.instance,
    private val onDiagnosticError: (DiagnosticReport) -> Unit,
    private val scope: CoroutineScope
) {

    fun attachDelegates(session: GeckoSession) {
        session.navigationDelegate = createNavigationDelegate()
        session.progressDelegate = createProgressDelegate()
    }

    private fun createNavigationDelegate(): GeckoSession.NavigationDelegate {
        return object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                tabManager.updateTabState(tabId, canGoBack = canGoBack)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                tabManager.updateTabState(tabId, canGoForward = canGoForward)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                val newUrl = url ?: return
                tabManager.updateTabState(tabId, url = newUrl)
                blockingEventRepository.onTabNavigated(tabId)

                // Record history only if NOT in private browsing mode
                if (!isPrivate) {
                    scope.launch(Dispatchers.IO) {
                        historyRepository.recordVisit(
                            title = tabManager.activeTab?.title ?: newUrl,
                            url = newUrl
                        )
                    }
                }
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                val currentUrl = tabManager.activeTab?.url
                val allowPopup = popupManager.shouldAllowNewWindow(
                    tabId = tabId,
                    sourceUrl = currentUrl,
                    targetUrl = uri,
                    hasUserGesture = false
                )

                if (allowPopup) {
                    // Open in new tab
                    val newTab = tabManager.createTab(initialUrl = uri, isPrivate = isPrivate)
                    return GeckoResult.fromValue(newTab.session)
                }

                // Suppress popup
                return null
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: org.mozilla.geckoview.WebRequestError
            ): GeckoResult<String>? {
                val errorDetail = "Error code: ${error.code}, Category: ${error.category}"
                val currentUrl = uri ?: tabManager.activeTab?.url ?: ""
                val recentBlocked = blockingEventRepository.stats.value.currentTabAdsBlocked

                val report = DiagnosticClassifier.classify(
                    geckoErrorCategory = error.category,
                    errorDetail = errorDetail,
                    currentUrl = currentUrl,
                    recentBlockedCountOnDomain = recentBlocked
                )

                onDiagnosticError(report)
                return null
            }
        }
    }

    private fun createProgressDelegate(): GeckoSession.ProgressDelegate {
        return object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                tabManager.updateTabState(tabId, url = url, isLoading = true, progress = 10)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tabManager.updateTabState(tabId, isLoading = false, progress = 100)
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                tabManager.updateTabState(tabId, progress = progress)
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                tabManager.updateTabState(tabId, isSecure = securityInfo.isSecure)
            }
        }
    }
}
