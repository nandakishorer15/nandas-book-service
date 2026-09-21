package com.example.data.model

data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val downloadCount: Int,
    val subjects: List<String>,
    val languages: List<String>,
    val textUrl: String?,
    val htmlUrl: String?,
    val epubUrl: String?,
    val pdfUrl: String?,
    val isBookmarked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val readingProgress: Float = 0f,
    val lastReadTimestamp: Long = 0L,
    val cachedExcerpt: String? = null
) {
    val displayAuthor: String
        get() {
            if (author.isBlank()) return "Unknown Author"
            // If Gutenberg format is "Lastname, Firstname", convert to "Firstname Lastname"
            val parts = author.split(",")
            return if (parts.size == 2) {
                "${parts[1].trim()} ${parts[0].trim()}"
            } else {
                author
            }
        }

    val primarySubject: String
        get() = subjects.firstOrNull()?.split("--")?.firstOrNull()?.trim() ?: "Literature & Fiction"
}

fun GutendexBook.toDomainModel(
    isBookmarked: Boolean = false,
    isDownloaded: Boolean = false,
    localFilePath: String? = null,
    readingProgress: Float = 0f
): Book {
    val authorsText = authors.joinToString(", ") { it.name }
    
    // Pick the best image cover available
    val cover = formats["image/jpeg"]
        ?: formats["image/png"]
        ?: "https://www.gutenberg.org/cache/epub/$id/pg$id.cover.medium.jpg"

    // Pick text and HTML urls
    val textUrl = formats["text/plain; charset=utf-8"]
        ?: formats["text/plain; charset=us-ascii"]
        ?: formats["text/plain"]

    val htmlUrl = formats["text/html"]
        ?: formats["text/html; charset=utf-8"]

    val epubUrl = formats["application/epub+zip"]
    val pdfUrl = formats["application/pdf"]

    return Book(
        id = id,
        title = title,
        author = authorsText,
        coverUrl = cover,
        downloadCount = downloadCount,
        subjects = subjects,
        languages = languages,
        textUrl = textUrl,
        htmlUrl = htmlUrl,
        epubUrl = epubUrl,
        pdfUrl = pdfUrl,
        isBookmarked = isBookmarked,
        isDownloaded = isDownloaded,
        localFilePath = localFilePath,
        readingProgress = readingProgress
    )
}
