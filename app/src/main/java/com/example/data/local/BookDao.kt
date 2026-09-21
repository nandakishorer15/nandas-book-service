package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE isBookmarked = 1 ORDER BY lastReadTimestamp DESC")
    fun getBookmarkedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isDownloaded = 1 ORDER BY lastReadTimestamp DESC")
    fun getDownloadedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE readingProgress > 0 ORDER BY lastReadTimestamp DESC")
    fun getReadingHistory(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Int): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdDirect(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(book: BookEntity)

    @Query("UPDATE books SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Int, isBookmarked: Boolean)

    @Query("UPDATE books SET isDownloaded = 1, localFilePath = :filePath WHERE id = :id")
    suspend fun updateDownloaded(id: Int, filePath: String)

    @Query("UPDATE books SET lastReadPosition = :position, readingProgress = :progress, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: Int, position: Int, progress: Float, timestamp: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Int)
}
