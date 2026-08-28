package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WordEntity
import com.example.data.model.WordWithReview
import com.example.data.seed.VocabSeedData
import com.example.data.util.SM2Algorithm
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar

class VocabRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val wordDao = database.wordDao()
    private val reviewDao = database.reviewDao()
    private val statsDao = database.statsDao()
    private val dailyActivityDao = database.dailyActivityDao()

    suspend fun initializeDataIfNeeded() = withContext(ioDispatcher) {
        val wordCount = wordDao.getWordCount()
        if (wordCount == 0) {
            val seedWords = VocabSeedData.getAllSeedWords()
            wordDao.insertWords(seedWords)

            val initialReviews = seedWords.map { word ->
                ReviewStateEntity(
                    wordId = word.id,
                    easeFactor = 2.5,
                    intervalDays = 0,
                    repetitions = 0,
                    nextReviewDate = System.currentTimeMillis(),
                    lastReviewedDate = 0L,
                    status = "new"
                )
            }
            reviewDao.insertAll(initialReviews)

            val initialStats = UserStatsEntity(
                id = 1,
                currentStreak = 4,
                longestStreak = 7,
                xpTotal = 360,
                level = 4,
                dailyGoal = 15,
                lastActiveDate = System.currentTimeMillis()
            )
            statsDao.insertOrUpdate(initialStats)

            // Seed recent 5 days of activity to showcase the streak tracker
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val cal = java.util.Calendar.getInstance()
            for (i in 0..4) {
                val pastCal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -i)
                }
                val dateStr = sdf.format(pastCal.time)
                val reviewedCount = if (i == 0) 8 else (15 + (i * 3))
                val goalMet = reviewedCount >= 15
                dailyActivityDao.insertOrUpdate(
                    com.example.data.model.DailyActivityEntity(
                        dateString = dateStr,
                        wordsReviewed = reviewedCount,
                        quizzesCompleted = if (i % 2 == 0) 1 else 0,
                        xpEarned = reviewedCount * 15,
                        goalMet = goalMet,
                        timestamp = pastCal.timeInMillis
                    )
                )
            }
        }
    }

    fun getAllWordsWithReviews(): Flow<List<WordWithReview>> {
        return combine(
            wordDao.getAllWords(),
            reviewDao.getAllReviewStates()
        ) { words, reviews ->
            val reviewMap = reviews.associateBy { it.wordId }
            words.map { word ->
                WordWithReview(
                    word = word,
                    review = reviewMap[word.id]
                )
            }
        }
    }

    fun getWordsByBook(bookId: String): Flow<List<WordWithReview>> {
        return combine(
            wordDao.getWordsByBook(bookId),
            reviewDao.getAllReviewStates()
        ) { words, reviews ->
            val reviewMap = reviews.associateBy { it.wordId }
            words.map { word ->
                WordWithReview(
                    word = word,
                    review = reviewMap[word.id]
                )
            }
        }
    }

    fun searchWords(query: String): Flow<List<WordWithReview>> {
        return combine(
            wordDao.searchWords(query),
            reviewDao.getAllReviewStates()
        ) { words, reviews ->
            val reviewMap = reviews.associateBy { it.wordId }
            words.map { word ->
                WordWithReview(
                    word = word,
                    review = reviewMap[word.id]
                )
            }
        }
    }

    suspend fun getDueWordsForStudy(batchSize: Int = 15): List<WordWithReview> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val dueReviews = reviewDao.getDueReviewStates(now)
        val sortedReviews = dueReviews.sortedWith(
            compareBy<ReviewStateEntity> {
                when (it.status) {
                    "learning" -> 0
                    "new" -> 1
                    else -> 2
                }
            }.thenBy { it.nextReviewDate }
        ).take(batchSize)

        val result = mutableListOf<WordWithReview>()
        for (rev in sortedReviews) {
            val word = wordDao.getWordById(rev.wordId)
            if (word != null) {
                result.add(WordWithReview(word, rev))
            }
        }

        // Fallback: If no due reviews, pull first batch of words
        if (result.isEmpty()) {
            val allWords = wordDao.getAllWords().firstOrNull() ?: emptyList()
            for (w in allWords.take(batchSize)) {
                val rev = reviewDao.getReviewStateForWord(w.id) ?: ReviewStateEntity(wordId = w.id)
                result.add(WordWithReview(w, rev))
            }
        }
        result
    }

    suspend fun processReviewGrade(wordId: Long, grade: SM2Algorithm.ReviewGrade): ReviewStateEntity = withContext(ioDispatcher) {
        val currentReview = reviewDao.getReviewStateForWord(wordId) ?: ReviewStateEntity(wordId = wordId)
        val updatedReview = SM2Algorithm.calculateNextReview(currentReview, grade)
        reviewDao.insertOrUpdate(updatedReview)

        // Award XP and update streak
        awardXpAndUpdateStreak(grade.xp)
        updatedReview
    }

    private suspend fun awardXpAndUpdateStreak(xpEarned: Int, isReview: Boolean = true, isQuiz: Boolean = false) {
        val currentStats = statsDao.getUserStatsDirect() ?: UserStatsEntity(id = 1)
        val newXp = currentStats.xpTotal + xpEarned
        val newLevel = (newXp / 100) + 1

        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val todayStr = sdf.format(java.util.Date(now))

        // Update or insert today's activity record
        val existingToday = dailyActivityDao.getActivityForDate(todayStr) ?: com.example.data.model.DailyActivityEntity(
            dateString = todayStr,
            wordsReviewed = 0,
            quizzesCompleted = 0,
            xpEarned = 0,
            goalMet = false,
            timestamp = now
        )

        val updatedReviewed = existingToday.wordsReviewed + (if (isReview) 1 else 0)
        val updatedQuizzes = existingToday.quizzesCompleted + (if (isQuiz) 1 else 0)
        val updatedTodayXp = existingToday.xpEarned + xpEarned
        val isGoalMet = updatedReviewed >= currentStats.dailyGoal

        dailyActivityDao.insertOrUpdate(
            existingToday.copy(
                wordsReviewed = updatedReviewed,
                quizzesCompleted = updatedQuizzes,
                xpEarned = updatedTodayXp,
                goalMet = isGoalMet,
                timestamp = now
            )
        )

        val calToday = Calendar.getInstance().apply { timeInMillis = now }
        val calLast = Calendar.getInstance().apply { timeInMillis = currentStats.lastActiveDate }

        val isSameDay = calToday.get(Calendar.YEAR) == calLast.get(Calendar.YEAR) &&
                calToday.get(Calendar.DAY_OF_YEAR) == calLast.get(Calendar.DAY_OF_YEAR)

        calToday.add(Calendar.DAY_OF_YEAR, -1)
        val isConsecutiveDay = calToday.get(Calendar.YEAR) == calLast.get(Calendar.YEAR) &&
                calToday.get(Calendar.DAY_OF_YEAR) == calLast.get(Calendar.DAY_OF_YEAR)

        val newStreak = when {
            isSameDay -> currentStats.currentStreak.coerceAtLeast(1)
            isConsecutiveDay -> currentStats.currentStreak + 1
            currentStats.lastActiveDate == 0L -> 1
            else -> 1 // Reset if missed day
        }

        val longestStreak = maxOf(newStreak, currentStats.longestStreak)

        val updatedStats = currentStats.copy(
            xpTotal = newXp,
            level = newLevel,
            currentStreak = newStreak,
            longestStreak = longestStreak,
            lastActiveDate = now
        )
        statsDao.insertOrUpdate(updatedStats)
    }

    suspend fun awardQuizXp(xpEarned: Int) = withContext(ioDispatcher) {
        awardXpAndUpdateStreak(xpEarned, isReview = false, isQuiz = true)
    }

    fun getRecentDailyActivity(limit: Int = 14): Flow<List<com.example.data.model.DailyActivityEntity>> {
        return dailyActivityDao.getRecentActivity(limit)
    }

    fun getCompletedDaysCount(): Flow<Int> {
        return dailyActivityDao.getCompletedDaysCount()
    }

    suspend fun updateDailyGoal(goal: Int) = withContext(ioDispatcher) {
        val currentStats = statsDao.getUserStatsDirect() ?: UserStatsEntity(id = 1)
        statsDao.insertOrUpdate(currentStats.copy(dailyGoal = goal))
    }

    suspend fun resetWordProgress(wordId: Long) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        reviewDao.resetWordProgress(wordId, now)
    }

    fun getDueCount(): Flow<Int> {
        val now = System.currentTimeMillis()
        return reviewDao.getDueReviewCount(now)
    }

    fun getAverageEaseFactor(): Flow<Double?> {
        return reviewDao.getAverageEaseFactor()
    }

    fun getUpcomingReviewSchedule(limit: Int = 20): Flow<List<ReviewStateEntity>> {
        return reviewDao.getUpcomingReviewSchedule(limit)
    }

    suspend fun exportBackupJson(): String = withContext(ioDispatcher) {
        val stats = statsDao.getUserStatsDirect()
        val reviews = reviewDao.getAllReviewStatesDirect()
        com.example.data.util.BackupManager.generateBackupJson(stats, reviews)
    }

    suspend fun restoreBackupFromJson(jsonString: String): Result<com.example.data.util.BackupData> = withContext(ioDispatcher) {
        val parseResult = com.example.data.util.BackupManager.parseBackupJson(jsonString)
        if (parseResult.isFailure) {
            return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("Unknown parse error"))
        }

        val backupData = parseResult.getOrThrow()
        // Save stats
        statsDao.insertOrUpdate(backupData.stats)
        // Save review states
        if (backupData.reviewStates.isNotEmpty()) {
            reviewDao.insertOrUpdateAll(backupData.reviewStates)
        }
        Result.success(backupData)
    }

    fun getUserStats(): Flow<UserStatsEntity?> = statsDao.getUserStats()

    companion object {
        @Volatile
        private var INSTANCE: VocabRepository? = null

        fun getInstance(context: Context): VocabRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = VocabRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
