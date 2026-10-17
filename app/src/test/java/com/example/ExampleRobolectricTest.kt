package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.BookmarkEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aegis Browser", appName)
    }

    @Test
    fun `verify room bookmark persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val dao = db.bookmarkDao()

        val id = dao.insertBookmark(BookmarkEntity(title = "DuckDuckGo", url = "https://duckduckgo.com"))
        assertTrue(id > 0)

        val isSaved = dao.isBookmarked("https://duckduckgo.com")
        assertTrue(isSaved)
    }
}
