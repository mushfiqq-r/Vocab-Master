package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WordEntity
import com.example.data.model.WordWithReview
import com.example.data.repository.VocabRepository
import com.example.data.util.SM2Algorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenTab(val route: String, val title: String) {
    object Study : ScreenTab("study", "Study")
    object Library : ScreenTab("library", "Library")
    object Progress : ScreenTab("progress", "Progress")
    object Tutor : ScreenTab("tutor", "Word Tutor")
}

data class StudySessionState(
    val wordsQueue: List<WordWithReview> = emptyList(),
    val currentIndex: Int = 0,
    val isCardFlipped: Boolean = false,
    val isSessionComplete: Boolean = false,
    val reviewedInSession: Int = 0,
    val newLearnedInSession: Int = 0,
    val xpEarnedInSession: Int = 0,
    val masteredInSession: Int = 0,
    val showMasteryCelebration: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VocabRepository.getInstance(application)

    private val _currentTab = MutableStateFlow<ScreenTab>(ScreenTab.Study)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBookFilter = MutableStateFlow("ALL") // "ALL", "gre_333", "ws1", "ws2"
    val selectedBookFilter: StateFlow<String> = _selectedBookFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("ALL") // "ALL", "new", "learning", "mastered"
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _selectedWordDetail = MutableStateFlow<WordEntity?>(null)
    val selectedWordDetail: StateFlow<WordEntity?> = _selectedWordDetail.asStateFlow()

    private val _studyState = MutableStateFlow(StudySessionState())
    val studyState: StateFlow<StudySessionState> = _studyState.asStateFlow()

    private val _wordOfTheDay = MutableStateFlow<WordEntity?>(null)
    val wordOfTheDay: StateFlow<WordEntity?> = _wordOfTheDay.asStateFlow()

    val userStats: StateFlow<UserStatsEntity?> = repository.getUserStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatsEntity())

    val wordsWithReviews: StateFlow<List<WordWithReview>> = repository.getAllWordsWithReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLibraryWords: StateFlow<List<WordWithReview>> = combine(
        wordsWithReviews,
        _searchQuery,
        _selectedBookFilter,
        _selectedStatusFilter
    ) { list, query, book, status ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.word.term.contains(query, ignoreCase = true) ||
                    item.word.banglaMeaning.contains(query, ignoreCase = true) ||
                    item.word.primaryMeaning.contains(query, ignoreCase = true)

            val matchesBook = book == "ALL" || item.word.bookIds.contains(book)

            val itemStatus = item.review?.status ?: "new"
            val matchesStatus = status == "ALL" || itemStatus.equals(status, ignoreCase = true)

            matchesQuery && matchesBook && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDataIfNeeded()
            startNewStudySession()
            pickWordOfTheDay()
        }
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBookFilter(book: String) {
        _selectedBookFilter.value = book
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun selectWordDetail(word: WordEntity?) {
        _selectedWordDetail.value = word
    }

    private fun pickWordOfTheDay() {
        viewModelScope.launch {
            wordsWithReviews.collect { words ->
                if (words.isNotEmpty() && _wordOfTheDay.value == null) {
                    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                    val index = dayOfYear % words.size
                    _wordOfTheDay.value = words[index].word
                }
            }
        }
    }

    fun startNewStudySession(batchSize: Int = 15) {
        viewModelScope.launch {
            val dueWords = repository.getDueWordsForStudy(batchSize)
            _studyState.value = StudySessionState(
                wordsQueue = dueWords,
                currentIndex = 0,
                isCardFlipped = false,
                isSessionComplete = dueWords.isEmpty(),
                reviewedInSession = 0,
                newLearnedInSession = 0,
                xpEarnedInSession = 0,
                masteredInSession = 0,
                showMasteryCelebration = false
            )
        }
    }

    fun flipCard() {
        _studyState.value = _studyState.value.copy(
            isCardFlipped = !_studyState.value.isCardFlipped
        )
    }

    fun gradeCurrentWord(grade: SM2Algorithm.ReviewGrade) {
        val current = _studyState.value
        val currentWordItem = current.wordsQueue.getOrNull(current.currentIndex) ?: return

        viewModelScope.launch {
            val updatedReview = repository.processReviewGrade(currentWordItem.word.id, grade)

            val isNew = currentWordItem.review?.repetitions == 0 && updatedReview.repetitions > 0
            val isNowMastered = updatedReview.status == "mastered" && currentWordItem.review?.status != "mastered"

            val nextIndex = current.currentIndex + 1
            val isFinished = nextIndex >= current.wordsQueue.size

            _studyState.value = current.copy(
                currentIndex = nextIndex,
                isCardFlipped = false,
                isSessionComplete = isFinished,
                reviewedInSession = current.reviewedInSession + 1,
                newLearnedInSession = current.newLearnedInSession + (if (isNew) 1 else 0),
                xpEarnedInSession = current.xpEarnedInSession + grade.xp,
                masteredInSession = current.masteredInSession + (if (isNowMastered) 1 else 0),
                showMasteryCelebration = isNowMastered
            )
        }
    }

    fun dismissCelebration() {
        _studyState.value = _studyState.value.copy(showMasteryCelebration = false)
    }

    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(newGoal)
        }
    }
}
