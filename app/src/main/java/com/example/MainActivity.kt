package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DownloadProgressDialog
import com.example.ui.screens.detail.BookDetailBottomSheet
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.reader.ReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FreeBooksApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeBooksApp(
    viewModel: BookViewModel = viewModel()
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State observation
    val displayedBooks by viewModel.displayedBooks.collectAsStateWithLifecycle()
    val popularBooks by viewModel.popularBooks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val selectedBook by viewModel.selectedBook.collectAsStateWithLifecycle()
    val readingBook by viewModel.readingBook.collectAsStateWithLifecycle()
    val readingText by viewModel.readingText.collectAsStateWithLifecycle()
    val isLoadingReadingText by viewModel.isLoadingReadingText.collectAsStateWithLifecycle()
    val readerFontSize by viewModel.readerFontSize.collectAsStateWithLifecycle()
    val readerFontFamily by viewModel.readerFontFamily.collectAsStateWithLifecycle()
    val readerTheme by viewModel.readerTheme.collectAsStateWithLifecycle()

    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()

    val bookmarkedBooks by viewModel.bookmarkedBooks.collectAsStateWithLifecycle()
    val downloadedBooks by viewModel.downloadedBooks.collectAsStateWithLifecycle()
    val readingHistory by viewModel.readingHistory.collectAsStateWithLifecycle()

    // Handle back button when reader or detail is open
    BackHandler(enabled = readingBook != null || selectedBook != null) {
        if (readingBook != null) {
            viewModel.closeReader()
        } else if (selectedBook != null) {
            scope.launch {
                sheetState.hide()
                viewModel.selectBook(null)
            }
        }
    }

    if (readingBook != null) {
        // Fullscreen distraction-free reader
        ReaderScreen(
            book = readingBook!!,
            text = readingText,
            isLoading = isLoadingReadingText,
            fontSize = readerFontSize,
            fontFamily = readerFontFamily,
            readerTheme = readerTheme,
            onBackClick = { viewModel.closeReader() },
            onToggleBookmark = { viewModel.toggleBookmark(it) },
            onDownloadPdf = { viewModel.downloadPdf(it) },
            onFontSizeChange = { viewModel.setFontSize(it) },
            onFontFamilyChange = { viewModel.setFontFamily(it) },
            onReaderThemeChange = { viewModel.setReaderTheme(it) },
            onProgressUpdate = { progress, position ->
                viewModel.updateReadingProgress(progress, position)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = selectedNavIndex == 0,
                        onClick = { selectedNavIndex = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedNavIndex == 0) Icons.Filled.Explore else Icons.Outlined.Explore,
                                contentDescription = "Explore"
                            )
                        },
                        label = { Text("Explore") },
                        modifier = Modifier.testTag("nav_explore")
                    )

                    NavigationBarItem(
                        selected = selectedNavIndex == 1,
                        onClick = { selectedNavIndex = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedNavIndex == 1) Icons.Filled.LocalLibrary else Icons.Outlined.LocalLibrary,
                                contentDescription = "My Library"
                            )
                        },
                        label = { Text("My Library") },
                        modifier = Modifier.testTag("nav_library")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedNavIndex == 0) {
                    HomeScreen(
                        displayedBooks = displayedBooks,
                        popularBooks = popularBooks,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        isLoading = isLoading,
                        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                        onClearSearch = { viewModel.clearSearch() },
                        onCategorySelected = { viewModel.onCategorySelected(it) },
                        onBookClick = { book -> viewModel.selectBook(book) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) }
                    )
                } else {
                    LibraryScreen(
                        bookmarkedBooks = bookmarkedBooks,
                        downloadedBooks = downloadedBooks,
                        readingHistory = readingHistory,
                        onBookClick = { book -> viewModel.selectBook(book) },
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        onDownloadPdf = { viewModel.downloadPdf(it) }
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (selectedBook != null && readingBook == null) {
        BookDetailBottomSheet(
            book = selectedBook,
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    viewModel.selectBook(null)
                }
            },
            onReadClick = { book ->
                viewModel.startReading(book)
            },
            onDownloadPdfClick = { book ->
                viewModel.downloadPdf(book)
            },
            onToggleBookmark = { book ->
                viewModel.toggleBookmark(book)
            }
        )
    }

    // Download / PDF Generation Progress Dialog
    DownloadProgressDialog(
        status = downloadStatus,
        onDismiss = { viewModel.dismissDownloadStatus() },
        onOpenPdf = { file -> viewModel.openDownloadedPdf(file) },
        onSharePdf = { file, title -> viewModel.shareDownloadedPdf(file, title) }
    )
}
