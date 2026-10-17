package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "aegis_settings")

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun addBookmark(title: String, url: String) {
        bookmarkDao.insertBookmark(BookmarkEntity(title = title, url = url))
    }

    suspend fun removeBookmark(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun removeBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }
}

class HistoryRepository(private val historyDao: HistoryDao) {
    val history: Flow<List<HistoryEntity>> = historyDao.getRecentHistory()

    suspend fun recordVisit(title: String, url: String) {
        if (url.startsWith("about:") || url.isBlank()) return
        historyDao.insertHistory(HistoryEntity(title = title.ifBlank { url }, url = url))
    }

    suspend fun deleteHistory(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }
}

data class BrowserPreferences(
    val adBlockingEnabled: Boolean = true,
    val trackingProtectionEnabled: Boolean = true,
    val popupProtectionEnabled: Boolean = true,
    val searchEngineName: String = "Google",
    val homePageUrl: String = "https://www.google.com"
) {
    fun getSearchUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return when (searchEngineName) {
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            "Brave" -> "https://search.brave.com/search?q=$encoded"
            "Startpage" -> "https://www.startpage.com/sp/search?query=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }
}

class SettingsRepository(private val context: Context) {

    private val KEY_AD_BLOCKING = booleanPreferencesKey("ad_blocking_enabled")
    private val KEY_TRACKER_PROTECTION = booleanPreferencesKey("tracker_protection_enabled")
    private val KEY_POPUP_PROTECTION = booleanPreferencesKey("popup_protection_enabled")
    private val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
    private val KEY_HOMEPAGE = stringPreferencesKey("home_page_url")

    val preferences: Flow<BrowserPreferences> = context.dataStore.data.map { prefs ->
        BrowserPreferences(
            adBlockingEnabled = prefs[KEY_AD_BLOCKING] ?: true,
            trackingProtectionEnabled = prefs[KEY_TRACKER_PROTECTION] ?: true,
            popupProtectionEnabled = prefs[KEY_POPUP_PROTECTION] ?: true,
            searchEngineName = prefs[KEY_SEARCH_ENGINE] ?: "Google",
            homePageUrl = prefs[KEY_HOMEPAGE] ?: "https://www.google.com"
        )
    }

    suspend fun setAdBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AD_BLOCKING] = enabled }
    }

    suspend fun setTrackingProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TRACKER_PROTECTION] = enabled }
    }

    suspend fun setPopupProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_POPUP_PROTECTION] = enabled }
    }

    suspend fun setSearchEngine(engine: String) {
        context.dataStore.edit { it[KEY_SEARCH_ENGINE] = engine }
    }

    suspend fun setHomePage(url: String) {
        context.dataStore.edit { it[KEY_HOMEPAGE] = url }
    }
}
