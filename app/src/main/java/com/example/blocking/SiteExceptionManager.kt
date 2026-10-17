package com.example.blocking

import java.util.concurrent.ConcurrentHashMap

data class SiteException(
    val domain: String,
    val adBlockingDisabled: Boolean = true,
    val popupAllowed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class SiteExceptionManager {

    private val exceptions = ConcurrentHashMap<String, SiteException>()

    fun isAdBlockingDisabled(urlOrDomain: String): Boolean {
        val host = FilterRule.extractHost(urlOrDomain).lowercase()
        if (host.isEmpty()) return false
        return getException(host)?.adBlockingDisabled == true
    }

    fun isPopupAllowed(urlOrDomain: String): Boolean {
        val host = FilterRule.extractHost(urlOrDomain).lowercase()
        if (host.isEmpty()) return false
        return getException(host)?.popupAllowed == true
    }

    fun setAdBlockingDisabled(domain: String, disabled: Boolean) {
        val cleanDomain = FilterRule.extractHost(domain).lowercase()
        if (cleanDomain.isBlank()) return
        val current = exceptions[cleanDomain]
        if (current != null) {
            exceptions[cleanDomain] = current.copy(adBlockingDisabled = disabled)
        } else {
            exceptions[cleanDomain] = SiteException(domain = cleanDomain, adBlockingDisabled = disabled)
        }
    }

    fun setPopupAllowed(domain: String, allowed: Boolean) {
        val cleanDomain = FilterRule.extractHost(domain).lowercase()
        if (cleanDomain.isBlank()) return
        val current = exceptions[cleanDomain]
        if (current != null) {
            exceptions[cleanDomain] = current.copy(popupAllowed = allowed)
        } else {
            exceptions[cleanDomain] = SiteException(domain = cleanDomain, popupAllowed = allowed)
        }
    }

    fun removeException(domain: String) {
        val cleanDomain = FilterRule.extractHost(domain).lowercase()
        exceptions.remove(cleanDomain)
    }

    fun getAllExceptions(): List<SiteException> {
        return exceptions.values.toList()
    }

    private fun getException(host: String): SiteException? {
        exceptions[host]?.let { return it }
        var parent = host
        while (parent.contains(".")) {
            parent = parent.substringAfter(".")
            exceptions[parent]?.let { return it }
        }
        return null
    }

    companion object {
        val instance by lazy { SiteExceptionManager() }
    }
}
