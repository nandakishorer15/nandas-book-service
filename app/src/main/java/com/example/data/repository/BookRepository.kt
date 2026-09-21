package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.BookEntity
import com.example.data.model.Book
import com.example.data.model.CuratedBooks
import com.example.data.model.toDomainModel
import com.example.data.pdf.PdfExporter
import com.example.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class BookRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val bookDao = db.bookDao()
    private val apiService = ApiClient.service
    private val httpClient = ApiClient.okHttpClient

    val bookmarkedBooks: Flow<List<Book>> = bookDao.getBookmarkedBooks().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val downloadedBooks: Flow<List<Book>> = bookDao.getDownloadedBooks().map { entities ->
        entities.map { it.toDomainModel() }
    }

    val readingHistory: Flow<List<Book>> = bookDao.getReadingHistory().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun getPopularBooks(): List<Book> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBooks(sort = "popular")
            if (response.results.isNotEmpty()) {
                val apiBooks = response.results.map { it.toDomainModel() }
                mergeWithLocalStatus(apiBooks)
            } else {
                mergeWithLocalStatus(CuratedBooks.list)
            }
        } catch (_: Exception) {
            mergeWithLocalStatus(CuratedBooks.list)
        }
    }

    suspend fun searchBooks(query: String): List<Book> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getPopularBooks()
        try {
            val response = apiService.getBooks(search = query.trim())
            if (response.results.isNotEmpty()) {
                val apiBooks = response.results.map { it.toDomainModel() }
                mergeWithLocalStatus(apiBooks)
            } else {
                // Fallback filter on curated list
                val filtered = CuratedBooks.list.filter { matchesSearchQuery(it, query) }
                mergeWithLocalStatus(filtered)
            }
        } catch (_: Exception) {
            val filtered = CuratedBooks.list.filter { matchesSearchQuery(it, query) }
            mergeWithLocalStatus(filtered)
        }
    }

    fun matchesSearchQuery(book: Book, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isBlank()) return true
        if (book.title.lowercase().contains(q) || book.author.lowercase().contains(q)) return true
        val queryWords = q.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val authorLower = book.author.lowercase()
        val titleLower = book.title.lowercase()
        if (queryWords.size > 1 && queryWords.all { authorLower.contains(it) || titleLower.contains(it) }) return true
        return book.subjects.any { s -> s.contains(q, ignoreCase = true) }
    }

    suspend fun getBooksByTopic(topic: String): List<Book> = withContext(Dispatchers.IO) {
        if (topic.equals("All", ignoreCase = true)) return@withContext getPopularBooks()
        try {
            val response = apiService.getBooks(topic = topic.lowercase())
            if (response.results.isNotEmpty()) {
                val apiBooks = response.results.map { it.toDomainModel() }
                mergeWithLocalStatus(apiBooks)
            } else {
                val filtered = CuratedBooks.list.filter { book ->
                    book.subjects.any { s -> s.contains(topic, ignoreCase = true) }
                }
                mergeWithLocalStatus(if (filtered.isNotEmpty()) filtered else CuratedBooks.list)
            }
        } catch (_: Exception) {
            val filtered = CuratedBooks.list.filter { book ->
                book.subjects.any { s -> s.contains(topic, ignoreCase = true) }
            }
            mergeWithLocalStatus(if (filtered.isNotEmpty()) filtered else CuratedBooks.list)
        }
    }

    private suspend fun mergeWithLocalStatus(books: List<Book>): List<Book> {
        return books.map { book ->
            val local = bookDao.getBookByIdDirect(book.id)
            if (local != null) {
                book.copy(
                    isBookmarked = local.isBookmarked,
                    isDownloaded = local.isDownloaded,
                    localFilePath = local.localFilePath,
                    readingProgress = local.readingProgress,
                    lastReadTimestamp = local.lastReadTimestamp
                )
            } else {
                book
            }
        }
    }

    suspend fun getBookDetails(bookId: Int): Book? = withContext(Dispatchers.IO) {
        val local = bookDao.getBookByIdDirect(bookId)
        if (local != null) return@withContext local.toDomainModel()

        // Check curated
        val curated = CuratedBooks.list.find { it.id == bookId }
        if (curated != null) return@withContext curated

        // Fetch from API
        try {
            val apiBook = apiService.getBookById(bookId).toDomainModel()
            apiBook
        } catch (_: Exception) {
            null
        }
    }

    suspend fun toggleBookmark(book: Book): Boolean = withContext(Dispatchers.IO) {
        val newStatus = !book.isBookmarked
        val existing = bookDao.getBookByIdDirect(book.id)
        if (existing != null) {
            bookDao.updateBookmark(book.id, newStatus)
        } else {
            val entity = BookEntity.fromDomainModel(book.copy(isBookmarked = newStatus))
            bookDao.insertOrUpdate(entity)
        }
        newStatus
    }

    suspend fun updateReadingProgress(book: Book, position: Int, progress: Float) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = bookDao.getBookByIdDirect(book.id)
        if (existing != null) {
            bookDao.updateReadingProgress(book.id, position, progress, now)
        } else {
            val entity = BookEntity.fromDomainModel(
                book.copy(readingProgress = progress, lastReadTimestamp = now)
            ).copy(lastReadPosition = position)
            bookDao.insertOrUpdate(entity)
        }
    }

    suspend fun loadBookText(book: Book): String = withContext(Dispatchers.IO) {
        // 1. Check if cached in DB
        val local = bookDao.getBookByIdDirect(book.id)
        if (!local?.cachedContent.isNullOrBlank()) {
            return@withContext local!!.cachedContent!!
        }

        // 2. Check if local text file exists
        val textFile = File(context.filesDir, "book_text_${book.id}.txt")
        if (textFile.exists()) {
            val text = textFile.readText()
            if (text.isNotBlank()) {
                saveCachedContent(book, text)
                return@withContext text
            }
        }

        // 3. Try to fetch from remote URL
        val urlsToTry = listOfNotNull(
            book.textUrl,
            "https://www.gutenberg.org/files/${book.id}/${book.id}-0.txt",
            "https://www.gutenberg.org/ebooks/${book.id}.txt.utf-8"
        )

        for (url in urlsToTry) {
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string()
                        if (!rawBody.isNullOrBlank()) {
                            val cleanText = cleanGutenbergText(rawBody)
                            textFile.writeText(cleanText)
                            saveCachedContent(book, cleanText)
                            return@withContext cleanText
                        }
                    }
                }
            } catch (_: Exception) {
                // Continue to next URL
            }
        }

        // 4. Fallback to cached excerpt or curated text
        val curated = CuratedBooks.list.find { it.id == book.id }
        val fallback = curated?.cachedExcerpt
            ?: "Content preview is currently unavailable for offline reading. Please connect to the internet to download the full edition."
        fallback
    }

    private suspend fun saveCachedContent(book: Book, content: String) {
        val existing = bookDao.getBookByIdDirect(book.id)
        if (existing != null) {
            bookDao.insertOrUpdate(existing.copy(cachedContent = content))
        } else {
            bookDao.insertOrUpdate(BookEntity.fromDomainModel(book, cachedContent = content))
        }
    }

    suspend fun downloadBookPdf(
        book: Book,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        // Check if already downloaded
        if (!book.localFilePath.isNullOrBlank()) {
            val existingFile = File(book.localFilePath)
            if (existingFile.exists()) {
                onProgress(1.0f)
                return@withContext existingFile
            }
        }

        // 1. Fetch text
        onProgress(0.1f)
        val text = loadBookText(book)
        onProgress(0.3f)

        // 2. Generate PDF using native PdfExporter
        val pdfFile = PdfExporter.generateBookPdf(
            context = context,
            book = book,
            bookText = text,
            onProgress = { p ->
                // Map progress from 0.3 to 1.0
                onProgress(0.3f + (p * 0.7f))
            }
        )

        // 3. Mark as downloaded in DB
        val existing = bookDao.getBookByIdDirect(book.id)
        if (existing != null) {
            bookDao.updateDownloaded(book.id, pdfFile.absolutePath)
        } else {
            val entity = BookEntity.fromDomainModel(
                book.copy(isDownloaded = true, localFilePath = pdfFile.absolutePath)
            )
            bookDao.insertOrUpdate(entity)
        }

        pdfFile
    }

    private fun cleanGutenbergText(raw: String): String {
        // Remove Gutenberg header and footer boilerplates if present
        var text = raw
        val startMarkers = listOf(
            "*** START OF THE PROJECT GUTENBERG",
            "*** START OF THIS PROJECT GUTENBERG",
            "*END*THE SMALL PRINT!"
        )
        for (marker in startMarkers) {
            val index = text.indexOf(marker, ignoreCase = true)
            if (index != -1) {
                val endOfLine = text.indexOf("\n", index)
                if (endOfLine != -1) {
                    text = text.substring(endOfLine + 1)
                }
                break
            }
        }

        val endMarkers = listOf(
            "*** END OF THE PROJECT GUTENBERG",
            "*** END OF THIS PROJECT GUTENBERG",
            "End of the Project Gutenberg",
            "End of Project Gutenberg"
        )
        for (marker in endMarkers) {
            val index = text.indexOf(marker, ignoreCase = true)
            if (index != -1) {
                text = text.substring(0, index)
                break
            }
        }

        return text.trim()
    }
}
