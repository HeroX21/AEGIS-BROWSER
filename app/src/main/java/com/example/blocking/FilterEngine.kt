package com.example.blocking

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class FilterUpdateStatus(
    val version: String = "2026.10.1",
    val ruleCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isUpdating: Boolean = false,
    val lastError: String? = null
)

class FilterEngine private constructor(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Domain anchored block rules for O(1) domain lookup
    private val domainBlockRules = ConcurrentHashMap<String, MutableList<FilterRule>>()
    // General path / substring / regex block rules
    private val generalBlockRules = ArrayList<FilterRule>()

    // Exception rules (whitelist overrides)
    private val domainExceptionRules = ConcurrentHashMap<String, MutableList<FilterRule>>()
    private val generalExceptionRules = ArrayList<FilterRule>()

    private val isLoaded = AtomicBoolean(false)
    private val _status = MutableStateFlow(FilterUpdateStatus())
    val status: StateFlow<FilterUpdateStatus> = _status.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isLoaded.get()) return@withContext

        // Check if we have a cached updated list on disk; if not, read from assets
        val cachedFile = File(context.filesDir, CACHED_FILTER_FILE)
        val content = if (cachedFile.exists() && cachedFile.length() > 0) {
            try {
                cachedFile.readText()
            } catch (e: Exception) {
                readAssetFilters()
            }
        } else {
            readAssetFilters()
        }

        applyRules(content)
        isLoaded.set(true)
    }

    private fun readAssetFilters(): String {
        return try {
            context.assets.open("filters/adguard_base.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    @Synchronized
    private fun applyRules(filterContent: String) {
        val parsedRules = FilterParser.parseFilterList(filterContent)

        domainBlockRules.clear()
        generalBlockRules.clear()
        domainExceptionRules.clear()
        generalExceptionRules.clear()

        for (rule in parsedRules) {
            if (rule.ruleType == RuleType.EXCEPTION) {
                if (rule.isDomainAnchor) {
                    domainExceptionRules.getOrPut(rule.pattern.lowercase()) { ArrayList() }.add(rule)
                } else {
                    generalExceptionRules.add(rule)
                }
            } else {
                if (rule.isDomainAnchor) {
                    domainBlockRules.getOrPut(rule.pattern.lowercase()) { ArrayList() }.add(rule)
                } else {
                    generalBlockRules.add(rule)
                }
            }
        }

        _status.value = _status.value.copy(
            ruleCount = parsedRules.size,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun shouldBlock(requestUrl: String, sourceUrl: String?, resourceType: ResourceType = ResourceType.OTHER): Boolean {
        if (requestUrl.isBlank()) return false

        val sourceDomain = sourceUrl?.let { FilterRule.extractHost(it) }
        val targetDomain = FilterRule.extractHost(requestUrl).lowercase()

        // 1. Check EXCEPTION rules first (@@ rules take absolute precedence)
        if (matchesDomainRules(domainExceptionRules, targetDomain, requestUrl, sourceDomain, resourceType)) {
            return false
        }
        for (rule in generalExceptionRules) {
            if (rule.matches(requestUrl, sourceDomain, resourceType)) {
                return false
            }
        }

        // 2. Check BLOCK rules
        if (matchesDomainRules(domainBlockRules, targetDomain, requestUrl, sourceDomain, resourceType)) {
            return true
        }

        for (rule in generalBlockRules) {
            if (rule.matches(requestUrl, sourceDomain, resourceType)) {
                return true
            }
        }

        return false
    }

    private fun matchesDomainRules(
        ruleMap: Map<String, List<FilterRule>>,
        targetDomain: String,
        targetUrl: String,
        sourceDomain: String?,
        resourceType: ResourceType
    ): Boolean {
        if (targetDomain.isEmpty()) return false

        // Check exact domain
        ruleMap[targetDomain]?.let { rules ->
            for (rule in rules) {
                if (rule.matches(targetUrl, sourceDomain, resourceType)) return true
            }
        }

        // Check parent domains (e.g. ad.example.com -> example.com)
        var parent = targetDomain
        while (parent.contains(".")) {
            parent = parent.substringAfter(".")
            ruleMap[parent]?.let { rules ->
                for (rule in rules) {
                    if (rule.matches(targetUrl, sourceDomain, resourceType)) return true
                }
            }
        }
        return false
    }

    suspend fun updateFilterList(customUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        _status.value = _status.value.copy(isUpdating = true, lastError = null)
        val url = customUrl ?: DEFAULT_FILTER_UPDATE_URL

        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                _status.value = _status.value.copy(
                    isUpdating = false,
                    lastError = "Server returned HTTP ${response.code}"
                )
                return@withContext false
            }

            val body = response.body ?: throw IllegalStateException("Empty response body")
            val contentLength = body.contentLength()
            if (contentLength > MAX_FILTER_SIZE_BYTES) {
                _status.value = _status.value.copy(
                    isUpdating = false,
                    lastError = "Filter file exceeds size limit (>${MAX_FILTER_SIZE_BYTES / 1024 / 1024}MB)"
                )
                return@withContext false
            }

            val content = body.string()
            if (content.isBlank() || !content.contains("!")) {
                _status.value = _status.value.copy(
                    isUpdating = false,
                    lastError = "Invalid filter file format"
                )
                return@withContext false
            }

            // Write to temp file first to ensure atomic swap
            val tempFile = File(context.filesDir, "filters_temp.txt")
            FileOutputStream(tempFile).use { out ->
                out.write(content.toByteArray())
            }

            val targetFile = File(context.filesDir, CACHED_FILTER_FILE)
            if (tempFile.renameTo(targetFile)) {
                applyRules(content)
                _status.value = _status.value.copy(
                    isUpdating = false,
                    lastUpdated = System.currentTimeMillis(),
                    lastError = null
                )
                return@withContext true
            } else {
                tempFile.delete()
                _status.value = _status.value.copy(
                    isUpdating = false,
                    lastError = "Failed to save filter file"
                )
                return@withContext false
            }
        } catch (e: Exception) {
            _status.value = _status.value.copy(
                isUpdating = false,
                lastError = e.message ?: "Update network error"
            )
            return@withContext false
        }
    }

    companion object {
        private const val CACHED_FILTER_FILE = "adguard_base_cached.txt"
        private const val MAX_FILTER_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB bound to prevent memory exhaustion
        private const val DEFAULT_FILTER_UPDATE_URL =
            "https://raw.githubusercontent.com/AdguardTeam/AdguardFilters/master/BaseFilter/sections/adservers.txt"

        @Volatile
        private var instance: FilterEngine? = null

        fun getInstance(context: Context): FilterEngine {
            return instance ?: synchronized(this) {
                instance ?: FilterEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
