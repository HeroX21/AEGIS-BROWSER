package com.example

import com.example.blocking.BlockCategory
import com.example.blocking.BlockingEventRepository
import com.example.blocking.FilterParser
import com.example.blocking.FilterRule
import com.example.blocking.PopupManager
import com.example.blocking.ResourceType
import com.example.blocking.RuleType
import com.example.blocking.SiteExceptionManager
import com.example.browser.UrlUtils
import com.example.compatibility.DiagnosticClassifier
import com.example.compatibility.FailureCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AegisBrowserTests {

    // --- 1. URL & Search Query Tests ---
    @Test
    fun testUrlResolution_exactHttps() {
        val result = UrlUtils.resolveInput("https://example.org/path", "https://www.google.com/search?q=%s")
        assertEquals("https://example.org/path", result)
    }

    @Test
    fun testUrlResolution_domainWithoutProtocol() {
        val result = UrlUtils.resolveInput("wikipedia.org", "https://www.google.com/search?q=%s")
        assertEquals("https://wikipedia.org", result)
    }

    @Test
    fun testUrlResolution_searchQuery() {
        val result = UrlUtils.resolveInput("privacy respecting browser", "https://www.google.com/search?q=%s")
        assertEquals("https://www.google.com/search?q=privacy+respecting+browser", result)
    }

    @Test
    fun testUrlResolution_emptyString() {
        val result = UrlUtils.resolveInput("   ", "https://www.google.com/search?q=%s")
        assertEquals("about:blank", result)
    }

    @Test
    fun testExtractHost() {
        assertEquals("example.com", FilterRule.extractHost("https://example.com/test?query=1"))
        assertEquals("sub.domain.org", FilterRule.extractHost("http://sub.domain.org:8080/"))
    }

    // --- 2. Filter Rule Parsing & Matching Tests ---
    @Test
    fun testFilterParser_domainAnchor() {
        val rule = FilterParser.parseLine("||doubleclick.net^\$third-party")
        assertNotNull(rule)
        assertEquals("doubleclick.net", rule!!.pattern)
        assertTrue(rule.isDomainAnchor)
        assertEquals(true, rule.thirdPartyOnly)
        assertEquals(RuleType.BLOCK, rule.ruleType)

        // Matching test
        assertTrue(rule.matches("https://ad.doubleclick.net/ad.js", "example.com", ResourceType.SCRIPT))
        // Same-party should not match when third-party only is specified
        assertFalse(rule.matches("https://ad.doubleclick.net/ad.js", "doubleclick.net", ResourceType.SCRIPT))
    }

    @Test
    fun testFilterParser_exceptionPrecedence() {
        val blockRule = FilterParser.parseLine("||ads.example.com^")
        val exceptionRule = FilterParser.parseLine("@@||ads.example.com^")

        assertNotNull(blockRule)
        assertNotNull(exceptionRule)
        assertEquals(RuleType.EXCEPTION, exceptionRule!!.ruleType)

        // Block rule matches
        assertTrue(blockRule!!.matches("https://ads.example.com/banner.png", "example.com", ResourceType.IMAGE))
        // Exception rule matches
        assertTrue(exceptionRule.matches("https://ads.example.com/banner.png", "example.com", ResourceType.IMAGE))
    }

    @Test
    fun testFilterParser_commentsIgnored() {
        assertNull(FilterParser.parseLine("! This is a comment"))
        assertNull(FilterParser.parseLine("[Adblock Plus 2.0]"))
        assertNull(FilterParser.parseLine(""))
    }

    // --- 3. Diagnostic Classifier Tests ---
    @Test
    fun testDiagnostic_sslFailure() {
        val report = DiagnosticClassifier.classify(
            geckoErrorCategory = 1,
            errorDetail = "ERROR_SECURITY_SSL_CERT_INVALID",
            currentUrl = "https://untrusted.com"
        )
        assertEquals(FailureCategory.SSL_CERTIFICATE_ERROR, report.category)
        assertTrue(report.category.isWarning)
        assertFalse(report.canAllowSite)
    }

    @Test
    fun testDiagnostic_dnsFailure() {
        val report = DiagnosticClassifier.classify(
            geckoErrorCategory = 2,
            errorDetail = "ERROR_HOST_NOT_FOUND",
            currentUrl = "https://nonexistent-site-12345.xyz"
        )
        assertEquals(FailureCategory.DNS_FAILURE, report.category)
    }

    @Test
    fun testDiagnostic_antiAdblockDetection() {
        val report = DiagnosticClassifier.classify(
            geckoErrorCategory = 0,
            errorDetail = "Site error: please disable_adblock to view content",
            currentUrl = "https://news-site.com",
            recentBlockedCountOnDomain = 8
        )
        assertEquals(FailureCategory.SUSPECTED_ANTI_ADBLOCK, report.category)
        assertTrue(report.canAllowSite)
    }

    // --- 4. Site Exception Whitelisting Tests ---
    @Test
    fun testSiteExceptionManager() {
        val manager = SiteExceptionManager()
        assertFalse(manager.isAdBlockingDisabled("example.com"))

        manager.setAdBlockingDisabled("example.com", true)
        assertTrue(manager.isAdBlockingDisabled("example.com"))
        assertTrue(manager.isAdBlockingDisabled("sub.example.com"))

        manager.setAdBlockingDisabled("example.com", false)
        assertFalse(manager.isAdBlockingDisabled("example.com"))
    }

    // --- 5. Popup & Redirect Loop Tests ---
    @Test
    fun testPopupSuppression() {
        val exceptionMgr = SiteExceptionManager()
        val blockingRepo = BlockingEventRepository()
        val popupMgr = PopupManager(exceptionMgr, blockingRepo)

        // Without user gesture, unsolicited popup is blocked
        val allowedWithoutGesture = popupMgr.shouldAllowNewWindow(
            tabId = "tab1",
            sourceUrl = "https://source.com",
            targetUrl = "https://ad-popup.com",
            hasUserGesture = false
        )
        assertFalse(allowedWithoutGesture)

        // With user gesture, legitimate new window is allowed
        val allowedWithGesture = popupMgr.shouldAllowNewWindow(
            tabId = "tab1",
            sourceUrl = "https://source.com",
            targetUrl = "https://accounts.google.com",
            hasUserGesture = true
        )
        assertTrue(allowedWithGesture)
    }

    @Test
    fun testMaliciousRedirectLoopDetection() {
        val popupMgr = PopupManager()
        val tabId = "test-tab-loop"

        // Fire 4 rapid redirects: under threshold
        repeat(4) {
            assertFalse(popupMgr.isMaliciousRedirectLoop(tabId, "https://redirect-site.com/step$it"))
        }

        // 6th rapid redirect: exceeds threshold (5 in 2.5s)
        assertTrue(popupMgr.isMaliciousRedirectLoop(tabId, "https://redirect-site.com/loop"))
    }

    // --- 6. Event Deduplication Test ---
    @Test
    fun testEventDeduplication() {
        val repo = BlockingEventRepository()
        val tabId = "tab-dedupe-test"

        // First event recorded
        val firstRecorded = repo.recordEvent(
            tabId = tabId,
            category = BlockCategory.AD,
            url = "https://ad.doubleclick.net/ad.js",
            domain = "doubleclick.net"
        )
        assertTrue(firstRecorded)

        // Duplicate event on same tab should be suppressed
        val duplicateRecorded = repo.recordEvent(
            tabId = tabId,
            category = BlockCategory.AD,
            url = "https://ad.doubleclick.net/ad.js",
            domain = "doubleclick.net"
        )
        assertFalse(duplicateRecorded)

        assertEquals(1, repo.stats.value.totalAdsBlocked)
    }
}
