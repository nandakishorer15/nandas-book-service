package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Book

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val downloadCount: Int,
    val subjects: String, // Comma or newline separated
    val languages: String,
    val textUrl: String?,
    val htmlUrl: String?,
    val epubUrl: String?,
    val pdfUrl: String?,
    val isBookmarked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val lastReadPosition: Int = 0,
    val readingProgress: Float = 0f,
    val lastReadTimestamp: Long = 0L,
    val cachedContent: String? = null
) {
    fun toDomainModel(): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            downloadCount = downloadCount,
            subjects = subjects.split(";;").filter { it.isNotBlank() },
            languages = languages.split(",").filter { it.isNotBlank() },
            textUrl = textUrl,
            htmlUrl = htmlUrl,
            epubUrl = epubUrl,
            pdfUrl = pdfUrl,
            isBookmarked = isBookmarked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath,
            readingProgress = readingProgress,
            lastReadTimestamp = lastReadTimestamp,
            cachedExcerpt = cachedContent?.take(500)
        )
    }

    companion object {
        fun fromDomainModel(book: Book, cachedContent: String? = null): BookEntity {
            return BookEntity(
                id = book.id,
                title = book.title,
                author = book.author,
                coverUrl = book.coverUrl,
                downloadCount = book.downloadCount,
                subjects = book.subjects.joinToString(";;"),
                languages = book.languages.joinToString(","),
                textUrl = book.textUrl,
                htmlUrl = book.htmlUrl,
                epubUrl = book.epubUrl,
                pdfUrl = book.pdfUrl,
                isBookmarked = book.isBookmarked,
                isDownloaded = book.isDownloaded,
                localFilePath = book.localFilePath,
                readingProgress = book.readingProgress,
                lastReadTimestamp = book.lastReadTimestamp,
                cachedContent = cachedContent
            )
        }
    }
}
