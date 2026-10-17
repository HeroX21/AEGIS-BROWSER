package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blocking.FilterRule
import com.example.ui.components.BookmarksHistoryDialog
import com.example.ui.components.BrowserTopBar
import com.example.ui.components.DiagnosticBanner
import com.example.ui.components.PrivacyShieldSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TabSwitcherSheet
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkVoid
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.PrivatePurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.mozilla.geckoview.GeckoView

/**
 * Main browser screen hosting BrowserTopBar, the GeckoView rendering area,
 * bottom navigation controls, and overlay dialogs.
 */
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val blockingStats by viewModel.blockingStats.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val diagnosticReport by viewModel.diagnosticReport.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentBookmarked.collectAsStateWithLifecycle()

    val activeTab = tabs.find { it.id == activeTabId }

    // Modal Sheet & Dialog visibility states
    var showShieldSheet by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showLibraryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkVoid)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Browser Top Bar with address input, search, and navigation controls
            BrowserTopBar(
                currentUrl = activeTab?.url ?: "about:blank",
                isLoading = activeTab?.isLoading == true,
                progress = activeTab?.progress ?: 0,
                isSecure = activeTab?.isSecure == true,
                isPrivate = activeTab?.isPrivate == true,
                isSiteWhitelisted = activeTab?.url?.let {
                    viewModel.isSiteAdBlockingDisabled(FilterRule.extractHost(it))
                } == true,
                blockedCount = blockingStats.currentTabAdsBlocked + blockingStats.currentTabTrackersBlocked,
                isBookmarked = isBookmarked,
                onNavigate = { viewModel.loadUrl(it) },
                onReload = { viewModel.reload() },
                onStop = { viewModel.stop() },
                onShieldClick = { showShieldSheet = true },
                onNewTab = { isPrivate -> viewModel.openNewTab(isPrivate = isPrivate) },
                onToggleBookmark = { viewModel.toggleBookmark() },
                onOpenLibrary = { showLibraryDialog = true },
                onOpenSettings = { showSettingsDialog = true }
            )

            // GeckoView Web Rendering Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab != null) {
                    AndroidView(
                        factory = { context ->
                            GeckoView(context).apply {
                                viewModel.tabManager.sessionManager.attachToView(this, activeTab.session)
                            }
                        },
                        update = { geckoView ->
                            viewModel.tabManager.sessionManager.attachToView(geckoView, activeTab.session)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("geckoview_surface")
                    )
                }

                // Diagnostic overlay if anti-adblock or network error occurs
                diagnosticReport?.let { report ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        DiagnosticBanner(
                            report = report,
                            onRetry = { viewModel.retryNavigation() },
                            onAllowSite = { viewModel.allowCurrentSiteAndRetry() },
                            onDismiss = { viewModel.dismissDiagnostic() }
                        )
                    }
                }
            }

            // Bottom Navigation Toolbar (with Navigation Bar Insets Padding)
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(width = 0.5.dp, color = DarkSurfaceBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(
                        onClick = { viewModel.goBack() },
                        enabled = activeTab?.canGoBack == true,
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (activeTab?.canGoBack == true) TextPrimary else TextMuted
                        )
                    }

                    // Forward button
                    IconButton(
                        onClick = { viewModel.goForward() },
                        enabled = activeTab?.canGoForward == true,
                        modifier = Modifier.testTag("nav_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (activeTab?.canGoForward == true) TextPrimary else TextMuted
                        )
                    }

                    // Home button
                    IconButton(
                        onClick = { viewModel.goHome() },
                        modifier = Modifier.testTag("nav_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = TextSecondary
                        )
                    }

                    // Tab Counter Pill Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .border(
                                width = 1.5.dp,
                                color = if (activeTab?.isPrivate == true) PrivatePurple else ElectricCyan,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { showTabSwitcher = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("tab_switcher_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs.size.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTab?.isPrivate == true) PrivatePurple else ElectricCyan
                        )
                    }
                }
            }
        }

        // Modals & Sheets
        if (showShieldSheet && activeTab != null) {
            val isWhitelisted = viewModel.isSiteAdBlockingDisabled(FilterRule.extractHost(activeTab.url))
            PrivacyShieldSheet(
                currentUrl = activeTab.url,
                isSecure = activeTab.isSecure,
                blockingStats = blockingStats,
                filterStatus = filterStatus,
                isSiteWhitelisted = isWhitelisted,
                onToggleSiteException = { viewModel.toggleSiteException(it) },
                onTriggerFilterUpdate = { viewModel.triggerFilterUpdate() },
                onDismiss = { showShieldSheet = false }
            )
        }

        if (showTabSwitcher) {
            TabSwitcherSheet(
                tabs = tabs,
                activeTabId = activeTabId,
                onSelectTab = { viewModel.selectTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onNewTab = { isPrivate -> viewModel.openNewTab(isPrivate = isPrivate) },
                onCloseAll = { isPrivateOnly -> viewModel.closeAllTabs(isPrivateOnly) },
                onDismiss = { showTabSwitcher = false }
            )
        }

        if (showLibraryDialog) {
            BookmarksHistoryDialog(
                bookmarks = bookmarks,
                history = history,
                onSelectUrl = { viewModel.loadUrl(it) },
                onDeleteBookmark = { viewModel.removeBookmark(it) },
                onDeleteHistory = { viewModel.deleteHistory(it) },
                onClearHistory = { viewModel.clearHistory() },
                onDismiss = { showLibraryDialog = false }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                preferences = preferences,
                settingsRepository = viewModel.settingsRepository,
                filterStatus = filterStatus,
                onTriggerFilterUpdate = { viewModel.triggerFilterUpdate() },
                onClearAllData = { viewModel.clearAllBrowsingData() },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
