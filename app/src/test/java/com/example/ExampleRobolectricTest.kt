package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CuratedBooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nanda's service to the book readers", appName)
    }

    @Test
    fun `curated books catalog is loaded and populated`() {
        val books = CuratedBooks.list
        assertTrue("Curated books catalog should not be empty", books.isNotEmpty())
        assertTrue("Contains Pride and Prejudice", books.any { it.title.contains("Pride and Prejudice") })
        assertTrue("Contains Frankenstein", books.any { it.title.contains("Frankenstein") })
        assertTrue("Contains Sherlock Holmes", books.any { it.title.contains("Sherlock Holmes") })
    }

    @Test
    fun `search by title returns matching books`() {
        val results = CuratedBooks.list.filter {
            it.title.contains("Dracula", ignoreCase = true) ||
            it.author.contains("Dracula", ignoreCase = true)
        }
        assertTrue("Expected to find Dracula", results.isNotEmpty())
        assertEquals("Stoker, Bram", results.first().author)
    }

    @Test
    fun `search by author returns matching books`() {
        val results = CuratedBooks.list.filter {
            val authorLower = it.author.lowercase()
            val queryWords = "Jane Austen".lowercase().split(" ")
            queryWords.all { word -> authorLower.contains(word) }
        }
        assertTrue("Expected to find Jane Austen books", results.isNotEmpty())
        assertTrue(results.any { it.title.contains("Pride and Prejudice") })
    }
}
