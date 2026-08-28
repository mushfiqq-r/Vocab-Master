package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyActivityEntity
import com.example.data.model.ReviewStateEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY term ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE term LIKE '%' || :query || '%' OR banglaMeaning LIKE '%' || :query || '%' OR primaryMeaning LIKE '%' || :query || '%' ORDER BY term ASC")
    fun searchWords(query: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE bookIds LIKE '%' || :bookId || '%' ORDER BY term ASC")
    fun getWordsByBook(bookId: String): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_states")
    fun getAllReviewStates(): Flow<List<ReviewStateEntity>>

    @Query("SELECT * FROM review_states WHERE wordId = :wordId LIMIT 1")
    suspend fun getReviewStateForWord(wordId: Long): ReviewStateEntity?

    @Query("SELECT * FROM review_states WHERE nextReviewDate <= :timestamp OR status = 'new'")
    suspend fun getDueReviewStates(timestamp: Long): List<ReviewStateEntity>

    @Query("SELECT COUNT(*) FROM review_states WHERE nextReviewDate <= :timestamp OR status = 'new'")
    fun getDueReviewCount(timestamp: Long): Flow<Int>

    @Query("SELECT AVG(easeFactor) FROM review_states WHERE status != 'new'")
    fun getAverageEaseFactor(): Flow<Double?>

    @Query("SELECT * FROM review_states ORDER BY nextReviewDate ASC LIMIT :limit")
    fun getUpcomingReviewSchedule(limit: Int): Flow<List<ReviewStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reviewState: ReviewStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(reviewStates: List<ReviewStateEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(reviewStates: List<ReviewStateEntity>)

    @Query("SELECT * FROM review_states")
    suspend fun getAllReviewStatesDirect(): List<ReviewStateEntity>

    @Query("SELECT COUNT(*) FROM review_states WHERE status = :status")
    fun countByStatus(status: String): Flow<Int>

    @Query("UPDATE review_states SET easeFactor = 2.5, intervalDays = 0, repetitions = 0, nextReviewDate = :timestamp, lastReviewedDate = 0, status = 'new' WHERE wordId = :wordId")
    suspend fun resetWordProgress(wordId: Long, timestamp: Long)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsDirect(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)
}

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC")
    fun getAllActivity(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity WHERE dateString = :dateString LIMIT 1")
    suspend fun getActivityForDate(dateString: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC LIMIT :limit")
    fun getRecentActivity(limit: Int): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity ORDER BY dateString DESC LIMIT :limit")
    suspend fun getRecentActivityDirect(limit: Int): List<DailyActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(activity: DailyActivityEntity)

    @Query("SELECT COUNT(*) FROM daily_activity WHERE goalMet = 1")
    fun getCompletedDaysCount(): Flow<Int>
}

