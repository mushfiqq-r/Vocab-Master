package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSessionState
import com.example.data.model.QuizType
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WordEntity
import com.example.data.model.WordWithReview
import com.example.data.repository.VocabRepository
import com.example.data.util.QuizGenerator
import com.example.data.util.SM2Algorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ScreenTab(val route: String, val title: String) {
    object Study : ScreenTab("study", "Study")
    object Quiz : ScreenTab("quiz", "Quiz")
    object Library : ScreenTab("library", "Library")
    object Tutor : ScreenTab("tutor", "Word Tutor")
    object Progress : ScreenTab("progress", "Progress")
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

    private val _selectedTierFilter = MutableStateFlow(0) // 0: ALL, 1: Core, 2: Moderate, 3: Advanced
    val selectedTierFilter: StateFlow<Int> = _selectedTierFilter.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow("A_TO_Z") // "A_TO_Z", "Z_TO_A", "DIFFICULTY", "MASTERY"
    val selectedSortOrder: StateFlow<String> = _selectedSortOrder.asStateFlow()

    private val _showQuickSearchDialog = MutableStateFlow(false)
    val showQuickSearchDialog: StateFlow<Boolean> = _showQuickSearchDialog.asStateFlow()

    fun openQuickSearch() {
        _showQuickSearchDialog.value = true
    }

    fun closeQuickSearch() {
        _showQuickSearchDialog.value = false
    }

    private val _selectedWordDetail = MutableStateFlow<WordEntity?>(null)
    val selectedWordDetail: StateFlow<WordEntity?> = _selectedWordDetail.asStateFlow()

    private val _studyState = MutableStateFlow(StudySessionState())
    val studyState: StateFlow<StudySessionState> = _studyState.asStateFlow()

    private val _wordOfTheDay = MutableStateFlow<WordEntity?>(null)
    val wordOfTheDay: StateFlow<WordEntity?> = _wordOfTheDay.asStateFlow()

    private val _quizState = MutableStateFlow(QuizSessionState())
    val quizState: StateFlow<QuizSessionState> = _quizState.asStateFlow()

    val userStats: StateFlow<UserStatsEntity?> = repository.getUserStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatsEntity())

    val dueReviewCount: StateFlow<Int> = repository.getDueCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageEaseFactor: StateFlow<Double> = repository.getAverageEaseFactor()
        .map { it ?: 2.5 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2.5)

    val upcomingReviews: StateFlow<List<ReviewStateEntity>> = repository.getUpcomingReviewSchedule()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDailyActivity: StateFlow<List<com.example.data.model.DailyActivityEntity>> = repository.getRecentDailyActivity(14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDaysCount: StateFlow<Int> = repository.getCompletedDaysCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _showStreakDetailDialog = MutableStateFlow(false)
    val showStreakDetailDialog: StateFlow<Boolean> = _showStreakDetailDialog.asStateFlow()

    fun openStreakDetailDialog() {
        _showStreakDetailDialog.value = true
    }

    fun closeStreakDetailDialog() {
        _showStreakDetailDialog.value = false
    }

    val streakMilestones = listOf(
        com.example.data.model.StreakMilestone(3, "Spark Initiate", "Bronze Flame", 50, "3-Day Learning Consistency"),
        com.example.data.model.StreakMilestone(7, "Weekly Flame", "Silver Torch", 150, "7-Day Consistent Scholar"),
        com.example.data.model.StreakMilestone(14, "Fortnight Inferno", "Gold Blaze", 300, "14-Day Memory Dedication"),
        com.example.data.model.StreakMilestone(30, "Monthly Master", "Diamond Sun", 750, "30-Day Vocabulary Mastery"),
        com.example.data.model.StreakMilestone(60, "Iron Will", "Obsidian Phoenix", 1500, "60-Day Habit Champion"),
        com.example.data.model.StreakMilestone(100, "Century Legend", "Celestial Nova", 3000, "100-Day GRE Polymath")
    )

    val wordsWithReviews: StateFlow<List<WordWithReview>> = repository.getAllWordsWithReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class LibraryFilterCriteria(
        val query: String,
        val book: String,
        val status: String,
        val tier: Int,
        val sortOrder: String
    )

    private val filterCriteria = combine(
        combine(_searchQuery, _selectedBookFilter) { q, b -> Pair(q, b) },
        combine(_selectedStatusFilter, _selectedTierFilter, _selectedSortOrder) { s, t, o -> Triple(s, t, o) }
    ) { pair, triple ->
        LibraryFilterCriteria(
            query = pair.first,
            book = pair.second,
            status = triple.first,
            tier = triple.second,
            sortOrder = triple.third
        )
    }

    val filteredLibraryWords: StateFlow<List<WordWithReview>> = combine(
        wordsWithReviews,
        filterCriteria
    ) { list, criteria ->
        val filtered = list.filter { item ->
            val q = criteria.query.trim()
            val matchesQuery = q.isBlank() ||
                    item.word.term.contains(q, ignoreCase = true) ||
                    item.word.banglaMeaning.contains(q, ignoreCase = true) ||
                    item.word.primaryMeaning.contains(q, ignoreCase = true) ||
                    item.word.preciseMeaning.contains(q, ignoreCase = true) ||
                    item.word.memoryHook.contains(q, ignoreCase = true)

            val matchesBook = criteria.book == "ALL" || item.word.bookIds.contains(criteria.book)

            val itemStatus = item.review?.status ?: "new"
            val matchesStatus = criteria.status == "ALL" || itemStatus.equals(criteria.status, ignoreCase = true)

            val matchesTier = criteria.tier == 0 || item.word.difficultyTier == criteria.tier

            matchesQuery && matchesBook && matchesStatus && matchesTier
        }

        when (criteria.sortOrder) {
            "Z_TO_A" -> filtered.sortedByDescending { it.word.term.lowercase() }
            "DIFFICULTY" -> filtered.sortedWith(compareBy({ it.word.difficultyTier }, { it.word.term }))
            "MASTERY" -> filtered.sortedWith(
                compareByDescending<WordWithReview> {
                    when (it.review?.status) {
                        "mastered" -> 3
                        "learning" -> 2
                        else -> 1
                    }
                }.thenBy { it.word.term }
            )
            else -> filtered.sortedBy { it.word.term.lowercase() }
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

    fun setTierFilter(tier: Int) {
        _selectedTierFilter.value = tier
    }

    fun setSortOrder(order: String) {
        _selectedSortOrder.value = order
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

    fun resetWordProgress(wordId: Long) {
        viewModelScope.launch {
            repository.resetWordProgress(wordId)
        }
    }

    // ================= QUIZ FUNCTIONALITY =================

    fun setQuizBookFilter(book: String) {
        _quizState.value = _quizState.value.copy(selectedBookFilter = book)
    }

    fun setQuizType(type: QuizType) {
        _quizState.value = _quizState.value.copy(selectedQuizType = type)
    }

    fun setQuizQuestionCount(count: Int) {
        _quizState.value = _quizState.value.copy(questionCount = count)
    }

    fun startQuiz() {
        val allWords = wordsWithReviews.value.map { it.word }
        if (allWords.isEmpty()) return

        val state = _quizState.value
        val questions = QuizGenerator.generateQuiz(
            allWords = allWords,
            bookFilter = state.selectedBookFilter,
            quizType = state.selectedQuizType,
            questionCount = state.questionCount
        )

        _quizState.value = state.copy(
            isConfiguring = false,
            questions = questions,
            currentIndex = 0,
            isFinished = false,
            score = 0,
            streak = 0,
            maxStreak = 0,
            totalXpEarned = 0,
            missedQuestions = emptyList()
        )
    }

    fun answerQuizQuestion(selectedOptionIndex: Int) {
        val state = _quizState.value
        val currentQ = state.questions.getOrNull(state.currentIndex) ?: return
        if (currentQ.isAnswered) return // Prevent multiple answers

        val isCorrect = selectedOptionIndex == currentQ.correctIndex
        val updatedQ = currentQ.copy(
            selectedOptionIndex = selectedOptionIndex,
            isAnswered = true,
            isCorrect = isCorrect
        )

        val updatedQuestions = state.questions.toMutableList()
        updatedQuestions[state.currentIndex] = updatedQ

        val newStreak = if (isCorrect) state.streak + 1 else 0
        val newMaxStreak = maxOf(state.maxStreak, newStreak)
        val earnedXp = if (isCorrect) (20 + (newStreak * 2)) else 5 // Bonus for streaks
        val newScore = if (isCorrect) state.score + 1 else state.score

        val newMissed = if (!isCorrect) {
            state.missedQuestions + updatedQ
        } else {
            state.missedQuestions
        }

        _quizState.value = state.copy(
            questions = updatedQuestions,
            score = newScore,
            streak = newStreak,
            maxStreak = newMaxStreak,
            totalXpEarned = state.totalXpEarned + earnedXp,
            missedQuestions = newMissed
        )

        // Award XP and update word SM2 mastery state based on quiz response
        viewModelScope.launch {
            val grade = if (isCorrect) {
                if (newStreak >= 3) SM2Algorithm.ReviewGrade.EASY else SM2Algorithm.ReviewGrade.GOOD
            } else {
                SM2Algorithm.ReviewGrade.AGAIN
            }
            repository.processReviewGrade(currentQ.targetWord.id, grade)
        }
    }

    fun nextQuizQuestion() {
        val state = _quizState.value
        val nextIdx = state.currentIndex + 1
        if (nextIdx >= state.questions.size) {
            _quizState.value = state.copy(isFinished = true)
        } else {
            _quizState.value = state.copy(currentIndex = nextIdx)
        }
    }

    fun resetQuizConfig() {
        _quizState.value = _quizState.value.copy(
            isConfiguring = true,
            isFinished = false,
            currentIndex = 0,
            questions = emptyList()
        )
    }

    fun retakeMissedQuizQuestions() {
        val state = _quizState.value
        if (state.missedQuestions.isEmpty()) return

        // Reset the missed questions to unattempted state
        val resetQuestions = state.missedQuestions.mapIndexed { index, q ->
            q.copy(
                id = index + 1,
                selectedOptionIndex = null,
                isAnswered = false,
                isCorrect = false
            )
        }

        _quizState.value = state.copy(
            isConfiguring = false,
            questions = resetQuestions,
            currentIndex = 0,
            isFinished = false,
            score = 0,
            streak = 0,
            maxStreak = 0,
            totalXpEarned = 0,
            missedQuestions = emptyList()
        )
    }

    // ================= BACKUP & RESTORE FUNCTIONALITY =================

    private val _localBackups = MutableStateFlow<List<com.example.data.util.BackupMetadata>>(emptyList())
    val localBackups: StateFlow<List<com.example.data.util.BackupMetadata>> = _localBackups.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    fun clearBackupMessage() {
        _backupStatusMessage.value = null
    }

    fun loadLocalBackups(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = com.example.data.util.BackupManager.listLocalBackups(context)
            _localBackups.value = list
        }
    }

    fun createLocalBackup(context: Context, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson()
                val result = com.example.data.util.BackupManager.saveToLocalAppDirectory(context, json)
                if (result.isSuccess) {
                    val file = result.getOrThrow()
                    loadLocalBackups(context)
                    val msg = "Backup saved successfully (${file.name})"
                    _backupStatusMessage.value = msg
                    onComplete?.invoke(true, msg)
                } else {
                    val err = "Failed to create local backup: ${result.exceptionOrNull()?.message}"
                    _backupStatusMessage.value = err
                    onComplete?.invoke(false, err)
                }
            } catch (e: Exception) {
                val err = "Backup error: ${e.message}"
                _backupStatusMessage.value = err
                onComplete?.invoke(false, err)
            }
        }
    }

    suspend fun getExportJson(): String {
        return repository.exportBackupJson()
    }

    fun restoreFromJson(context: Context, jsonString: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.restoreBackupFromJson(jsonString)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                loadLocalBackups(context)
                val msg = "Restored ${data.reviewStates.size} SM-2 words and ${data.stats.xpTotal} XP successfully!"
                _backupStatusMessage.value = msg
                onComplete?.invoke(true, msg)
            } else {
                val err = "Restore failed: ${result.exceptionOrNull()?.message}"
                _backupStatusMessage.value = err
                onComplete?.invoke(false, err)
            }
        }
    }

    fun restoreFromLocalBackupFile(context: Context, filePath: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, "Backup file not found")
                    }
                    return@launch
                }
                val content = file.readText()
                withContext(Dispatchers.Main) {
                    restoreFromJson(context, content, onComplete)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Error reading backup file: ${e.message}")
                }
            }
        }
    }

    fun deleteLocalBackupFile(context: Context, filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.data.util.BackupManager.deleteLocalBackup(filePath)
            loadLocalBackups(context)
        }
    }
}
