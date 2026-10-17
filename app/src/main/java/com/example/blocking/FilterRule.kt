package com.example.blocking

enum class ResourceType {
    DOCUMENT,
    SUBDOCUMENT,
    SCRIPT,
    IMAGE,
    STYLESHEET,
    XMLHTTPREQUEST,
    FONT,
    MEDIA,
    WEBSOCKET,
    OTHER
}

enum class RuleType {
    BLOCK,
    EXCEPTION
}

data class FilterRule(
    val rawRule: String,
    val ruleType: RuleType = RuleType.BLOCK,
    val pattern: String,
    val isDomainAnchor: Boolean = false,
    val isExactMatch: Boolean = false,
    val isRegex: Boolean = false,
    val regex: Regex? = null,
    val domainsIncluded: Set<String> = emptySet(),
    val domainsExcluded: Set<String> = emptySet(),
    val resourceTypes: Set<ResourceType> = emptySet(),
    val thirdPartyOnly: Boolean? = null
) {
    fun matches(targetUrl: String, sourceDomain: String?, resourceType: ResourceType): Boolean {
        // Domain inclusion / exclusion checks
        if (sourceDomain != null) {
            val lowerSource = sourceDomain.lowercase()
            if (domainsExcluded.any { lowerSource == it || lowerSource.endsWith(".$it") }) {
                return false
            }
            if (domainsIncluded.isNotEmpty() && !domainsIncluded.any { lowerSource == it || lowerSource.endsWith(".$it") }) {
                return false
            }
        }

        // Resource type check
        if (resourceTypes.isNotEmpty() && !resourceTypes.contains(resourceType)) {
            return false
        }

        // Third party check
        if (thirdPartyOnly == true && sourceDomain != null) {
            val targetHost = extractHost(targetUrl)
            if (isSameParty(targetHost, sourceDomain)) {
                return false
            }
        }

        // Pattern matching
        if (isRegex && regex != null) {
            return regex.containsMatchIn(targetUrl)
        }

        if (isDomainAnchor) {
            val targetHost = extractHost(targetUrl).lowercase()
            val lowerPattern = pattern.lowercase()
            return targetHost == lowerPattern || targetHost.endsWith(".$lowerPattern")
        }

        return targetUrl.contains(pattern, ignoreCase = true)
    }

    companion object {
        fun extractHost(url: String): String {
            return try {
                val clean = url.trim()
                val withoutProtocol = if (clean.contains("://")) {
                    clean.substringAfter("://")
                } else clean
                withoutProtocol.substringBefore("/").substringBefore("?").substringBefore(":")
            } catch (e: Exception) {
                ""
            }
        }

        fun isSameParty(host1: String, host2: String): Boolean {
            val h1 = host1.lowercase()
            val h2 = host2.lowercase()
            if (h1 == h2) return true
            val parts1 = h1.split(".")
            val parts2 = h2.split(".")
            if (parts1.size >= 2 && parts2.size >= 2) {
                val base1 = parts1.takeLast(2).joinToString(".")
                val base2 = parts2.takeLast(2).joinToString(".")
                return base1 == base2
            }
            return false
        }
    }
}
