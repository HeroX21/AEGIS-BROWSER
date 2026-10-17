package com.example.browser

import android.webkit.URLUtil
import java.net.URI

object UrlUtils {

    fun resolveInput(input: String, searchProviderQueryUrl: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return "about:blank"
        }

        // Scheme check (e.g. http://, https://, about:)
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }

        // Domain-like pattern check (e.g., example.com, sub.example.org/path, localhost:8080)
        val hasWhitespace = trimmed.any { it.isWhitespace() }
        val looksLikeDomain = !hasWhitespace && (
            trimmed.contains(".") && !trimmed.endsWith(".") ||
            trimmed.startsWith("localhost")
        )

        if (looksLikeDomain) {
            return "https://$trimmed"
        }

        // Search query
        val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
        return if (searchProviderQueryUrl.contains("%s")) {
            searchProviderQueryUrl.replace("%s", encoded)
        } else {
            searchProviderQueryUrl + encoded
        }
    }

    fun isValidWebUrl(url: String): Boolean {
        return URLUtil.isValidUrl(url) && (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true))
    }

    fun extractDisplayHost(url: String): String {
        return try {
            val uri = URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
