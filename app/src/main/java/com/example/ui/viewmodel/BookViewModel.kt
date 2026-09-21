package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Book
import com.example.data.model.CuratedBooks
import com.example.data.pdf.PdfExporter
import com.example.data.repository.BookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ReaderTheme(val label: String) {
    PAPER("Paper"),
    SEPIA("Sepia"),
    NIGHT("Night"),
    OLED("OLED")
}

enum class ReaderFontFamily(val label: String) {
    SERIF("Serif (Classic)"),
    SANS("Sans-Serif (Modern)"),
    MONO("Monospace")
}

data class DownloadStatus(
    val isDownloading: Boolean = false,
    val bookId: Int? = null,
    val bookTitle: String = "",
    val progress: Float = 0f,
    val completedFile: File? = null,
    val error: String? = null
)

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    // Explore / Popular Books
    private val _popularBooks = MutableStateFlow<List<Book>>(CuratedBooks.list)
    val popularBooks: StateFlow<List<Book>> = _popularBooks.asStateFlow()

    // Filtered / Search Books
    private val _displayedBooks = MutableStateFlow<List<Book>>(CuratedBooks.list)
    val displayedBooks: StateFlow<List<Book>> = _displayedBooks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Selected Book for Detail
    private val _selectedBook = MutableStateFlow<Book?>(null)
    val selectedBook: StateFlow<Book?> = _selectedBook.asStateFlow()

    // In-App Reader State
    private val _readingBook = MutableStateFlow<Book?>(null)
    val readingBook: StateFlow<Book?> = _readingBook.asStateFlow()

    private val _readingText = MutableStateFlow("")
    val readingText: StateFlow<String> = _readingText.asStateFlow()

    private val _isLoadingReadingText = MutableStateFlow(false)
    val isLoadingReadingText: StateFlow<Boolean> = _isLoadingReadingText.asStateFlow()

    private val _readerFontSize = MutableStateFlow(18f)
    val readerFontSize: StateFlow<Float> = _readerFontSize.asStateFlow()

    private val _readerFontFamily = MutableStateFlow(ReaderFontFamily.SERIF)
    val readerFontFamily: StateFlow<ReaderFontFamily> = _readerFontFamily.asStateFlow()

    private val _readerTheme = MutableStateFlow(ReaderTheme.PAPER)
    val readerTheme: StateFlow<ReaderTheme> = _readerTheme.asStateFlow()

    // Download state
    private val _downloadStatus = MutableStateFlow(DownloadStatus())
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    // Library from Room
    val bookmarkedBooks: StateFlow<List<Book>> = repository.bookmarkedBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedBooks: StateFlow<List<Book>> = repository.downloadedBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingHistory: StateFlow<List<Book>> = repository.readingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        loadInitialCatalog()
    }

    private fun loadInitialCatalog() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val books = repository.getPopularBooks()
                _popularBooks.value = books
                if (_searchQuery.value.isBlank() && _selectedCategory.value == "All") {
                    _displayedBooks.value = books
                }
            } catch (_: Exception) {
                // Curated fallback already set
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350) // Debounce
            _isLoading.value = true
            try {
                if (query.isBlank()) {
                    if (_selectedCategory.value == "All") {
                        _displayedBooks.value = _popularBooks.value
                    } else {
                        onCategorySelected(_selectedCategory.value)
                    }
                } else {
                    val results = repository.searchBooks(query)
                    _displayedBooks.value = results
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        onCategorySelected(_selectedCategory.value)
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = ""
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.getBooksByTopic(category)
                _displayedBooks.value = results
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectBook(book: Book?) {
        _selectedBook.value = book
    }

    fun toggleBookmark(book: Book) {
        viewModelScope.launch {
            val newStatus = repository.toggleBookmark(book)
            val updated = book.copy(isBookmarked = newStatus)

            // Update in lists
            updateBookInLists(updated)

            if (_selectedBook.value?.id == book.id) {
                _selectedBook.value = updated
            }
            if (_readingBook.value?.id == book.id) {
                _readingBook.value = updated
            }
        }
    }

    private fun updateBookInLists(updated: Book) {
        _popularBooks.value = _popularBooks.value.map { if (it.id == updated.id) updated else it }
        _displayedBooks.value = _displayedBooks.value.map { if (it.id == updated.id) updated else it }
    }

    fun startReading(book: Book) {
        _readingBook.value = book
        viewModelScope.launch {
            _isLoadingReadingText.value = true
            try {
                val text = repository.loadBookText(book)
                _readingText.value = text
            } finally {
                _isLoadingReadingText.value = false
            }
        }
    }

    fun closeReader() {
        _readingBook.value = null
        _readingText.value = ""
    }

    fun updateReadingProgress(progress: Float, position: Int) {
        val current = _readingBook.value ?: return
        viewModelScope.launch {
            repository.updateReadingProgress(current, position, progress)
            val updated = current.copy(readingProgress = progress)
            _readingBook.value = updated
            updateBookInLists(updated)
        }
    }

    fun setFontSize(size: Float) {
        _readerFontSize.value = size.coerceIn(12f, 32f)
    }

    fun setFontFamily(family: ReaderFontFamily) {
        _readerFontFamily.value = family
    }

    fun setReaderTheme(theme: ReaderTheme) {
        _readerTheme.value = theme
    }

    fun downloadPdf(book: Book) {
        viewModelScope.launch {
            _downloadStatus.value = DownloadStatus(
                isDownloading = true,
                bookId = book.id,
                bookTitle = book.title,
                progress = 0.05f
            )
            try {
                val file = repository.downloadBookPdf(book) { progress ->
                    _downloadStatus.value = _downloadStatus.value.copy(progress = progress)
                }
                val updated = book.copy(isDownloaded = true, localFilePath = file.absolutePath)
                updateBookInLists(updated)
                if (_selectedBook.value?.id == book.id) {
                    _selectedBook.value = updated
                }

                _downloadStatus.value = DownloadStatus(
                    isDownloading = false,
                    bookId = book.id,
                    bookTitle = book.title,
                    progress = 1f,
                    completedFile = file
                )
            } catch (e: Exception) {
                _downloadStatus.value = DownloadStatus(
                    isDownloading = false,
                    bookId = book.id,
                    bookTitle = book.title,
                    error = e.localizedMessage ?: "Failed to generate PDF. Please try again."
                )
            }
        }
    }

    fun dismissDownloadStatus() {
        _downloadStatus.value = DownloadStatus()
    }

    fun openDownloadedPdf(file: File) {
        PdfExporter.openPdf(getApplication(), file)
    }

    fun shareDownloadedPdf(file: File, bookTitle: String) {
        PdfExporter.sharePdf(getApplication(), file, bookTitle)
    }
}
