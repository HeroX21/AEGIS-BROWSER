package com.example.compatibility

import com.example.blocking.FilterRule

enum class FailureCategory(val displayName: String, val isWarning: Boolean) {
    BLOCKED_BY_FILTER("Resource Blocked by Shield", false),
    NETWORK_UNAVAILABLE("Network Offline", false),
    DNS_FAILURE("DNS Resolution Failure", false),
    CONNECTION_FAILURE("Connection Refused / Reset", false),
    SSL_CERTIFICATE_ERROR("Security / SSL Failure", true),
    PERMISSION_DENIED("Browser Permission Denied", false),
    SUSPECTED_ANTI_ADBLOCK("Potential Site Protection / Anti-Adblock", true),
    UNKNOWN_FAILURE("Navigation Issue", false)
}

data class DiagnosticReport(
    val category: FailureCategory,
    val domain: String,
    val summary: String,
    val technicalDetails: String,
    val suggestion: String,
    val canRetry: Boolean = true,
    val canAllowSite: Boolean = true
)

object DiagnosticClassifier {

    fun classify(
        geckoErrorCategory: Int?,
        errorDetail: String?,
        currentUrl: String,
        recentBlockedCountOnDomain: Int = 0
    ): DiagnosticReport {
        val domain = FilterRule.extractHost(currentUrl)
        val detailLower = (errorDetail ?: "").lowercase()

        // 1. SSL / TLS Security errors
        if (detailLower.contains("cert") || detailLower.contains("ssl") || detailLower.contains("security")) {
            return DiagnosticReport(
                category = FailureCategory.SSL_CERTIFICATE_ERROR,
                domain = domain,
                summary = "Secure connection to $domain could not be verified.",
                technicalDetails = "TLS certificate validation failed: ${sanitizeDetail(errorDetail)}",
                suggestion = "The site's security certificate is invalid or untrusted. For your safety, do not proceed if exchanging personal data.",
                canAllowSite = false
            )
        }

        // 2. DNS resolution errors
        if (detailLower.contains("dns") || detailLower.contains("host_not_found") || detailLower.contains("unknown host")) {
            return DiagnosticReport(
                category = FailureCategory.DNS_FAILURE,
                domain = domain,
                summary = "Server address for $domain could not be found.",
                technicalDetails = "DNS lookup yielded no records: ${sanitizeDetail(errorDetail)}",
                suggestion = "Check that the URL is spelled correctly or that your internet connection has access to the DNS resolver.",
                canAllowSite = false
            )
        }

        // 3. Network offline
        if (detailLower.contains("network_offline") || detailLower.contains("no internet") || detailLower.contains("unreachable")) {
            return DiagnosticReport(
                category = FailureCategory.NETWORK_UNAVAILABLE,
                domain = domain,
                summary = "No internet connection detected.",
                technicalDetails = "Device network interface is offline.",
                suggestion = "Please check your Wi-Fi or mobile data connection and retry.",
                canAllowSite = false
            )
        }

        // 4. Connection refused / timeout
        if (detailLower.contains("connection_refused") || detailLower.contains("reset")) {
            return DiagnosticReport(
                category = FailureCategory.CONNECTION_FAILURE,
                domain = domain,
                summary = "Connection to $domain was refused or reset.",
                technicalDetails = sanitizeDetail(errorDetail),
                suggestion = "The remote server may be temporarily down or undergoing maintenance.",
                canAllowSite = true
            )
        }

        // 5. Suspected Anti-Adblock detection
        // Distinguish from ordinary timeouts: only flag as anti-adblock if site had ad blocking active
        // and explicit anti-adblock keyword cues appear or site is known to gate behind verified ad payloads
        if (recentBlockedCountOnDomain > 0 &&
            (detailLower.contains("adblock") || detailLower.contains("disable_adblock") || detailLower.contains("whitelist") || detailLower.contains("verification"))
        ) {
            return DiagnosticReport(
                category = FailureCategory.SUSPECTED_ANTI_ADBLOCK,
                domain = domain,
                summary = "This site appears to restrict access when ad blocking is active.",
                technicalDetails = "Site scripts detected missing advertising resources ($recentBlockedCountOnDomain elements suppressed).",
                suggestion = "Temporarily allowing ads on $domain may restore normal site layout. You can re-enable blocking anytime from the Shield menu.",
                canAllowSite = true
            )
        }

        // 6. Generic or unknown error
        return DiagnosticReport(
            category = FailureCategory.UNKNOWN_FAILURE,
            domain = domain,
            summary = "Unable to load page on $domain.",
            technicalDetails = sanitizeDetail(errorDetail),
            suggestion = "Try reloading the page. If the issue persists, check site whitelist settings.",
            canAllowSite = true
        )
    }

    private fun sanitizeDetail(raw: String?): String {
        if (raw.isNullOrBlank()) return "No specific error code returned."
        // Strip out query params or potential tokens from error strings
        return raw.replace(Regex("[?&][^=]+=[^&]*"), "[param]")
    }
}
